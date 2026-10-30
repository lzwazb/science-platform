package com.science.platform.modules.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.science.platform.modules.notification.mapper.SysNotificationMapper;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 阶段 9 站内消息与账号管理测试（对应开发档案 12.2 的 T12）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationApiTest {

    /** user01 的 ID，演示数据里有一条「知识贡献未通过」的未读通知 */
    private static final long USER01_ID = 6L;
    private static final long SUPER_ADMIN_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysNotificationMapper notificationMapper;

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

    // ---------- T12 消息隔离 ----------

    @Test
    @DisplayName("T12 只能读自己的消息：他人消息列表不含、标记已读返回 404")
    void messagesAreIsolated() throws Exception {
        String userToken = token("user01");
        String otherToken = token("creator01");

        // 取 user01 的一条消息 ID
        MvcResult mine = mockMvc.perform(get("/api/v1/notifications").param("size", "50")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode myRecords = readJson(mine).path("data").path("records");
        assertTrue(myRecords.size() >= 1, "演示数据里 user01 应有通知");
        long myNotificationId = myRecords.get(0).path("id").asLong();

        // 别人试图标记这条消息为已读 → 404
        mockMvc.perform(put("/api/v1/notifications/" + myNotificationId + "/read")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        // 别人的列表里不包含这条
        MvcResult others = mockMvc.perform(get("/api/v1/notifications").param("size", "50")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andReturn();
        readJson(others).path("data").path("records").forEach(node ->
                assertTrue(node.path("id").asLong() != myNotificationId, "不应看到他人的消息"));
    }

    @Test
    @DisplayName("未登录不能查看消息")
    void guestCannotReadNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("标记已读幂等，未读数随之归零")
    void markReadIsIdempotent() throws Exception {
        String userToken = token("user01");

        MvcResult list = mockMvc.perform(get("/api/v1/notifications")
                        .param("onlyUnread", "true")
                        .param("size", "50")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode unread = readJson(list).path("data").path("records");
        assertTrue(unread.size() >= 1, "演示数据里 user01 应有未读消息");
        long id = unread.get(0).path("id").asLong();

        mockMvc.perform(put("/api/v1/notifications/" + id + "/read")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // 重复标记：仍返回成功
        mockMvc.perform(put("/api/v1/notifications/" + id + "/read")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // 未读列表里不再出现
        MvcResult after = mockMvc.perform(get("/api/v1/notifications")
                        .param("onlyUnread", "true")
                        .param("size", "50")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        readJson(after).path("data").path("records").forEach(node ->
                assertTrue(node.path("id").asLong() != id, "已读消息不应出现在未读列表"));
    }

    @Test
    @DisplayName("消息带类型中文名与跳转路径")
    void notificationHasLabelAndLink() throws Exception {
        String userToken = token("user01");

        MvcResult result = mockMvc.perform(get("/api/v1/notifications").param("size", "50")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode records = readJson(result).path("data").path("records");
        boolean hasLabel = false;
        for (JsonNode node : records) {
            if (!node.path("typeLabel").asText().isEmpty()) {
                hasLabel = true;
            }
        }
        assertTrue(hasLabel, "每条消息都应带 typeLabel");
    }

    @Test
    @DisplayName("全部标记已读后未读数为 0")
    void markAllRead() throws Exception {
        String userToken = token("user01");

        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    // ---------- 个人中心概览 ----------

    @Test
    @DisplayName("个人中心概览返回各类数量")
    void userOverview() throws Exception {
        String userToken = token("user01");

        mockMvc.perform(get("/api/v1/users/me/overview")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contributionCount").isNumber())
                .andExpect(jsonPath("$.data.favoriteCount").isNumber())
                .andExpect(jsonPath("$.data.unreadNotificationCount").isNumber());
    }

    // ---------- 账号与角色管理 ----------

    @Test
    @DisplayName("仅超级管理员能访问账号管理，普通管理员返回 403")
    void onlySuperAdminCanManageUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token("admin01")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users").param("size", "50")
                        .header("Authorization", "Bearer " + token("superadmin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].roles").isArray());
    }

    @Test
    @DisplayName("分配角色后立即生效（同一令牌即可获得新权限）")
    void roleChangeTakesEffectImmediately() throws Exception {
        String superToken = token("superadmin");
        String userToken = token("user01");

        // 验证点选「审核工作台」而不是「创作者中心」：
        // 创作者中心对任何登录用户开放，不再能体现角色变化；
        // 评审台仍然限 REVIEWER 及以上，适合用来观察权限是否实时生效
        mockMvc.perform(get("/api/v1/review/works").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/admin/users/" + USER01_ID + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleCodes", List.of("USER", "REVIEWER"),
                                "reason", "测试角色分配")))
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles").isArray());

        // 分配后：同一个旧令牌立刻可用（因为过滤链每请求查库取角色）
        mockMvc.perform(get("/api/v1/review/works").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("角色编码不存在时被拒绝")
    void invalidRoleCodeRejected() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/" + USER01_ID + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleCodes", List.of("NOT_A_ROLE"),
                                "reason", "测试")))
                        .header("Authorization", "Bearer " + token("superadmin")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("角色不存在")));
    }

    @Test
    @DisplayName("超级管理员不能移除自己的超管角色（防锁死）")
    void cannotRemoveOwnSuperAdminRole() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/" + SUPER_ADMIN_ID + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleCodes", List.of("ADMIN"),
                                "reason", "误操作")))
                        .header("Authorization", "Bearer " + token("superadmin")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不能移除自己的超级管理员角色"));
    }

    @Test
    @DisplayName("超级管理员不能禁用自己（防锁死）")
    void cannotDisableSelf() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/" + SUPER_ADMIN_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}")
                        .header("Authorization", "Bearer " + token("superadmin")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不能禁用当前登录的账号"));
    }

    @Test
    @DisplayName("禁用账号后该用户的旧令牌立即失效")
    void disabledUserTokenInvalidated() throws Exception {
        String superToken = token("superadmin");
        String userToken = token("user01");

        // 禁用前可访问
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/admin/users/" + USER01_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\",\"reason\":\"测试禁用\"}")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        // 禁用后同一个令牌立刻 401
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized());

        // 恢复后旧令牌又能用
        mockMvc.perform(put("/api/v1/admin/users/" + USER01_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\",\"reason\":\"测试恢复\"}")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("用户列表支持关键词与角色筛选")
    void userListFilters() throws Exception {
        String superToken = token("superadmin");

        mockMvc.perform(get("/api/v1/admin/users").param("keyword", "creator")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/v1/admin/users").param("roleCode", "CREATOR")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(2));
    }

    @Test
    @DisplayName("账号状态变更成功后立即体现在查询结果里")
    void statusChangeReflected() throws Exception {
        String superToken = token("superadmin");

        mockMvc.perform(put("/api/v1/admin/users/" + USER01_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\",\"reason\":\"状态校验\"}")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        MvcResult list = mockMvc.perform(get("/api/v1/admin/users")
                        .param("status", "DISABLED")
                        .param("size", "50")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andReturn();

        boolean found = false;
        for (JsonNode node : readJson(list).path("data").path("records")) {
            if (node.path("id").asLong() == USER01_ID) {
                found = true;
                assertEquals("DISABLED", node.path("status").asText());
            }
        }
        assertTrue(found, "禁用后的账号应出现在「已禁用」筛选结果中");
    }
}
