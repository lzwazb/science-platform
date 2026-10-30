package com.science.platform.modules.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章列表项（公开列表与后台列表共用）。
 *
 * <p>不含正文，避免列表接口返回大量长文本。</p>
 */
@Data
public class ArticleListItemVO {

    private Long id;

    private String title;

    private String summary;

    private String coverUrl;

    private Long categoryId;

    private String categoryName;

    private Long authorId;

    private String authorName;

    /** PUBLISHED / DRAFT / OFFLINE，后台列表需要；公开接口只会返回 PUBLISHED */
    private String status;

    private LocalDateTime publishedAt;

    private Integer viewCount;

    private Integer versionNo;
}
