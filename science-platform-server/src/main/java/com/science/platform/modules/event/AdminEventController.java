package com.science.platform.modules.event;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.event.dto.AwardRequest;
import com.science.platform.modules.event.dto.EventSaveRequest;
import com.science.platform.modules.event.entity.CreativeEvent;
import com.science.platform.modules.event.service.EventReviewService;
import com.science.platform.modules.event.service.EventService;
import com.science.platform.modules.event.service.EventSubmissionService;
import com.science.platform.modules.event.vo.AwardVO;
import com.science.platform.modules.event.vo.EventDetailVO;
import com.science.platform.modules.event.vo.EventListItemVO;
import com.science.platform.modules.event.vo.SubmissionListItemVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台活动管理（管理员及以上）。
 */
@RestController
@RequestMapping("/api/v1/admin/events")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminEventController {

    private final EventService eventService;
    private final EventSubmissionService submissionService;
    private final EventReviewService eventReviewService;

    public AdminEventController(EventService eventService,
                                EventSubmissionService submissionService,
                                EventReviewService eventReviewService) {
        this.eventService = eventService;
        this.submissionService = submissionService;
        this.eventReviewService = eventReviewService;
    }

    /**
     * 后台活动列表，含草稿与已取消。
     */
    @GetMapping
    public ApiResponse<List<EventListItemVO>> list() {
        LocalDateTime now = LocalDateTime.now();
        return ApiResponse.success(eventService.listAllEvents().stream().map(event -> {
            EventListItemVO vo = new EventListItemVO();
            vo.setId(event.getId());
            vo.setTitle(event.getTitle());
            vo.setDescription(event.getDescription());
            vo.setCoverUrl(event.getCoverUrl());
            vo.setStartAt(event.getStartAt());
            vo.setEndAt(event.getEndAt());
            vo.setStatus(event.getStatus());
            vo.setTimeStatus(EventService.timeStatusOf(event, now));
            vo.setAcceptingSubmission(EventService.isAcceptingSubmission(event, now));
            vo.setSubmissionCount(submissionService.countByEvent(event.getId()));
            vo.setCreatedAt(event.getCreatedAt());
            return vo;
        }).toList());
    }

    /**
     * 后台活动详情（含规则与投稿列表）。
     */
    @GetMapping("/{id}")
    public ApiResponse<EventDetailVO> detail(@PathVariable Long id) {
        CreativeEvent event = eventService.requireEvent(id);
        LocalDateTime now = LocalDateTime.now();

        EventDetailVO vo = new EventDetailVO();
        vo.setId(event.getId());
        vo.setTitle(event.getTitle());
        vo.setDescription(event.getDescription());
        vo.setRules(event.getRules());
        vo.setCoverUrl(event.getCoverUrl());
        vo.setStartAt(event.getStartAt());
        vo.setEndAt(event.getEndAt());
        vo.setStatus(event.getStatus());
        vo.setTimeStatus(EventService.timeStatusOf(event, now));
        vo.setAcceptingSubmission(EventService.isAcceptingSubmission(event, now));
        vo.setCreatedAt(event.getCreatedAt());
        vo.setSubmissionCount(submissionService.countByEvent(event.getId()));
        vo.setAwards(submissionService.listAwards(event.getId()));
        vo.setMySubmissions(submissionService.listForReview(id, null));
        return ApiResponse.success(vo);
    }

    /**
     * 创建活动。
     */
    @PostMapping
    public ApiResponse<EventDetailVO> create(@Valid @RequestBody EventSaveRequest request) {
        CreativeEvent event = eventService.create(request, SecurityUtils.currentUserId());
        return ApiResponse.success("活动已创建", detail(event.getId()).getData());
    }

    /**
     * 编辑活动。
     */
    @PutMapping("/{id}")
    public ApiResponse<EventDetailVO> update(@PathVariable Long id,
                                             @Valid @RequestBody EventSaveRequest request) {
        eventService.update(id, request);
        return ApiResponse.success("活动已更新", detail(id).getData());
    }

    /**
     * 发布活动，开始接受投稿。
     */
    @PostMapping("/{id}/publish")
    public ApiResponse<EventDetailVO> publish(@PathVariable Long id) {
        eventService.publish(id);
        return ApiResponse.success("活动已发布", detail(id).getData());
    }

    /**
     * 结束活动。
     */
    @PostMapping("/{id}/close")
    public ApiResponse<EventDetailVO> close(@PathVariable Long id) {
        eventService.close(id);
        return ApiResponse.success("活动已结束", detail(id).getData());
    }

    /**
     * 删除活动（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ApiResponse.success("活动已删除", null);
    }

    /**
     * 确认优秀作品。
     */
    @PostMapping("/{id}/awards")
    public ApiResponse<List<AwardVO>> confirmAwards(@PathVariable Long id,
                                                    @Valid @RequestBody AwardRequest request) {
        eventReviewService.confirmAwards(id, request, SecurityUtils.currentUserId());
        return ApiResponse.success("优秀作品已确认", submissionService.listAwards(id));
    }

    /**
     * 撤销某件投稿的获奖标记。
     */
    @PostMapping("/submissions/{submissionId}/revoke-award")
    public ApiResponse<Void> revokeAward(@PathVariable Long submissionId) {
        eventReviewService.revokeAward(submissionId, SecurityUtils.currentUserId());
        return ApiResponse.success("已撤销获奖标记", null);
    }
}
