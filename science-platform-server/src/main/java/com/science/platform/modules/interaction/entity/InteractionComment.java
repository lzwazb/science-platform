package com.science.platform.modules.interaction.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体。
 *
 * <p>通过 {@code targetType + targetId} 指向文章或作品，不使用外键，
 * 目标存在性由 Service 校验。</p>
 */
@Data
@TableName("interaction_comment")
public class InteractionComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** ARTICLE / WORK */
    private String targetType;

    private Long targetId;

    /** 父评论 ID，支持一层回复 */
    private Long parentId;

    private String content;

    /** VISIBLE / HIDDEN / DELETED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
