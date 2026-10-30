package com.science.platform.modules.work.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 科普创意作品实体。
 *
 * <p>状态流转：DRAFT → PENDING → PUBLISHED / REJECTED；已发布作品被编辑后回到 PENDING 重新送审。</p>
 */
@Data
@TableName("creator_work")
public class CreatorWork {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    private String title;

    private String description;

    private String content;

    private String coverUrl;

    private Long categoryId;

    /** DRAFT / PENDING / PUBLISHED / REJECTED / OFFLINE */
    private String status;

    private Long reviewerId;

    private String reviewComment;

    private LocalDateTime reviewedAt;

    private Integer viewCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
