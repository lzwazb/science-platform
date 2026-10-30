package com.science.platform.modules.interaction.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容反馈实体。
 */
@Data
@TableName("content_feedback")
public class ContentFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** ARTICLE / WORK / COMMENT */
    private String targetType;

    private Long targetId;

    /** WRONG_CONTENT / COPYRIGHT / SPAM / SUGGESTION / OTHER */
    private String feedbackType;

    private String description;

    /** PENDING / PROCESSING / RESOLVED / REJECTED */
    private String status;

    private Long handlerId;

    private String handleComment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime handledAt;
}
