package com.science.platform.modules.event.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动列表项。
 */
@Data
public class EventListItemVO {

    private Long id;

    private String title;

    private String description;

    private String coverUrl;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    /** 管理状态：DRAFT / PUBLISHED / CLOSED / CANCELLED */
    private String status;

    /**
     * 时间状态（由服务端按当前时间计算，不落库）：
     * NOT_STARTED 未开始 / ONGOING 进行中 / ENDED 已结束 / UNPUBLISHED 未发布
     */
    private String timeStatus;

    /** 投稿总数 */
    private Long submissionCount;

    /** 是否还能投稿（进行中 且 已发布） */
    private boolean acceptingSubmission;

    private LocalDateTime createdAt;
}
