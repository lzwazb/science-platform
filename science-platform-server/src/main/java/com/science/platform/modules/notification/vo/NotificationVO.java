package com.science.platform.modules.notification.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息展示对象。
 */
@Data
public class NotificationVO {

    private Long id;

    /** REVIEW_RESULT / COMMENT_REPLY / EVENT_NOTICE / SYSTEM */
    private String type;

    /** 类型的中文名，前端直接展示，避免前端再维护一份映射 */
    private String typeLabel;

    private String title;

    private String content;

    /** ARTICLE / WORK / CONTRIBUTION / EVENT */
    private String relatedType;

    private Long relatedId;

    /** 从消息跳转到业务详情的前端路径，无法跳转时为 null */
    private String linkPath;

    private boolean read;

    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}
