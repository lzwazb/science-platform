package com.science.platform.modules.event.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 主题创作活动实体。
 *
 * <p>注意：前台展示的「未开始 / 进行中 / 已结束」不落库，由 {@code startAt}、{@code endAt}
 * 与当前时间共同判定。如果把时间状态也存成字段，时间一过就会出现状态与实际不一致。</p>
 */
@Data
@TableName("creative_event")
public class CreativeEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    /** 参与规则（纯文本，前台展示） */
    private String rules;

    private String coverUrl;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    /** DRAFT / PUBLISHED / CLOSED / CANCELLED */
    private String status;

    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
