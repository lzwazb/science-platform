package com.science.platform.modules.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章详情。
 *
 * <p>附带所属专题与（阶段 5 起）关联创意作品，体现「知识与作品关联」这一项目特色。</p>
 */
@Data
public class ArticleDetailVO {

    private Long id;

    private String title;

    private String summary;

    private String content;

    private String coverUrl;

    private Long categoryId;

    private String categoryName;

    private Long authorId;

    private String authorName;

    /** PUBLISHED / DRAFT / OFFLINE */
    private String status;

    private LocalDateTime publishedAt;

    private Integer viewCount;

    private Integer versionNo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 所属专题（简要信息） */
    private List<TopicBriefVO> topics;

    /** 关联的已发布创意作品（阶段 5 起填充，体现「知识与作品关联」） */
    private List<com.science.platform.modules.work.vo.WorkBriefVO> works;
}
