package com.science.platform.modules.contribution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.science.platform.modules.contribution.mapper.ArticleVersionMapper;
import com.science.platform.modules.contribution.mapper.KnowledgeContributionMapper;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 阶段 6 知识共建核心流程测试（对应开发档案 12.2 的 T06、T07、T08）。
 *
 * <p>类级 {@code @Transactional}：每个用例结束后回滚，审核通过改动的文章正文、
 * 生成的版本快照与通知都不会留在演示数据里。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContributionApiTest {

    /** 演示数据里的文章 4，当前为第 1 版；测试通过审核后应变为第 2 版 */
    private static final long ARTICLE_ID = 4L;
    private static final long PENDING_CONTRIBUTION_ID = 1L;
    private static final long ARTICLE_ID_5 = 5L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScienceArticleMapper articleMapper;

    @Autowired
    private ArticleVersionMapper versionMapper;

    @Autowired
    private KnowledgeContributionMapper contributionMapper;

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

    private long submitContribution(String token, long articleId, String content) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "articleId", articleId,
                "contributionType", "SUPPLEMENT",
                "suggestedContent", content,
                "reason", "原文没有提到这一点",
                "referenceSource", "教材相关章节"));
        MvcResult result = mockMvc.perform(post("/api/v1/contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("id").asLong();
    }

    // ---------- T06 提交校验 ----------

    @Test
    @DisplayName("T06 缺少建议内容 / 理由 / 来源时参数校验失败，不落库")
    void submitRequiresMandatoryFields() throws Exception {
        long before = contributionMapper.selectCount(null);

        mockMvc.perform(post("/api/v1/contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":4,\"contributionType\":\"CORRECTION\"}")
                        .header("Authorization", "Bearer " + token("user01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("建议内容不能为空"),
                                org.hamcrest.Matchers.containsString("提交理由不能为空"),
                                org.hamcrest.Matchers.containsString("参考来源不能为空"))));

        assertEquals(before, contributionMapper.selectCount(null), "校验失败不应写入数据库");
    }

    @Test
    @DisplayName("未登录不能提交贡献")
    void guestCannotSubmit() throws Exception {
        mockMvc.perform(post("/api/v1/contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":4,\"contributionType\":\"CORRECTION\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("提交成功后为待审核状态，并出现在我的贡献里")
    void submitCreatesPendingContribution() throws Exception {
        String userToken = token("user01");
        long id = submitContribution(userToken, ARTICLE_ID, "建议补充一段实验证据说明。");

        MvcResult mine = mockMvc.perform(get("/api/v1/contributions/mine").param("size", "50")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode records = readJson(mine).path("data").path("records");
        boolean found = false;
        for (JsonNode node : records) {
            if (node.path("id").asLong() == id) {
                found = true;
                assertEquals("PENDING", node.path("status").asText());
                assertEquals("光合作用分两步：光反应与卡尔文循环", node.path("articleTitle").asText());
            }
        }
        assertTrue(found, "新提交的贡献应出现在我的贡献列表里");
    }

    @Test
    @DisplayName("不能查看他人的贡献")
    void cannotViewOthersContribution() throws Exception {
        mockMvc.perform(get("/api/v1/contributions/" + PENDING_CONTRIBUTION_ID)
                        .header("Authorization", "Bearer " + token("creator01")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("撤回自己的待审核建议")
    void withdrawPendingContribution() throws Exception {
        String userToken = token("user01");
        long id = submitContribution(userToken, ARTICLE_ID, "准备撤回的建议。");

        mockMvc.perform(post("/api/v1/contributions/" + id + "/withdraw")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));

        // 已撤回的不能再撤回
        mockMvc.perform(post("/api/v1/contributions/" + id + "/withdraw")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    // ---------- 审核工作台 ----------

    @Test
    @DisplayName("创作者不能进入知识贡献审核工作台")
    void creatorCannotReview() throws Exception {
        mockMvc.perform(get("/api/v1/review/contributions")
                        .header("Authorization", "Bearer " + token("creator01")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("审核列表默认只返回待审核，且附带原文供对照")
    void reviewListAndDetail() throws Exception {
        String reviewerToken = token("reviewer01");

        MvcResult list = mockMvc.perform(get("/api/v1/review/contributions").param("size", "50")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode records = readJson(list).path("data").path("records");
        for (JsonNode node : records) {
            assertEquals("PENDING", node.path("status").asText());
        }

        MvcResult detail = mockMvc.perform(get("/api/v1/review/contributions/" + PENDING_CONTRIBUTION_ID)
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referenceSource").isNotEmpty())
                .andExpect(jsonPath("$.data.reason").isNotEmpty())
                .andReturn();

        String articleContent = readJson(detail).path("data").path("articleContent").asText();
        assertTrue(articleContent.length() > 100, "审核详情应带上文章当前正文，供评审对照");
    }

    // ---------- T07 驳回 ----------

    @Test
    @DisplayName("T07 驳回必须填写意见；驳回后提交者能看到意见与状态")
    void rejectRequiresComment() throws Exception {
        String reviewerToken = token("reviewer01");

        mockMvc.perform(post("/api/v1/review/contributions/" + PENDING_CONTRIBUTION_ID + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("驳回时必须填写审核意见"));

        mockMvc.perform(post("/api/v1/review/contributions/" + PENDING_CONTRIBUTION_ID + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"建议方向对，但需要补充权威来源链接。\"}")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.reviewComment").value("建议方向对，但需要补充权威来源链接。"));
    }

    // ---------- T08 通过并生成版本 ----------

    @Test
    @DisplayName("T08 通过后：文章正文更新、版本号+1、历史快照保存旧内容、通知生成")
    void approveUpdatesArticleAndCreatesVersion() throws Exception {
        String userToken = token("user01");
        String reviewerToken = token("reviewer01");

        // 自己提交一条待审核建议，不依赖演示数据里某条记录的固定状态
        long contributionId = submitContribution(userToken, ARTICLE_ID, "建议补充一段实验证据说明。");

        ScienceArticle before = articleMapper.selectById(ARTICLE_ID);
        int versionBefore = before.getVersionNo();
        String contentBefore = before.getContent();
        long versionRowsBefore = versionMapper.selectCount(null);
        long notificationRowsBefore = notificationMapper.selectCount(null);

        String newContent = "这是审核通过后采用的正文，用于验证版本流转。";
        String body = objectMapper.writeValueAsString(Map.of(
                "comment", "按建议采纳",
                "finalContent", newContent));

        mockMvc.perform(post("/api/v1/review/contributions/" + contributionId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.finalContent").value(newContent));

        // 文章：正文被替换，版本号 +1
        ScienceArticle after = articleMapper.selectById(ARTICLE_ID);
        assertEquals(versionBefore + 1, after.getVersionNo(), "版本号应加一");
        assertEquals(newContent, after.getContent(), "正文应更新为最终采用的内容");

        // 历史快照：记录了「被覆盖之前的旧内容」
        assertEquals(versionRowsBefore + 1, versionMapper.selectCount(null), "应新增一条历史版本");
        var versionQuery = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.science.platform.modules.contribution.entity.ArticleVersion>();
        versionQuery.eq(com.science.platform.modules.contribution.entity.ArticleVersion::getArticleId, ARTICLE_ID)
                .eq(com.science.platform.modules.contribution.entity.ArticleVersion::getVersionNo, versionBefore);
        var snapshot = versionMapper.selectOne(versionQuery);
        assertNotNull(snapshot, "应存在版本号为变更前版本的历史记录");
        assertEquals(contentBefore, snapshot.getContentSnapshot(), "快照内容必须是变更前的旧正文");
        assertEquals(contributionId, snapshot.getSourceContributionId(), "快照应能追溯到来源贡献");
        assertNotNull(snapshot.getOperatorId(), "快照应记录操作者");

        // 通知：给提交者发了一条审核结果
        assertEquals(notificationRowsBefore + 1, notificationMapper.selectCount(null), "应生成一条站内通知");
    }

    @Test
    @DisplayName("同一条建议不能被重复处理")
    void cannotProcessTwice() throws Exception {
        String userToken = token("user01");
        String reviewerToken = token("reviewer01");
        long contributionId = submitContribution(userToken, ARTICLE_ID, "可被重复审核测试的建议。");
        String body = objectMapper.writeValueAsString(Map.of("comment", "通过"));

        // 记录处理前的版本条数（该文章可能已有历史版本，用差值断言更稳）
        var countQuery = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.science.platform.modules.contribution.entity.ArticleVersion>();
        countQuery.eq(com.science.platform.modules.contribution.entity.ArticleVersion::getArticleId, ARTICLE_ID);
        long versionsBefore = versionMapper.selectCount(countQuery);

        mockMvc.perform(post("/api/v1/review/contributions/" + contributionId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/review/contributions/" + contributionId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("不能重复处理")));

        // 重复处理不应产生额外的历史版本
        assertEquals(versionsBefore + 1, versionMapper.selectCount(countQuery),
                "一次审核只应生成一条历史版本");
    }

    @Test
    @DisplayName("不填最终正文时，以建议内容作为新正文")
    void approveWithoutFinalContentFallsBackToSuggestion() throws Exception {
        String userToken = token("user01");
        String reviewerToken = token("reviewer01");
        String suggestion = "这是读者建议的完整正文内容。";
        long id = submitContribution(userToken, ARTICLE_ID_5, suggestion);

        mockMvc.perform(post("/api/v1/review/contributions/" + id + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"采纳\"}")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalContent").value(suggestion));

        assertEquals(suggestion, articleMapper.selectById(ARTICLE_ID_5).getContent());
    }

    // ---------- 版本历史 ----------

    @Test
    @DisplayName("已发布文章的版本历史对游客可读")
    void versionHistoryPubliclyReadable() throws Exception {
        // 演示数据里文章 3 已有一条第 1 版快照
        MvcResult result = mockMvc.perform(get("/api/v1/articles/3/versions"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode versions = readJson(result).path("data");
        assertTrue(versions.size() >= 1);
        assertEquals(1, versions.get(0).path("versionNo").asInt(), "倒序时最后一条是第 1 版");
        assertNotNull(versions.get(0).path("changeNote").asText());
    }

    @Test
    @DisplayName("指定版本返回内容快照；不存在返回 404")
    void versionDetailAndNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/articles/3/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andExpect(jsonPath("$.data.contentSnapshot").isNotEmpty())
                .andExpect(jsonPath("$.data.operatorName").isNotEmpty());

        mockMvc.perform(get("/api/v1/articles/3/versions/99"))
                .andExpect(status().isNotFound());
    }
}
