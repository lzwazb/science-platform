package com.science.platform.modules.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.science.platform.modules.interaction.mapper.InteractionFavoriteMapper;
import com.science.platform.modules.interaction.mapper.InteractionLikeMapper;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 阶段 7 社区互动测试（对应开发档案 12.2 的 T10）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InteractionApiTest {

    /** 演示数据里的已发布文章 */
    private static final long ARTICLE_ID = 4L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InteractionLikeMapper likeMapper;

    @Autowired
    private InteractionFavoriteMapper favoriteMapper;

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

    private String likeBody(long targetId) throws Exception {
        return objectMapper.writeValueAsString(Map.of("targetType", "ARTICLE", "targetId", targetId));
    }

    /** 当前该文章的点赞总数，用于做「增量」断言，避免依赖演示数据里的绝对条数 */
    private int currentLikeCount(long targetId) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.science.platform.modules.interaction.entity.InteractionLike>();
        wrapper.eq(com.science.platform.modules.interaction.entity.InteractionLike::getTargetType, "ARTICLE")
                .eq(com.science.platform.modules.interaction.entity.InteractionLike::getTargetId, targetId);
        Long count = likeMapper.selectCount(wrapper);
        return count == null ? 0 : count.intValue();
    }

    /** 指定用户对该文章的点赞记录数 */
    private long likeRecordsOf(long userId, long targetId) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.science.platform.modules.interaction.entity.InteractionLike>();
        wrapper.eq(com.science.platform.modules.interaction.entity.InteractionLike::getUserId, userId)
                .eq(com.science.platform.modules.interaction.entity.InteractionLike::getTargetType, "ARTICLE")
                .eq(com.science.platform.modules.interaction.entity.InteractionLike::getTargetId, targetId);
        return likeMapper.selectCount(wrapper);
    }

    // ---------- T10 重复点赞 ----------

    @Test
    @DisplayName("T10 同一用户重复点赞只保留一条记录，计数不重复增长")
    void duplicateLikeCreatesSingleRecord() throws Exception {
        String userToken = token("user01");

        // 先确保该用户初始没有点赞这篇文章
        mockMvc.perform(delete("/api/v1/interactions/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(likeBody(ARTICLE_ID))
                .header("Authorization", "Bearer " + userToken));

        int before = currentLikeCount(ARTICLE_ID);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/interactions/likes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(likeBody(ARTICLE_ID))
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk());
        }

        assertEquals(1L, likeRecordsOf(6L, ARTICLE_ID), "同一用户对同一目标只能有一条点赞记录");

        // 计数只增加 1
        mockMvc.perform(get("/api/v1/interactions/status")
                        .param("targetType", "ARTICLE")
                        .param("targetId", String.valueOf(ARTICLE_ID))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(before + 1));
    }

    @Test
    @DisplayName("不同用户点赞累加，取消后递减")
    void likeCountAcrossUsers() throws Exception {
        String user1 = token("user01");
        String creator = token("creator01");

        // 清掉两人的初始状态，保证从 0 增量开始计算
        mockMvc.perform(delete("/api/v1/interactions/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(likeBody(ARTICLE_ID))
                .header("Authorization", "Bearer " + user1));
        mockMvc.perform(delete("/api/v1/interactions/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(likeBody(ARTICLE_ID))
                .header("Authorization", "Bearer " + creator));

        int before = currentLikeCount(ARTICLE_ID);

        mockMvc.perform(post("/api/v1/interactions/likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(likeBody(ARTICLE_ID))
                        .header("Authorization", "Bearer " + user1))
                .andExpect(jsonPath("$.data.likeCount").value(before + 1));

        mockMvc.perform(post("/api/v1/interactions/likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(likeBody(ARTICLE_ID))
                        .header("Authorization", "Bearer " + creator))
                .andExpect(jsonPath("$.data.likeCount").value(before + 2));

        mockMvc.perform(delete("/api/v1/interactions/likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(likeBody(ARTICLE_ID))
                        .header("Authorization", "Bearer " + user1))
                .andExpect(jsonPath("$.data.likeCount").value(before + 1));

        // 取消不存在的点赞也要幂等成功
        mockMvc.perform(delete("/api/v1/interactions/likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(likeBody(ARTICLE_ID))
                        .header("Authorization", "Bearer " + user1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(before + 1));
    }

    @Test
    @DisplayName("给不存在或未发布的文章点赞返回 404")
    void cannotLikeInvisibleTarget() throws Exception {
        String userToken = token("user01");

        mockMvc.perform(post("/api/v1/interactions/likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(likeBody(999999L))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("游客能看互动计数，但 liked / favorited 恒为 false")
    void guestSeesCountsButNotOwnState() throws Exception {
        String userToken = token("user01");
        mockMvc.perform(post("/api/v1/interactions/likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(likeBody(ARTICLE_ID))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        int expected = currentLikeCount(ARTICLE_ID);

        mockMvc.perform(get("/api/v1/interactions/status")
                        .param("targetType", "ARTICLE")
                        .param("targetId", String.valueOf(ARTICLE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(expected))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.favorited").value(false));
    }

    @Test
    @DisplayName("未登录不能点赞")
    void guestCannotLike() throws Exception {
        mockMvc.perform(post("/api/v1/interactions/likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(likeBody(ARTICLE_ID)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 收藏 ----------

    @Test
    @DisplayName("收藏幂等，并出现在我的收藏里")
    void favoriteIsIdempotent() throws Exception {
        String userToken = token("user01");

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/interactions/favorites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(likeBody(ARTICLE_ID))
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.favoriteCount").value(1));
        }

        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.science.platform.modules.interaction.entity.InteractionFavorite>();
        wrapper.eq(com.science.platform.modules.interaction.entity.InteractionFavorite::getUserId, 6L)
                .eq(com.science.platform.modules.interaction.entity.InteractionFavorite::getTargetId, ARTICLE_ID);
        assertEquals(1L, favoriteMapper.selectCount(wrapper));

        mockMvc.perform(get("/api/v1/interactions/favorites/mine").param("size", "50")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // ---------- 评论 ----------

    @Test
    @DisplayName("游客可阅读评论，回复挂在一级评论下")
    void guestCanReadCommentsWithReplies() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/comments")
                        .param("targetType", "ARTICLE")
                        .param("targetId", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode records = readJson(result).path("data").path("records");
        assertTrue(records.size() >= 1);
        // 演示数据里评论 1 有一条回复
        boolean hasReply = false;
        for (JsonNode node : records) {
            if (node.path("replies").size() > 0) {
                hasReply = true;
            }
        }
        assertTrue(hasReply, "演示数据中应存在带回复的评论");
    }

    @Test
    @DisplayName("发表评论与回复；回复的回复归到同一根评论")
    void createCommentAndNestedReply() throws Exception {
        String user1 = token("user01");
        String creator = token("creator01");

        MvcResult created = mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetType", "ARTICLE",
                                "targetId", ARTICLE_ID,
                                "content", "这条评论用于测试")))
                        .header("Authorization", "Bearer " + user1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mine").value(true))
                .andReturn();
        long rootId = readJson(created).path("data").path("id").asLong();

        MvcResult reply = mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetType", "ARTICLE",
                                "targetId", ARTICLE_ID,
                                "content", "这是一级回复",
                                "parentId", rootId)))
                        .header("Authorization", "Bearer " + creator))
                .andExpect(status().isOk())
                .andReturn();
        long replyId = readJson(reply).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetType", "ARTICLE",
                                "targetId", ARTICLE_ID,
                                "content", "这是回复的回复",
                                "parentId", replyId)))
                        .header("Authorization", "Bearer " + user1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(rootId));
    }

    @Test
    @DisplayName("未登录不能发表评论")
    void guestCannotComment() throws Exception {
        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"ARTICLE\",\"targetId\":4,\"content\":\"游客评论\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("评论删除权限：本人可删、他人 403、管理员可删任意评论")
    void commentDeletePermissions() throws Exception {
        String user1 = token("user01");
        String creator = token("creator01");
        String admin = token("admin01");

        MvcResult created = mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetType", "ARTICLE",
                                "targetId", ARTICLE_ID,
                                "content", "待删除的评论")))
                        .header("Authorization", "Bearer " + user1))
                .andExpect(status().isOk())
                .andReturn();
        long commentId = readJson(created).path("data").path("id").asLong();

        // 他人删除 → 403
        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + creator))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只能删除自己的评论"));

        // 管理员删除 → 成功
        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        // 已删除的评论不再出现在列表里
        MvcResult list = mockMvc.perform(get("/api/v1/comments")
                        .param("targetType", "ARTICLE")
                        .param("targetId", String.valueOf(ARTICLE_ID))
                        .param("size", "50"))
                .andReturn();
        readJson(list).path("data").path("records").forEach(node ->
                assertTrue(node.path("id").asLong() != commentId, "已删除评论不应出现在列表中"));
    }

    // ---------- 内容反馈 ----------

    @Test
    @DisplayName("提交反馈并在后台查看、处理；重复处理被拒绝")
    void feedbackSubmitAndHandle() throws Exception {
        String userToken = token("user01");
        String adminToken = token("admin01");

        MvcResult created = mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetType", "ARTICLE",
                                "targetId", ARTICLE_ID,
                                "feedbackType", "SUGGESTION",
                                "description", "希望补充一张流程图")))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.targetTitle").isNotEmpty())
                .andReturn();
        long feedbackId = readJson(created).path("data").path("id").asLong();

        // 普通用户不能访问后台
        mockMvc.perform(get("/api/v1/admin/feedback").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // 管理员处理
        mockMvc.perform(post("/api/v1/admin/feedback/" + feedbackId + "/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"handleComment\":\"已补充\"}")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.handlerName").value("内容管理员"));

        // 重复处理 → 400
        mockMvc.perform(post("/api/v1/admin/feedback/" + feedbackId + "/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该反馈已处理完毕，不能再次处理"));
    }

    @Test
    @DisplayName("反馈说明为空时参数校验失败")
    void feedbackRequiresDescription() throws Exception {
        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"ARTICLE\",\"targetId\":4,\"feedbackType\":\"OTHER\"}")
                        .header("Authorization", "Bearer " + token("user01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("反馈说明不能为空")));
    }

    @Test
    @DisplayName("我的反馈只包含本人提交的记录")
    void myFeedbackIsolated() throws Exception {
        String user1 = token("user01");
        String creator = token("creator01");

        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetType", "ARTICLE",
                                "targetId", ARTICLE_ID,
                                "feedbackType", "OTHER",
                                "description", "隔离测试反馈")))
                        .header("Authorization", "Bearer " + user1))
                .andExpect(status().isOk());

        MvcResult creatorFeedback = mockMvc.perform(get("/api/v1/feedback/mine").param("size", "50")
                        .header("Authorization", "Bearer " + creator))
                .andExpect(status().isOk())
                .andReturn();

        readJson(creatorFeedback).path("data").path("records").forEach(node ->
                assertEquals(4L, node.path("userId").asLong(), "creator01 不应看到别人的反馈"));
    }
}
