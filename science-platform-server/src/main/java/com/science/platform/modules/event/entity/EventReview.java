package com.science.platform.modules.event.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动评审记录实体。
 *
 * <p>表上有 {@code uk_submission_reviewer(submission_id, reviewer_id)}，
 * 同一评审员对同一投稿只保留一份有效评审。</p>
 */
@Data
@TableName("event_review")
public class EventReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submissionId;

    private Long reviewerId;

    /** 评分，统一 1–5 */
    private Integer score;

    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
