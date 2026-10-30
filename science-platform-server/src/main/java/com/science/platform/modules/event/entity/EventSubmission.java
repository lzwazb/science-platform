package com.science.platform.modules.event.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动作品投稿实体。
 *
 * <p>表上有 {@code uk_event_work(event_id, work_id)}，同一作品在同一活动只能投稿一次。</p>
 */
@Data
@TableName("event_submission")
public class EventSubmission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long eventId;

    private Long workId;

    private Long submitterId;

    /** SUBMITTED / UNDER_REVIEW / REVIEWED / SELECTED / NOT_SELECTED / WITHDRAWN */
    private String status;

    /** FIRST / SECOND / THIRD / EXCELLENT，仅在管理员确认优秀作品后写入 */
    private String awardLevel;

    private LocalDateTime submittedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
