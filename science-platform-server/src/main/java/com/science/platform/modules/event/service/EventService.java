package com.science.platform.modules.event.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.event.dto.EventSaveRequest;
import com.science.platform.modules.event.entity.CreativeEvent;
import com.science.platform.modules.event.mapper.CreativeEventMapper;
import com.science.platform.modules.event.vo.EventDetailVO;
import com.science.platform.modules.event.vo.EventListItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 活动本体：创建、编辑、发布、结束，以及公开列表与详情。
 *
 * <p>时间状态（未开始 / 进行中 / 已结束）每次读取时按当前时间现算，
 * 不落库也不做定时任务——这样活动到期自动变成「已结束」，
 * 不会出现「时间到了但状态字段还是进行中」的不一致。</p>
 */
@Service
public class EventService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String TIME_NOT_STARTED = "NOT_STARTED";
    public static final String TIME_ONGOING = "ONGOING";
    public static final String TIME_ENDED = "ENDED";
    public static final String TIME_UNPUBLISHED = "UNPUBLISHED";

    private final CreativeEventMapper eventMapper;

    public EventService(CreativeEventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    /**
     * 计算活动的时间状态。
     *
     * <p>三种来源要分开处理：草稿/已取消 → 未发布；管理员手动结束 → 已结束；
     * 其余按当前时间与起止时间比较。如果只判断 {@code PUBLISHED}，
     * 已结束的活动会被错误地报成「未发布」。</p>
     */
    public static String timeStatusOf(CreativeEvent event, LocalDateTime now) {
        if (STATUS_DRAFT.equals(event.getStatus()) || STATUS_CANCELLED.equals(event.getStatus())) {
            return TIME_UNPUBLISHED;
        }
        if (STATUS_CLOSED.equals(event.getStatus())) {
            return TIME_ENDED;
        }
        if (event.getStartAt() != null && now.isBefore(event.getStartAt())) {
            return TIME_NOT_STARTED;
        }
        if (event.getEndAt() != null && now.isAfter(event.getEndAt())) {
            return TIME_ENDED;
        }
        return TIME_ONGOING;
    }

    /**
     * 当前是否接受投稿：必须是已发布状态且处于活动时间窗口内。
     */
    public static boolean isAcceptingSubmission(CreativeEvent event, LocalDateTime now) {
        return TIME_ONGOING.equals(timeStatusOf(event, now));
    }

    /**
     * 公开活动列表，只返回已发布（含已结束）的活动。
     */
    public List<CreativeEvent> listLiveEvents() {
        LambdaQueryWrapper<CreativeEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CreativeEvent::getStatus, List.of(STATUS_PUBLISHED, STATUS_CLOSED))
                .orderByDesc(CreativeEvent::getStartAt)
                .orderByDesc(CreativeEvent::getId);
        return eventMapper.selectList(wrapper);
    }

    /**
     * 后台活动列表，含草稿与已取消。
     */
    public List<CreativeEvent> listAllEvents() {
        LambdaQueryWrapper<CreativeEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(CreativeEvent::getId);
        return eventMapper.selectList(wrapper);
    }

    /**
     * 取活动，找不到抛 404。
     */
    public CreativeEvent requireEvent(Long id) {
        CreativeEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw BusinessException.notFound("活动不存在");
        }
        return event;
    }

    /**
     * 取公开可见的活动（已发布或已结束）。
     */
    public CreativeEvent requirePublicEvent(Long id) {
        CreativeEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw BusinessException.notFound("活动不存在");
        }
        if (STATUS_DRAFT.equals(event.getStatus()) || STATUS_CANCELLED.equals(event.getStatus())) {
            throw BusinessException.notFound("活动不存在");
        }
        return event;
    }

    @Transactional
    public CreativeEvent create(EventSaveRequest request, Long creatorId) {
        validateTimeRange(request);

        CreativeEvent event = new CreativeEvent();
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setRules(request.getRules());
        event.setCoverUrl(request.getCoverUrl());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : STATUS_DRAFT);
        event.setCreatorId(creatorId);
        eventMapper.insert(event);
        return event;
    }

    @Transactional
    public CreativeEvent update(Long id, EventSaveRequest request) {
        CreativeEvent event = requireEvent(id);
        validateTimeRange(request);

        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setRules(request.getRules());
        event.setCoverUrl(request.getCoverUrl());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        if (StringUtils.hasText(request.getStatus())) {
            event.setStatus(request.getStatus());
        }
        eventMapper.updateById(event);
        return event;
    }

    /**
     * 发布活动：草稿变为 PUBLISHED，开始接受投稿。
     */
    @Transactional
    public CreativeEvent publish(Long id) {
        CreativeEvent event = requireEvent(id);
        if (STATUS_PUBLISHED.equals(event.getStatus())) {
            throw new BusinessException("活动已发布");
        }
        if (STATUS_CLOSED.equals(event.getStatus())) {
            throw new BusinessException("已结束的活动不能重新发布");
        }
        event.setStatus(STATUS_PUBLISHED);
        eventMapper.updateById(event);
        return event;
    }

    /**
     * 结束活动：停止投稿，之后可确认优秀作品。
     */
    @Transactional
    public CreativeEvent close(Long id) {
        CreativeEvent event = requireEvent(id);
        if (STATUS_CLOSED.equals(event.getStatus())) {
            throw new BusinessException("活动已结束");
        }
        event.setStatus(STATUS_CLOSED);
        eventMapper.updateById(event);
        return event;
    }

    @Transactional
    public void delete(Long id) {
        CreativeEvent event = requireEvent(id);
        eventMapper.deleteById(event.getId());
    }

    private void validateTimeRange(EventSaveRequest request) {
        if (request.getStartAt() == null || request.getEndAt() == null) {
            return;
        }
        if (!request.getEndAt().isAfter(request.getStartAt())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
    }

    /** 空列表常量，避免各处重复 */
    static List<String> noIds() {
        return Collections.emptyList();
    }

    /**
     * 用于日志与错误提示的状态中文名。
     */
    static String statusText(String status) {
        return switch (status == null ? "" : status) {
            case STATUS_DRAFT -> "草稿";
            case STATUS_PUBLISHED -> "已发布";
            case STATUS_CLOSED -> "已结束";
            case STATUS_CANCELLED -> "已取消";
            default -> status;
        };
    }
}
