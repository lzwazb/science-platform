package com.science.platform.modules.event.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动投稿列表项。
 */
@Data
public class SubmissionListItemVO {

    private Long id;

    private Long eventId;

    private String eventTitle;

    private Long workId;

    private String workTitle;

    private String workCover;

    private String workDescription;

    private Long submitterId;

    private String submitterName;

    /** SUBMITTED / UNDER_REVIEW / REVIEWED / SELECTED / NOT_SELECTED / WITHDRAWN */
    private String status;

    /** FIRST / SECOND / THIRD / EXCELLENT，未获奖为空 */
    private String awardLevel;

    private LocalDateTime submittedAt;

    /** 平均分，无评审为空 */
    private Double averageScore;

    /** 已收到的评审条数 */
    private long reviewCount;

    /** 当前评审员是否已评过这一条（审核台用来提示「已评，可修改」） */
    private boolean reviewedByMe;
}
