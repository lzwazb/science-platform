package com.science.platform.modules.event;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.event.entity.CreativeEvent;
import com.science.platform.modules.event.service.EventService;
import com.science.platform.modules.event.service.EventSubmissionService;
import com.science.platform.modules.event.vo.AwardVO;
import com.science.platform.modules.event.vo.EventDetailVO;
import com.science.platform.modules.event.vo.EventListItemVO;
import com.science.platform.modules.event.vo.SubmissionListItemVO;
import com.science.platform.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动公开接口，游客可访问。
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;
    private final EventSubmissionService submissionService;

    public EventController(EventService eventService, EventSubmissionService submissionService) {
        this.eventService = eventService;
        this.submissionService = submissionService;
    }

    /**
     * 活动列表，只返回已发布或已结束的活动，并给出时间状态。
     */
    @GetMapping
    public ApiResponse<List<EventListItemVO>> list() {
        LocalDateTime now = LocalDateTime.now();
        List<EventListItemVO> items = eventService.listLiveEvents().stream()
                .map(event -> toListItem(event, now))
                .toList();
        return ApiResponse.success(items);
    }

    /**
     * 活动详情：含参与规则、优秀作品、以及当前用户在本活动中的投稿。
     */
    @GetMapping("/{id}")
    public ApiResponse<EventDetailVO> detail(@PathVariable Long id) {
        CreativeEvent event = eventService.requirePublicEvent(id);
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

        Long currentUserId = SecurityUtils.currentUserIdOrNull();
        if (currentUserId != null) {
            List<SubmissionListItemVO> mine = submissionService
                    .listMine(currentUserId, event.getId(), null);
            vo.setMySubmissions(mine);
        } else {
            vo.setMySubmissions(List.of());
        }

        return ApiResponse.success(vo);
    }

    /**
     * 列表项组装，供本控制器内部使用。
     */
    private EventListItemVO toListItem(CreativeEvent event, LocalDateTime now) {
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
    }

    /**
     * 优秀作品单独取（活动详情已内嵌，这里提供给需要单独刷新的场景）。
     */
    @GetMapping("/{id}/awards")
    public ApiResponse<List<AwardVO>> awards(@PathVariable Long id) {
        eventService.requirePublicEvent(id);
        return ApiResponse.success(submissionService.listAwards(id));
    }
}
