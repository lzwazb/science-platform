package com.science.platform.modules.work.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作品详情。
 */
@Data
public class WorkDetailVO {

    private Long id;

    private String title;

    private String description;

    private String content;

    private String coverUrl;

    private Long categoryId;

    private String categoryName;

    private Long creatorId;

    private String creatorName;

    /** 创作者简介，用于详情页作者卡片 */
    private String creatorBio;

    /** DRAFT / PENDING / PUBLISHED / REJECTED / OFFLINE */
    private String status;

    private String reviewComment;

    private LocalDateTime reviewedAt;

    private Integer viewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 关联的科普文章 */
    private List<ArticleBriefVO> articles;
}
