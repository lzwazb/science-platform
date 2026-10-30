package com.science.platform.modules.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 第二轮优化：活动时间状态在四个入口的一致性。
 *
 * <p>时间状态是「读时现算」的，如果前台列表、前台详情、后台管理、投稿校验
 * 各自写一套判断，就会出现「列表显示进行中但投稿被拒」这类矛盾。
 * 本测试确认四个入口用的是同一套规则。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventStatusConsistencyTest {

    /** 演示数据中处于进行中的活动 */
    private static final long ONGOING_EVENT = 1L;
    /** 演示数据中已被手动结束的活动 */
    private static final long ENDED_EVENT = 2L;

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

    @Test
    @DisplayName("公开列表、公开详情、后台详情三处时间状态一致")
    void timeStatusConsistentAcrossEndpoints() throws Exception {
        String adminToken = token("admin01");

        // 公开列表
        MvcResult listResult = mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = readJson(listResult).path("data");

        for (JsonNode item : list) {
            long id = item.path("id").asLong();
            String fromList = item.path("timeStatus").asText();
            boolean acceptingFromList = item.path("acceptingSubmission").asBoolean();

            // 公开详情
            MvcResult detailResult = mockMvc.perform(get("/api/v1/events/" + id))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode detail = readJson(detailResult).path("data");
            assertEquals(fromList, detail.path("timeStatus").asText(),
                    "活动 " + id + " 的列表与详情时间状态应一致");
            assertEquals(acceptingFromList, detail.path("acceptingSubmission").asBoolean(),
                    "活动 " + id + " 的列表与详情可投稿标记应一致");

            // 后台详情
            MvcResult adminResult = mockMvc.perform(get("/api/v1/admin/events/" + id)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode adminDetail = readJson(adminResult).path("data");
            assertEquals(fromList, adminDetail.path("timeStatus").asText(),
                    "活动 " + id + " 的后台与前台时间状态应一致");
        }
    }

    @Test
    @DisplayName("进行中的活动可投稿，已结束的活动被拒——与展示状态对应")
    void submissionRuleMatchesDisplayedStatus() throws Exception {
        String creatorToken = token("creator02");

        // 进行中活动：投稿应成功（作品 3 尚未报名活动 1）
        MvcResult detailOngoing = mockMvc.perform(get("/api/v1/events/" + ONGOING_EVENT))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(readJson(detailOngoing).path("data").path("acceptingSubmission").asBoolean(),
                "进行中的活动 acceptingSubmission 应为 true");
        assertEquals(EventService.TIME_ONGOING,
                readJson(detailOngoing).path("data").path("timeStatus").asText());

        mockMvc.perform(post("/api/v1/events/" + ONGOING_EVENT + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workId\":3}")
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk());

        // 已结束活动：投稿应被拒，与其展示状态对应
        MvcResult detailEnded = mockMvc.perform(get("/api/v1/events/" + ENDED_EVENT))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(EventService.TIME_ENDED,
                readJson(detailEnded).path("data").path("timeStatus").asText());
        assertTrue(!readJson(detailEnded).path("data").path("acceptingSubmission").asBoolean(),
                "已结束的活动 acceptingSubmission 应为 false");
    }

    @Test
    @DisplayName("静态判定方法：草稿不外露、已结束优先于时间判断")
    void timeStatusRules() {
        var now = java.time.LocalDateTime.of(2026, 10, 30, 12, 0);

        var ongoing = new com.science.platform.modules.event.entity.CreativeEvent();
        ongoing.setStatus("PUBLISHED");
        ongoing.setStartAt(now.minusDays(1));
        ongoing.setEndAt(now.plusDays(1));
        assertEquals(EventService.TIME_ONGOING, EventService.timeStatusOf(ongoing, now));
        assertTrue(EventService.isAcceptingSubmission(ongoing, now));

        var notStarted = new com.science.platform.modules.event.entity.CreativeEvent();
        notStarted.setStatus("PUBLISHED");
        notStarted.setStartAt(now.plusDays(1));
        notStarted.setEndAt(now.plusDays(5));
        assertEquals(EventService.TIME_NOT_STARTED, EventService.timeStatusOf(notStarted, now));
        assertTrue(!EventService.isAcceptingSubmission(notStarted, now));

        var expired = new com.science.platform.modules.event.entity.CreativeEvent();
        expired.setStatus("PUBLISHED");
        expired.setStartAt(now.minusDays(10));
        expired.setEndAt(now.minusDays(1));
        assertEquals(EventService.TIME_ENDED, EventService.timeStatusOf(expired, now));

        // 手动结束时即使时间还没到，也应判定为已结束
        var closedEarly = new com.science.platform.modules.event.entity.CreativeEvent();
        closedEarly.setStatus("CLOSED");
        closedEarly.setStartAt(now.minusDays(1));
        closedEarly.setEndAt(now.plusDays(10));
        assertEquals(EventService.TIME_ENDED, EventService.timeStatusOf(closedEarly, now));
        assertTrue(!EventService.isAcceptingSubmission(closedEarly, now));

        var draft = new com.science.platform.modules.event.entity.CreativeEvent();
        draft.setStatus("DRAFT");
        draft.setStartAt(now.minusDays(1));
        draft.setEndAt(now.plusDays(1));
        assertEquals(EventService.TIME_UNPUBLISHED, EventService.timeStatusOf(draft, now));

        var cancelled = new com.science.platform.modules.event.entity.CreativeEvent();
        cancelled.setStatus("CANCELLED");
        cancelled.setStartAt(now.minusDays(1));
        cancelled.setEndAt(now.plusDays(1));
        assertEquals(EventService.TIME_UNPUBLISHED, EventService.timeStatusOf(cancelled, now));
    }
}
