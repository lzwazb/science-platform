package com.science.platform.modules.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.science.platform.modules.interaction.mapper.InteractionCommentMapper;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 后台评论管理（第三轮修复 G1）。
 *
 * <p>覆盖四件事：管理员能跨内容检索、评审员无权进入、删除为逻辑删除且前台立刻不可见、
 * 非法目标类型被拒绝。类级 {@code @Transactional} 保证测试产生的评论与删除都回滚。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminCommentApiTest {

    /** 演示数据里的已发布文章 */
    private static final long ARTICLE_ID = 4L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InteractionCommentMapper commentMapper;

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

    /** 用指定用户在某篇文章下发一条评论，返回评论 ID */
    private long createComment(String token, String content) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "targetType", "ARTICLE",
                "targetId", ARTICLE_ID,
                "content", content));
        MvcResult result = mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("id").asLong();
    }

    @Test
    @DisplayName("管理员可按关键词检索评论，并带回目标标题与评论者")
    void adminCanListAndSearch() throws Exception {
        String userToken = token("user01");
        String adminToken = token("admin01");
        String marker = "后台检索唯一标记-" + System.nanoTime();
        long commentId = createComment(userToken, marker);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/comments")
                        .param("keyword", marker)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andReturn();

        JsonNode row = readJson(result).path("data").path("records").get(0);
        assertEquals(commentId, row.path("id").asLong());
        assertEquals("ARTICLE", row.path("targetType").asText());
        assertTrue(row.path("targetTitle").asText().length() > 0, "后台列表应带回目标标题");
        assertNotNull(row.path("userName").asText());
        assertEquals("VISIBLE", row.path("status").asText());
    }

    @Test
    @DisplayName("评审员无权访问后台评论接口")
    void reviewerCannotAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/comments")
                        .header("Authorization", "Bearer " + token("reviewer01")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("管理员删除后：前台不再可见，后台仍可按已删除筛出")
    void adminDeleteHidesFromPublicButKeepsRecord() throws Exception {
        String userToken = token("user01");
        String adminToken = token("admin01");
        String marker = "待删除评论-" + System.nanoTime();
        long commentId = createComment(userToken, marker);

        mockMvc.perform(delete("/api/v1/admin/comments/" + commentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertEquals("DELETED", commentMapper.selectById(commentId).getStatus(), "应为逻辑删除");

        // 前台按目标查评论，已删除的不再出现
        MvcResult publicResult = mockMvc.perform(get("/api/v1/comments")
                        .param("targetType", "ARTICLE")
                        .param("targetId", String.valueOf(ARTICLE_ID))
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode records = readJson(publicResult).path("data").path("records");
        boolean visible = false;
        for (JsonNode record : records) {
            if (record.path("id").asLong() == commentId) {
                visible = true;
                break;
            }
        }
        assertTrue(!visible, "已删除评论不应出现在前台列表");

        // 后台按 DELETED 仍能查到，便于追溯
        MvcResult adminResult = mockMvc.perform(get("/api/v1/admin/comments")
                        .param("status", "DELETED")
                        .param("keyword", marker)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(1, readJson(adminResult).path("data").path("total").asInt());
    }

    @Test
    @DisplayName("不支持的目标类型返回 400")
    void rejectsUnknownTargetType() throws Exception {
        mockMvc.perform(get("/api/v1/admin/comments")
                        .param("targetType", "TOPIC")
                        .header("Authorization", "Bearer " + token("admin01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("不支持的目标类型")));
    }
}
