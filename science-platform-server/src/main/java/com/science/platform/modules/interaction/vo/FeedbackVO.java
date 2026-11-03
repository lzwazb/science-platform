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

    /**
     * 可跳转目标的类型（ARTICLE / WORK）。
     *
     * <p>当反馈指向评论时，这里回溯到评论所属的文章或作品，
     * 前端据此拼出可点击的地址；目标已删除时为空。</p>
     */
    private String linkType;

    /** 可跳转目标的 ID，与 {@link #linkType} 配对使用 */
    private Long linkId;

    private String feedbackType;

    private String description;

    private String status;

    private Long handlerId;

    private String handlerName;

    private String handleComment;

    private LocalDateTime createdAt;

    private LocalDateTime handledAt;
}
