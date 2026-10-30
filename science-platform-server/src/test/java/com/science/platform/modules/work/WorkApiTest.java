package com.science.platform.modules.work;

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
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 阶段 5 作品模块测试。
 *
 * <p>类级 {@code @Transactional} 保证每个用例结束后回滚，测试期间创建的作品不会留在演示数据里。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkApiTest {

    private static final long PUBLISHED_WORKS = 3L;
    private static final long PENDING_WORK_ID = 4L;
    private static final long WORK_OF_CREATOR01 = 1L;
    private static final long WORK_REFERENCED_BY_EVENT = 3L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private long createWork(String token, String title) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", "测试作品",
                "content", "测试内容",
                "categoryId", 7,
                "articleIds", new long[]{1L}));
        MvcResult result = mockMvc.perform(post("/api/v1/creator/works")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("id").asLong();
    }

    // ---------- 公开接口 ----------

    @Test
    @DisplayName("公开作品列表只返回 PUBLISHED 作品")
    void publicListOnlyPublished() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/works").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(PUBLISHED_WORKS))
                .andReturn();

        readJson(result).path("data").path("records").forEach(node ->
                assertEquals("PUBLISHED", node.path("status").asText()));
    }

    @Test
    @DisplayName("待审核作品对前台完全不可见")
    void pendingWorkInvisibleToPublic() throws Exception {
        mockMvc.perform(get("/api/v1/works/" + PENDING_WORK_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("作品详情返回关联文章与创作者信息")
    void workDetailContainsRelations() throws Exception {
        mockMvc.perform(get("/api/v1/works/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("碳足迹的一天：从早餐到熄灯"))
                .andExpect(jsonPath("$.data.creatorName").value("青柠手绘"))
                .andExpect(jsonPath("$.data.articles[0].title").value("碳循环与碳中和：地球的收支账"));
    }

    @Test
    @DisplayName("文章详情反向展示关联的已发布作品")
    void articleShowsRelatedWorks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/articles/7"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode works = readJson(result).path("data").path("works");
        assertEquals(1, works.size(), "文章 7 关联了 1 件已发布作品");
        assertEquals("碳足迹的一天：从早餐到熄灯", works.get(0).path("title").asText());
    }

    // ---------- 权限 ----------

    @Test
    @DisplayName("普通用户访问创作者中心返回 403")
    void normalUserCannotAccessCreatorCenter() throws Exception {
        mockMvc.perform(get("/api/v1/creator/works")
                        .header("Authorization", "Bearer " + token("user01")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("创作者访问审核接口返回 403")
    void creatorCannotAccessReviewApi() throws Exception {
        mockMvc.perform(get("/api/v1/review/works")
                        .header("Authorization", "Bearer " + token("creator01")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("T05 作品归属：用户 B 不能查看 / 修改 / 删除用户 A 的作品")
    void cannotOperateOthersWork() throws Exception {
        String otherToken = token("creator02");

        mockMvc.perform(get("/api/v1/creator/works/" + WORK_OF_CREATOR01)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        String updateBody = objectMapper.writeValueAsString(Map.of("title", "篡改", "content", "篡改"));
        mockMvc.perform(put("/api/v1/creator/works/" + WORK_OF_CREATOR01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/creator/works/" + WORK_OF_CREATOR01)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("我的作品只返回本人作品")
    void myWorksFilteredByOwner() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/creator/works").param("size", "50")
                        .header("Authorization", "Bearer " + token("creator02")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode records = readJson(result).path("data").path("records");
        assertEquals(2, records.size(), "creator02 名下有 2 件作品（1 已发布 + 1 待审核）");
        records.forEach(node -> assertEquals(5L, node.path("creatorId").asLong()));
    }

    // ---------- 状态流转 ----------

    @Test
    @DisplayName("创建 → 提交 → 审核通过 → 前台可见")
    void fullLifecycle() throws Exception {
        String creatorToken = token("creator01");
        String reviewerToken = token("reviewer01");

        long workId = createWork(creatorToken, "【测试】全流程作品");

        // 创建后是草稿，前台不可见
        mockMvc.perform(get("/api/v1/works/" + workId)).andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/creator/works/" + workId + "/submit")
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // 待审核阶段仍然不可见
        mockMvc.perform(get("/api/v1/works/" + workId)).andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/review/works/" + workId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"通过\"}")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.reviewComment").value("通过"));

        // 发布后前台可见
        mockMvc.perform(get("/api/v1/works/" + workId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("重复提交审核被拒绝")
    void duplicateSubmitRejected() throws Exception {
        String creatorToken = token("creator01");
        long workId = createWork(creatorToken, "【测试】重复提交");

        mockMvc.perform(post("/api/v1/creator/works/" + workId + "/submit")
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/creator/works/" + workId + "/submit")
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("作品已在审核中，请勿重复提交"));
    }

    @Test
    @DisplayName("驳回必须填写意见；驳回后作者能看到意见，重新提交会清空意见")
    void rejectRequiresCommentAndResubmitClearsIt() throws Exception {
        String creatorToken = token("creator01");
        String reviewerToken = token("reviewer01");
        long workId = createWork(creatorToken, "【测试】驳回流程");

        mockMvc.perform(post("/api/v1/creator/works/" + workId + "/submit")
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk());

        // 驳回不填意见 → 400
        mockMvc.perform(post("/api/v1/review/works/" + workId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("驳回时必须填写审核意见"));

        // 驳回并填意见
        mockMvc.perform(post("/api/v1/review/works/" + workId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"请补充数据来源\"}")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        // 作者能看到意见
        mockMvc.perform(get("/api/v1/creator/works/" + workId)
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.reviewComment").value("请补充数据来源"));

        // 重新提交后审核意见与审核时间都应被清空
        MvcResult resubmit = mockMvc.perform(post("/api/v1/creator/works/" + workId + "/submit")
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = readJson(resubmit).path("data");
        assertEquals("PENDING", data.path("status").asText());
        assertTrue(data.path("reviewComment").isNull(), "重新提交后旧的审核意见必须清空");
        assertTrue(data.path("reviewedAt").isNull(), "重新提交后旧的审核时间必须清空");
    }

    @Test
    @DisplayName("编辑已发布作品会自动回到待审核，并从公开列表消失")
    void editingPublishedWorkSendsBackToPending() throws Exception {
        String creatorToken = token("creator01");

        MvcResult result = mockMvc.perform(put("/api/v1/creator/works/" + WORK_OF_CREATOR01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "银河系漫游指南（修订版）",
                                "content", "补充了观测误差说明。")))
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        assertTrue(readJson(result).path("data").path("reviewComment").isNull());

        mockMvc.perform(get("/api/v1/works/" + WORK_OF_CREATOR01))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/works").param("size", "50"))
                .andExpect(jsonPath("$.data.total").value(PUBLISHED_WORKS - 1));
    }

    @Test
    @DisplayName("非待审核状态的作品不能再次审核")
    void cannotReviewNonPendingWork() throws Exception {
        String reviewerToken = token("reviewer01");

        // 作品 1 已发布
        mockMvc.perform(post("/api/v1/review/works/" + WORK_OF_CREATOR01 + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("不能进行审核")));
    }

    @Test
    @DisplayName("已被活动投稿引用的作品不能删除")
    void cannotDeleteWorkReferencedByEvent() throws Exception {
        String creatorToken = token("creator02");

        mockMvc.perform(delete("/api/v1/creator/works/" + WORK_REFERENCED_BY_EVENT)
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("活动投稿引用")));
    }

    @Test
    @DisplayName("关联文章必须是已发布文章")
    void cannotLinkUnpublishedArticle() throws Exception {
        String creatorToken = token("creator01");

        // 先造一篇草稿文章
        String articleBody = objectMapper.writeValueAsString(Map.of(
                "title", "【测试】草稿文章",
                "content", "正文"));
        MvcResult articleResult = mockMvc.perform(post("/api/v1/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleBody)
                        .header("Authorization", "Bearer " + token("admin01")))
                .andExpect(status().isOk())
                .andReturn();
        long draftArticleId = readJson(articleResult).path("data").path("id").asLong();

        String workBody = objectMapper.writeValueAsString(Map.of(
                "title", "【测试】关联草稿文章",
                "content", "内容",
                "articleIds", new long[]{draftArticleId}));

        mockMvc.perform(post("/api/v1/creator/works")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workBody)
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("只能关联已发布的科普文章"));
    }

    @Test
    @DisplayName("作品分类必须是 WORK 类型")
    void workCategoryMustBeWorkType() throws Exception {
        String creatorToken = token("creator01");
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "【测试】错误分类",
                "content", "内容",
                "categoryId", 1));

        mockMvc.perform(post("/api/v1/creator/works")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("所选分类不是作品分类"));
    }

    @Test
    @DisplayName("下架后作品对前台不可见")
    void offlineHidesWork() throws Exception {
        String reviewerToken = token("reviewer01");

        mockMvc.perform(post("/api/v1/review/works/" + WORK_OF_CREATOR01 + "/offline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"内容需整改\"}")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFFLINE"));

        mockMvc.perform(get("/api/v1/works/" + WORK_OF_CREATOR01))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("审核列表默认只返回待审核作品")
    void reviewListDefaultsToPending() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/review/works")
                        .header("Authorization", "Bearer " + token("reviewer01")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode records = readJson(result).path("data").path("records");
        assertEquals(1, records.size(), "演示数据中只有 1 件待审核作品");
        assertEquals(PENDING_WORK_ID, records.get(0).path("id").asLong());
        assertNotNull(records.get(0).path("creatorName").asText());
        assertNull(records.get(0).path("reviewComment").textValue());
    }
}
