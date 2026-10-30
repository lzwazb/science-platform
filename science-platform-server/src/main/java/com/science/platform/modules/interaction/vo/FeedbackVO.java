package com.science.platform.modules.interaction.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容反馈展示对象。
 */
@Data
public class FeedbackVO {

    private Long id;

    private Long userId;

    private String userName;

    private String targetType;

    private Long targetId;

    /** 目标标题，便于后台直接看出反馈的是哪条内容 */
    private String targetTitle;

    private String feedbackType;

    private String description;

    private String status;

    private Long handlerId;

    private String handlerName;

    private String handleComment;

    private LocalDateTime createdAt;

    private LocalDateTime handledAt;
}
