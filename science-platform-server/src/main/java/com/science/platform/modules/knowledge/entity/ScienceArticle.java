package com.science.platform.modules.knowledge.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 正式科普文章实体。
 *
 * <p>注意：实体不直接作为接口返回对象，对外展示应使用 VO，避免暴露无关字段。</p>
 */
@Data
@TableName("science_article")
public class ScienceArticle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    private String content;

    private String coverUrl;

    private Long categoryId;

    private Long authorId;

    /** DRAFT / PUBLISHED / OFFLINE */
    private String status;

    private LocalDateTime publishedAt;

    private Integer viewCount;

    private Integer versionNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
