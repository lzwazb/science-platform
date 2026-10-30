package com.science.platform.modules.event.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动详情。
 */
@Data
public class EventDetailVO {

    private Long id;

    private String title;

    private String description;

    private String rules;

    private String coverUrl;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private String status;

    private String timeStatus;

    private boolean acceptingSubmission;

    private Long submissionCount;

    private LocalDateTime createdAt;

    /** 优秀作品（活动结束后由管理员确认） */
    private List<AwardVO> awards;

    /** 当前登录用户在本活动中的投稿（未登录为空） */
    private List<SubmissionListItemVO> mySubmissions;
}
