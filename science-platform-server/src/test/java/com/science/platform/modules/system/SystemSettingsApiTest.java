package com.science.platform.modules.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.science.platform.modules.system.mapper.SysAnnouncementMapper;
import com.science.platform.modules.system.mapper.SysConfigMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 第二轮优化：系统基础设置与公告的权限与功能测试。
 *
 * <p>对应验收要求：系统设置接口权限、普通管理员不可修改超管配置、
 * 公告能正常发布与下架、首页能读取后台维护的平台信息。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SystemSettingsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysConfigMapper configMapper;

    @Autowired
    private SysAnnouncementMapper announcementMapper;

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String token(String username) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("username", username, "password", "123456"));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("token").asText();
    }

    // ---------- 公开接口 ----------

    @Test
    @DisplayName("游客可读取平台信息，公告只含已发布")
    void siteInfoIsPublic() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/site/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").isNotEmpty())
                .andExpect(jsonPath("$.data.description").isNotEmpty())
                .andReturn();

        JsonNode data = readJson(result).path("data");
        JsonNode announcements = data.path("announcements");
        assertTrue(announcements.size() >= 1, "演示数据里至少有 2 条已发布公告");
        announcements.forEach(node ->
                assertEquals("PUBLISHED", node.path("status").asText(), "公开接口不应返回草稿公告"));
    }

    @Test
    @DisplayName("草稿公告不出现在公开列表")
    void draftAnnouncementInvisible() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/announcements"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode list = readJson(result).path("data");
        for (JsonNode node : list) {
            assertFalse(node.path("title").asText().contains("草稿"),
                    "草稿公告不应出现在公开列表");
        }
    }

    // ---------- 权限边界 ----------

    @Test
    @DisplayName("超级管理员可访问系统设置")
    void superAdminCanAccessSettings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings")
                        .header("Authorization", "Bearer " + token("superadmin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").isNotEmpty());
    }

    @Test
    @DisplayName("普通管理员不能读取或修改系统设置")
    void adminCannotAccessSettings() throws Exception {
        String adminToken = token("admin01");

        mockMvc.perform(get("/api/v1/admin/settings").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "被篡改的名称",
                                "description", "被篡改的简介")))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("普通管理员不能访问公告管理")
    void adminCannotManageAnnouncements() throws Exception {
        String adminToken = token("admin01");

        mockMvc.perform(get("/api/v1/admin/announcements").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"越权公告\",\"content\":\"x\"}")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录访问系统设置返回 401")
    void guestCannotAccessSettings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("评审员与创作者也不能访问系统设置")
    void otherRolesCannotAccessSettings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings")
                        .header("Authorization", "Bearer " + token("reviewer01")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/settings")
                        .header("Authorization", "Bearer " + token("creator01")))
                .andExpect(status().isForbidden());
    }

    // ---------- 设置读写 ----------

    @Test
    @DisplayName("超管修改平台信息后，首页接口立即读到新值")
    void updateSettingsReflectsOnSiteInfo() throws Exception {
        String superToken = token("superadmin");

        mockMvc.perform(put("/api/v1/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "测试平台名称",
                                "subtitle", "Test Platform",
                                "description", "这是测试用的平台简介。",
                                "banner", "测试横幅",
                                "contact", "test@example.com")))
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试平台名称"));

        mockMvc.perform(get("/api/v1/site/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试平台名称"))
                .andExpect(jsonPath("$.data.banner").value("测试横幅"))
                .andExpect(jsonPath("$.data.contact").value("test@example.com"));
    }

    @Test
    @DisplayName("平台名称或简介为空时校验失败")
    void settingsValidation() throws Exception {
        mockMvc.perform(put("/api/v1/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"description\":\"\"}")
                        .header("Authorization", "Bearer " + token("superadmin")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("平台名称不能为空"),
                                org.hamcrest.Matchers.containsString("平台简介不能为空"))));
    }

    // ---------- 公告生命周期 ----------

    @Test
    @DisplayName("公告：新增草稿 → 发布 → 下架 → 重新发布 → 删除")
    void announcementLifecycle() throws Exception {
        String superToken = token("superadmin");
        long publishedBefore = countPublished();

        // 新增草稿
        MvcResult created = mockMvc.perform(post("/api/v1/admin/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "【测试】生命周期公告",
                                "content", "测试正文",
                                "sortOrder", 10,
                                "status", "DRAFT")))
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        long id = readJson(created).path("data").path("id").asLong();

        // 草稿不影响公开列表
        assertEquals(publishedBefore, countPublished());

        // 发布后公开可见
        mockMvc.perform(post("/api/v1/admin/announcements/" + id + "/publish")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());
        assertEquals(publishedBefore + 1, countPublished());

        // 重复发布被拒绝
        mockMvc.perform(post("/api/v1/admin/announcements/" + id + "/publish")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("公告已是发布状态"));

        // 下架后公开列表恢复
        mockMvc.perform(post("/api/v1/admin/announcements/" + id + "/offline")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFFLINE"));
        assertEquals(publishedBefore, countPublished());

        // 可以重新发布
        mockMvc.perform(post("/api/v1/admin/announcements/" + id + "/publish")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 删除
        mockMvc.perform(delete("/api/v1/admin/announcements/" + id)
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk());
        assertEquals(publishedBefore, countPublished());
    }

    @Test
    @DisplayName("公告标题与正文必填")
    void announcementValidation() throws Exception {
        mockMvc.perform(post("/api/v1/admin/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"\"}")
                        .header("Authorization", "Bearer " + token("superadmin")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("公告标题不能为空"),
                                org.hamcrest.Matchers.containsString("公告正文不能为空"))));
    }

    @Test
    @DisplayName("后台公告列表含草稿，可查看全部状态")
    void adminAnnouncementListIncludesDraft() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/announcements")
                        .header("Authorization", "Bearer " + token("superadmin")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode list = readJson(result).path("data");
        boolean hasDraft = false;
        for (JsonNode node : list) {
            if ("DRAFT".equals(node.path("status").asText())) {
                hasDraft = true;
            }
        }
        assertTrue(hasDraft, "演示数据里有一条草稿公告，后台应能看到");
    }

    private long countPublished() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/announcements"))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").size();
    }
}
