package com.science.platform.modules.work.vo;

import lombok.Data;

/**
 * 关联文章的简要信息，用于作品详情页展示「本作品关联了哪些科普文章」。
 */
@Data
public class ArticleBriefVO {

    private Long id;

    private String title;

    private String summary;

    private String coverUrl;

    private String categoryName;
}
