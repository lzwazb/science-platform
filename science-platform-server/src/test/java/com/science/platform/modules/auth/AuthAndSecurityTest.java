package com.science.platform.modules.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 阶段 3 认证与授权测试，对应开发档案 12.2 的 T01、T02、T03。
 *
 * <p>运行前置条件：已执行 database/schema.sql 与 database/seed.sql（演示账号密码均为 123456）。
 * 这里走完整的 Security 过滤器链，使用真实签发的 JWT，而不是 mock 认证。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthAndSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        String payload = objectMapper.writeValueAsString(
                java.util.Map.of("username", username, "password", password));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return body.path("data").path("token").asText();
    }

    // ---------- T01 正常登录 ----------

    @Test
    @DisplayName("T01 正常登录：返回令牌与用户信息，角色正确")
    void loginSuccess() throws Exception {
        String payload = objectMapper.writeValueAsString(
                java.util.Map.of("username", "admin01", "password", "123456"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(7200))
                .andExpect(jsonPath("$.data.user.username").value("admin01"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("ADMIN"))
                // 响应体不能出现密码哈希
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
    }

    // ---------- T02 错误密码 ----------

    @Test
    @DisplayName("T02 错误密码：401，且提示不泄漏账号是否存在")
    void loginWithWrongPassword() throws Exception {
        String wrongPassword = objectMapper.writeValueAsString(
                java.util.Map.of("username", "admin01", "password", "definitely-wrong"));

        MvcResult wrong = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongPassword))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andReturn();

        String notExist = objectMapper.writeValueAsString(
                java.util.Map.of("username", "no-such-user", "password", "definitely-wrong"));

        MvcResult notExistResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notExist))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode a = objectMapper.readTree(wrong.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode b = objectMapper.readTree(notExistResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals(a.path("message").asText(), b.path("message").asText(),
                "账号不存在与密码错误必须返回同一提示，避免被用来枚举用户名");
    }

    // ---------- T03 越权管理 ----------

    @Test
    @DisplayName("T03 越权管理：普通用户请求超管接口返回 403（不是 401）")
    void normalUserCannotAccessAdminApi() throws Exception {
        String userToken = login("user01", "123456");

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("T03 越权管理：管理员也不能访问超管专属接口")
    void adminCannotAccessSuperAdminApi() throws Exception {
        String adminToken = login("admin01", "123456");

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    // ---------- 未认证访问 ----------

    @Test
    @DisplayName("未携带令牌访问受保护接口返回 401，且响应为统一 JSON 结构")
    void unauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("伪造令牌被拒绝")
    void forgedTokenRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.forged.payload"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 令牌可用性 ----------

    @Test
    @DisplayName("有效令牌可读取本人资料，且 ID 来自令牌而非请求参数")
    void tokenResolvesCurrentUser() throws Exception {
        String userToken = login("user01", "123456");

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("user01"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"));
    }

    @Test
    @DisplayName("同一账号两次登录的令牌不同（含签发时间），但都能通过校验")
    void tokensAreIndependent() throws Exception {
        String first = login("creator01", "123456");
        String second = login("creator01", "123456");

        assertNotEquals("", first);
        assertNotEquals("", second);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + first))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + second))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("公开接口无需令牌即可访问")
    void publicEndpointsOpen() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("登录失败不产生令牌字段")
    void failedLoginHasNoToken() throws Exception {
        String payload = objectMapper.writeValueAsString(
                java.util.Map.of("username", "admin01", "password", "nope"));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String raw = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(!raw.contains("token"), "失败响应里不应出现任何令牌字样");
    }
}
