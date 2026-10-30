package com.science.platform.modules.knowledge.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章 / 作品分类实体，通过 {@code type} 区分归属。
 */
@Data
@TableName("science_category")
public class ScienceCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** ARTICLE 文章分类 / WORK 作品分类 */
    private String type;

    private String description;

    private Integer sortOrder;

    /** ENABLED / DISABLED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
