package com.science.platform.modules.interaction;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.science.platform.modules.interaction.entity.InteractionComment;
import com.science.platform.modules.interaction.mapper.InteractionCommentMapper;
import com.science.platform.modules.notification.entity.SysNotification;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 评论回复通知的收件人是否正确。
 *
 * <p>这里覆盖的是一个真实的 bug：初版把 {@code parentId} 提升成根评论后，
 * 又用提升后的 id 去查「被回复的评论」，导致 A → B → C 的对话里
 * C 的通知发给了 A，B 什么都没收到。</p>
 *
 * <p>修复方式：新增 {@code reply_to_comment_id} 记录「实际在回谁」，
 * 与「楼层归属」分开。本测试同时验证这两件事不再混用。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentReplyNotificationTest {

    /** user01 */
    private static final long USER_A = 6L;
    /** creator01 */
    private static final long USER_B = 4L;
    /** creator02 */
    private static final long USER_C = 5L;
    /** 演示数据里没有评论的文章 */
    private static final long TARGET_ARTICLE = 8L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InteractionCommentMapper commentMapper;

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

    private long postComment(String token, String content, Long parentId) throws Exception {
        Map<String, Object> body = parentId == null
                ? Map.of("targetType", "ARTICLE", "targetId", TARGET_ARTICLE, "content", content)
                : Map.of("targetType", "ARTICLE", "targetId", TARGET_ARTICLE,
                        "content", content, "parentId", parentId);

        MvcResult result = mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("id").asLong();
    }

    /**
     * 取该文章下最后一条 COMMENT_REPLY 通知。
     */
    private SysNotification latestReplyNotification() {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotification::getType, "COMMENT_REPLY")
                .eq(SysNotification::getRelatedType, "ARTICLE")
                .eq(SysNotification::getRelatedId, TARGET_ARTICLE)
                .orderByDesc(SysNotification::getId)
                .last("LIMIT 1");
        return notificationMapper.selectOne(wrapper);
    }

    @Test
    @DisplayName("一层回复：通知发给被回复者")
    void replyNotifiesDirectParent() throws Exception {
        String tokenA = token("user01");
        String tokenB = token("creator01");

        long commentA = postComment(tokenA, "这是一条根评论。", null);
        postComment(tokenB, "B 回复 A。", commentA);

        SysNotification notification = latestReplyNotification();
        assertNotNull(notification, "回复应产生通知");
        assertEquals(USER_A, notification.getRecipientId(), "通知应发给被回复的 A");
    }

    @Test
    @DisplayName("三层对话：C 回复 B，通知发给 B 而不是 A（原 bug）")
    void replyToSecondLevelNotifiesSecondLevelAuthor() throws Exception {
        String tokenA = token("user01");
        String tokenB = token("creator01");
        String tokenC = token("creator02");

        // 用增量断言：库里可能已有历史通知（手工验证留下的），
        // 断言「绝对条数」会受环境影响而误报
        long toABefore = countReplyNotificationsTo(USER_A);
        long toBBefore = countReplyNotificationsTo(USER_B);

        long commentA = postComment(tokenA, "A 的根评论。", null);
        long commentB = postComment(tokenB, "B 回复 A。", commentA);
        long commentC = postComment(tokenC, "C 回复 B。", commentB);

        // 楼层归属仍是根评论 A（保持一层结构）
        InteractionComment savedC = commentMapper.selectById(commentC);
        assertEquals(commentA, savedC.getParentId(), "C 的楼层应挂在根评论 A 下");
        // 但「实际在回谁」记录的是 B
        assertEquals(commentB, savedC.getReplyToCommentId(), "reply_to_comment_id 应指向 B 的评论");

        // B 收到了 C 的回复
        assertEquals(toBBefore + 1, countReplyNotificationsTo(USER_B),
                "C 回复 B，B 应新增一条通知");
        // A 只因为「B 回复 A」新增一条，不会因为「C 回复 B」再收到
        assertEquals(toABefore + 1, countReplyNotificationsTo(USER_A),
                "A 应只因『B 回复 A』新增一条通知，不应收到 C 回复 B 的通知");
    }

    @Test
    @DisplayName("回复展示带「回复 @某人」，且自己回复自己不产生通知")
    void replyShowsTargetAndSelfReplySilent() throws Exception {
        String tokenA = token("user01");
        String tokenB = token("creator01");

        long commentA = postComment(tokenA, "A 的根评论。", null);
        long commentB = postComment(tokenB, "B 回复 A。", commentA);

        // 列表里应能看到 replyToUserName
        MvcResult list = mockMvc.perform(get("/api/v1/comments")
                        .param("targetType", "ARTICLE")
                        .param("targetId", String.valueOf(TARGET_ARTICLE))
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode records = readJson(list).path("data").path("records");
        assertTrue(records.size() >= 1);

        JsonNode root = records.get(0);
        JsonNode replies = root.path("replies");
        assertTrue(replies.size() >= 1, "根评论下应有回复");
        JsonNode firstReply = replies.get(0);
        // replyToUserName 是「被回复的人」——B 回复的是 A，所以应显示 A 的昵称
        assertEquals("好奇的小周", firstReply.path("replyToUserName").asText(),
                "回复应带被回复者昵称，供前端渲染「回复 @某人」");
        assertEquals("星野科普", firstReply.path("userName").asText(),
                "回复者自身昵称不受影响");

        // B 回复自己的评论：不产生通知
        long beforeCount = countReplyNotifications();
        postComment(tokenB, "B 补充一句自己的话。", commentB);
        assertEquals(beforeCount, countReplyNotifications(), "自己回复自己不应产生通知");
    }

    @Test
    @DisplayName("顶层评论的 replyToCommentId 为空")
    void rootCommentHasNoReplyTarget() throws Exception {
        String tokenA = token("user01");
        long id = postComment(tokenA, "顶层评论。", null);

        InteractionComment saved = commentMapper.selectById(id);
        assertNull(saved.getParentId());
        assertNull(saved.getReplyToCommentId());
    }

    private long countReplyNotifications() {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotification::getType, "COMMENT_REPLY")
                .eq(SysNotification::getRelatedId, TARGET_ARTICLE);
        return notificationMapper.selectCount(wrapper);
    }

    /**
     * 某个用户在该文章下收到的 COMMENT_REPLY 通知条数。
     */
    private long countReplyNotificationsTo(long recipientId) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotification::getRecipientId, recipientId)
                .eq(SysNotification::getType, "COMMENT_REPLY")
                .eq(SysNotification::getRelatedId, TARGET_ARTICLE);
        return notificationMapper.selectCount(wrapper);
    }
}
