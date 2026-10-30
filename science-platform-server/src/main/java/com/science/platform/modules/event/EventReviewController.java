package com.science.platform.modules.event;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.event.dto.SubmissionReviewRequest;
import com.science.platform.modules.event.entity.CreativeEvent;
import com.science.platform.modules.event.service.EventReviewService;
import com.science.platform.modules.event.service.EventService;
import com.science.platform.modules.event.service.EventSubmissionService;
import com.science.platform.modules.event.vo.SubmissionListItemVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动投稿评审（评审员及以上）。
 */
@RestController
@RequestMapping("/api/v1/review")
@PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN', 'SUPER_ADMIN')")
public class EventReviewController {

    private final EventReviewService eventReviewService;
    private final EventSubmissionService submissionService;
    private final EventService eventService;

    public EventReviewController(EventReviewService eventReviewService,
                                 EventSubmissionService submissionService,
                                 EventService eventService) {
        this.eventReviewService = eventReviewService;
        this.submissionService = submissionService;
        this.eventService = eventService;
    }

    /**
     * 某活动的全部投稿，含平均分与「我是否已评」。
     */
    @GetMapping("/events/{eventId}/submissions")
    public ApiResponse<List<SubmissionListItemVO>> list(@PathVariable Long eventId) {
        CreativeEvent event = eventService.requireEvent(eventId);
        // 草稿与已取消的活动不进入评审流程
        if (EventService.STATUS_DRAFT.equals(event.getStatus())
                || EventService.STATUS_CANCELLED.equals(event.getStatus())) {
            return ApiResponse.success(List.of());
        }
        return ApiResponse.success(submissionService.listForReview(
                eventId, SecurityUtils.currentUserId()));
    }

    /**
     * 对投稿打分并写评语。同一评审员重复提交为「修改」而不是新增。
     */
    @PostMapping("/submissions/{id}/review")
    public ApiResponse<SubmissionListItemVO> review(@PathVariable Long id,
                                                    @Valid @RequestBody SubmissionReviewRequest request) {
        return ApiResponse.success("评审已提交",
                eventReviewService.review(id, request, SecurityUtils.currentUserId()));
    }
}
