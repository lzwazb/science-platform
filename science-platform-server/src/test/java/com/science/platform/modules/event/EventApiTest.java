package com.science.platform.modules.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.science.platform.modules.event.entity.EventSubmission;
import com.science.platform.modules.event.mapper.EventReviewMapper;
import com.science.platform.modules.event.mapper.EventSubmissionMapper;
import com.science.platform.modules.event.service.EventService;
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
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 阶段 8 主题活动测试（对应开发档案 12.2 的 T11）。
 *
 * <p>演示数据：活动 1 进行中（投稿 2 条），活动 2 已结束（投稿 1 条且已获奖）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventApiTest {

    /** 进行中的活动 */
    private static final long ONGOING_EVENT = 1L;
    /** 已结束的活动 */
    private static final long ENDED_EVENT = 2L;
    /** 属于 creator01 的作品 */
    private static final long WORK_OF_CREATOR01 = 1L;
    /** 属于 creator02 且尚未报名活动 1 的作品 */
    private static final long WORK_OF_CREATOR02 = 3L;
    /** 已有评审记录的投稿 */
    private static final long REVIEWED_SUBMISSION = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventSubmissionMapper submissionMapper;

    @Autowired
    private EventReviewMapper reviewMapper;

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

    // ---------- 公开列表与时间状态 ----------

    @Test
    @DisplayName("活动列表返回时间状态：进行中 / 已结束")
    void listShowsTimeStatus() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode events = readJson(result).path("data");
        assertTrue(events.size() >= 2, "演示数据至少含两个已发布活动");

        boolean foundOngoing = false;
        boolean foundEnded = false;
        for (JsonNode event : events) {
            long id = event.path("id").asLong();
            if (id == ONGOING_EVENT) {
                assertEquals(EventService.TIME_ONGOING, event.path("timeStatus").asText());
                assertTrue(event.path("acceptingSubmission").asBoolean());
                foundOngoing = true;
            } else if (id == ENDED_EVENT) {
                assertEquals(EventService.TIME_ENDED, event.path("timeStatus").asText(),
                        "已结束的活动应判定为 ENDED，而不是未发布");
                assertTrue(!event.path("acceptingSubmission").asBoolean());
                foundEnded = true;
            }
        }
        assertTrue(foundOngoing && foundEnded, "列表应同时包含进行中与已结束的活动");
    }

    @Test
    @DisplayName("活动详情含规则与优秀作品")
    void detailContainsRulesAndAwards() throws Exception {
        mockMvc.perform(get("/api/v1/events/" + ENDED_EVENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rules").isNotEmpty())
                .andExpect(jsonPath("$.data.timeStatus").value(EventService.TIME_ENDED))
                .andExpect(jsonPath("$.data.awards[0].awardLevel").value("FIRST"))
                .andExpect(jsonPath("$.data.awards[0].workTitle").value("碳足迹的一天：从早餐到熄灯"));
    }

    @Test
    @DisplayName("草稿活动对前台不可见")
    void draftEventInvisible() throws Exception {
        String adminToken = token("admin01");
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "【测试】草稿活动",
                "startAt", "2026-11-01 00:00:00",
                "endAt", "2026-12-01 00:00:00",
                "status", "DRAFT"));

        MvcResult created = mockMvc.perform(post("/api/v1/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        long draftId = readJson(created).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/events/" + draftId))
                .andExpect(status().isNotFound());

        String raw = mockMvc.perform(get("/api/v1/events")).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(!raw.contains("草稿活动"), "公开列表不应出现草稿活动");
    }

    // ---------- T11 投稿时间校验 ----------

    @Test
    @DisplayName("T11 向已结束的活动投稿被拒绝")
    void cannotSubmitToEndedEvent() throws Exception {
        mockMvc.perform(post("/api/v1/events/" + ENDED_EVENT + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workId\":" + WORK_OF_CREATOR02 + "}")
                        .header("Authorization", "Bearer " + token("creator02")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("活动已结束，不能再投稿"));
    }

    @Test
    @DisplayName("T11 向未开始的活动投稿被拒绝")
    void cannotSubmitToNotStartedEvent() throws Exception {
        String adminToken = token("admin01");
        LocalDateTime start = LocalDateTime.now().plusDays(10);
        LocalDateTime end = start.plusDays(30);
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "【测试】未开始的活动",
                "startAt", start.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "endAt", end.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "status", "PUBLISHED"));

        MvcResult created = mockMvc.perform(post("/api/v1/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        long eventId = readJson(created).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/events/" + eventId + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workId\":" + WORK_OF_CREATOR01 + "}")
                        .header("Authorization", "Bearer " + token("creator01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("活动尚未开始，暂不能投稿"));
    }

    @Test
    @DisplayName("不能提交他人的作品")
    void cannotSubmitOthersWork() throws Exception {
        mockMvc.perform(post("/api/v1/events/" + ONGOING_EVENT + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workId\":" + WORK_OF_CREATOR02 + "}")
                        .header("Authorization", "Bearer " + token("creator01")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只能提交自己的作品"));
    }

    @Test
    @DisplayName("同一作品在同一活动不能重复投稿")
    void cannotSubmitSameWorkTwice() throws Exception {
        mockMvc.perform(post("/api/v1/events/" + ONGOING_EVENT + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workId\":" + WORK_OF_CREATOR01 + "}")
                        .header("Authorization", "Bearer " + token("creator01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该作品已报名本活动，不能重复投稿"));
    }

    @Test
    @DisplayName("普通用户不能投稿")
    void normalUserCannotSubmit() throws Exception {
        mockMvc.perform(post("/api/v1/events/" + ONGOING_EVENT + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workId\":" + WORK_OF_CREATOR01 + "}")
                        .header("Authorization", "Bearer " + token("user01")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("投稿成功后出现在我的投稿里")
    void submitAppearsInMine() throws Exception {
        String creatorToken = token("creator02");

        mockMvc.perform(post("/api/v1/events/" + ONGOING_EVENT + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workId\":" + WORK_OF_CREATOR02 + "}")
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workTitle").value("碳足迹的一天：从早餐到熄灯"));

        MvcResult mine = mockMvc.perform(get("/api/v1/events/" + ONGOING_EVENT + "/submissions/mine")
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = readJson(mine).path("data");
        assertTrue(items.size() >= 1);
        items.forEach(item -> assertEquals(5L, item.path("submitterId").asLong(),
                "我的投稿只能包含本人的记录"));
    }

    @Test
    @DisplayName("未登录访问我的投稿返回 401（不被公开规则放行）")
    void guestCannotReadMySubmissions() throws Exception {
        mockMvc.perform(get("/api/v1/events/" + ONGOING_EVENT + "/submissions/mine"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 评审 ----------

    @Test
    @DisplayName("评审打分；同一评审员重复提交为修改而非新增")
    void reviewIsUpsert() throws Exception {
        String reviewerToken = token("reviewer01");

        var countQuery = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.science.platform.modules.event.entity.EventReview>();
        countQuery.eq(com.science.platform.modules.event.entity.EventReview::getSubmissionId, REVIEWED_SUBMISSION)
                .eq(com.science.platform.modules.event.entity.EventReview::getReviewerId, 3L);
        long myReviewsBefore = reviewMapper.selectCount(countQuery);

        MvcResult first = mockMvc.perform(post("/api/v1/review/submissions/" + REVIEWED_SUBMISSION + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5,\"comment\":\"结构清晰\"}")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andReturn();
        long afterFirst = reviewMapper.selectCount(countQuery);

        // 该评审员此前若未评过则 +1，否则保持不变
        assertEquals(myReviewsBefore == 0 ? 1L : myReviewsBefore, afterFirst);

        // 再评一次：记录条数不变，分数被更新
        mockMvc.perform(post("/api/v1/review/submissions/" + REVIEWED_SUBMISSION + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":3,\"comment\":\"复议后改为 3 分\"}")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewedByMe").value(true));

        assertEquals(afterFirst, reviewMapper.selectCount(countQuery),
                "同一评审员对同一投稿只保留一份有效评审");

        // 分数确实被改成了 3
        var review = reviewMapper.selectOne(countQuery);
        assertEquals(3, review.getScore());
        assertTrue(review.getComment().contains("复议"));
    }

    @Test
    @DisplayName("评分与评语校验")
    void reviewValidation() throws Exception {
        String reviewerToken = token("reviewer01");

        mockMvc.perform(post("/api/v1/review/submissions/" + REVIEWED_SUBMISSION + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":9,\"comment\":\"越界\"}")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("评分最高 5 分")));

        mockMvc.perform(post("/api/v1/review/submissions/" + REVIEWED_SUBMISSION + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5}")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("请填写评语")));
    }

    @Test
    @DisplayName("创作者不能进入活动评审接口")
    void creatorCannotReviewSubmissions() throws Exception {
        mockMvc.perform(get("/api/v1/review/events/" + ONGOING_EVENT + "/submissions")
                        .header("Authorization", "Bearer " + token("creator01")))
                .andExpect(status().isForbidden());
    }

    // ---------- 优秀作品 ----------

    @Test
    @DisplayName("未评审的投稿不能被标为优秀")
    void unreviewedSubmissionCannotWinAward() throws Exception {
        String creatorToken = token("creator02");
        String adminToken = token("admin01");

        // 新建一条没有任何评审的投稿
        MvcResult submitted = mockMvc.perform(post("/api/v1/events/" + ONGOING_EVENT + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workId\":" + WORK_OF_CREATOR02 + "}")
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andReturn();
        long newSubmissionId = readJson(submitted).path("data").path("id").asLong();

        String body = objectMapper.writeValueAsString(Map.of(
                "awards", java.util.List.of(Map.of(
                        "submissionId", newSubmissionId,
                        "awardLevel", "EXCELLENT"))));

        mockMvc.perform(post("/api/v1/admin/events/" + ONGOING_EVENT + "/awards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("还没有任何评审记录")));

        // 投稿状态未被改动
        EventSubmission after = submissionMapper.selectById(newSubmissionId);
        assertEquals(null, after.getAwardLevel(), "被拒绝的获奖确认不应写入 award_level");
    }

    @Test
    @DisplayName("已评审的投稿可以确认为优秀作品")
    void reviewedSubmissionCanWinAward() throws Exception {
        String adminToken = token("admin01");

        String body = objectMapper.writeValueAsString(Map.of(
                "awards", java.util.List.of(Map.of(
                        "submissionId", REVIEWED_SUBMISSION,
                        "awardLevel", "FIRST"))));

        mockMvc.perform(post("/api/v1/admin/events/" + ONGOING_EVENT + "/awards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].awardLevel").value("FIRST"));

        mockMvc.perform(get("/api/v1/events/" + ONGOING_EVENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.awards[0].workTitle")
                        .value("银河系漫游指南：一张图看懂我们所在的家"));
    }

    @Test
    @DisplayName("评审员不能确认获奖名单（仅管理员）")
    void reviewerCannotConfirmAwards() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "awards", java.util.List.of(Map.of(
                        "submissionId", REVIEWED_SUBMISSION,
                        "awardLevel", "FIRST"))));

        mockMvc.perform(post("/api/v1/admin/events/" + ONGOING_EVENT + "/awards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token("reviewer01")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("获奖投稿必须属于该活动")
    void awardSubmissionMustBelongToEvent() throws Exception {
        String adminToken = token("admin01");

        // 投稿 3 属于活动 2
        String body = objectMapper.writeValueAsString(Map.of(
                "awards", java.util.List.of(Map.of(
                        "submissionId", 3,
                        "awardLevel", "FIRST"))));

        mockMvc.perform(post("/api/v1/admin/events/" + ONGOING_EVENT + "/awards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("不属于本活动")));
    }

    // ---------- 后台活动管理 ----------

    @Test
    @DisplayName("结束时间早于开始时间被拒绝")
    void invalidTimeRangeRejected() throws Exception {
        String adminToken = token("admin01");
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "【测试】时间错误的活划",
                "startAt", "2026-12-01 00:00:00",
                "endAt", "2026-11-01 00:00:00"));

        mockMvc.perform(post("/api/v1/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("结束时间必须晚于开始时间"));
    }

    @Test
    @DisplayName("发布活动后进入进行中，可接受投稿")
    void publishMakesEventOngoing() throws Exception {
        String adminToken = token("admin01");
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(30);
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "【测试】待发布活动",
                "startAt", start.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "endAt", end.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "status", "DRAFT"));

        MvcResult created = mockMvc.perform(post("/api/v1/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        long eventId = readJson(created).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/admin/events/" + eventId + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.timeStatus").value(EventService.TIME_ONGOING))
                .andExpect(jsonPath("$.data.acceptingSubmission").value(true));
    }

    @Test
    @DisplayName("结束活动后时间状态变为已结束")
    void closeEventStopsSubmission() throws Exception {
        String adminToken = token("admin01");

        mockMvc.perform(post("/api/v1/admin/events/" + ONGOING_EVENT + "/close")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.timeStatus").value(EventService.TIME_ENDED))
                .andExpect(jsonPath("$.data.acceptingSubmission").value(false));

        // 已结束的活动不能再投稿
        mockMvc.perform(post("/api/v1/events/" + ONGOING_EVENT + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workId\":" + WORK_OF_CREATOR02 + "}")
                        .header("Authorization", "Bearer " + token("creator02")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("普通用户不能访问后台活动接口")
    void normalUserCannotManageEvents() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + token("user01")))
                .andExpect(status().isForbidden());
    }
}
