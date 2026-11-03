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

    /** 作品形态分类（科普图文 / 海报 / 漫画 / 短视频） */
    private Long categoryId;

    /**
     * 所属知识分类（science_category.type = ARTICLE）。
     *
     * <p>可为空：作品不强制归入某个学科。与 {@link #categoryId} 的区别是——
     * 后者描述「这是一件什么形态的作品」，前者描述「它讲的是哪个领域的知识」。</p>
     */
    private Long knowledgeCategoryId;

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
