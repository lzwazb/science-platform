package com.science.platform.modules.knowledge.vo;

import lombok.Data;

/**
 * 分类展示对象。
 */
@Data
public class CategoryVO {

    private Long id;

    private String name;

    /** ARTICLE / WORK */
    private String type;

    private String description;

    private Integer sortOrder;

    /** ENABLED / DISABLED */
    private String status;
}
