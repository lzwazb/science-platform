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

    /** 作者昵称，作品详情页的卡片要按知识库列表的样式展示「作者 + 阅读数」 */
    private String authorName;

    private Integer viewCount;
}
