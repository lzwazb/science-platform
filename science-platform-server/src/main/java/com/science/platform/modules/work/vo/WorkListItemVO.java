package com.science.platform.modules.work.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作品列表项（公开列表、我的作品、审核工作台共用）。
 */
@Data
public class WorkListItemVO {

    private Long id;

    private String title;

    private String description;

    private String coverUrl;

    private Long categoryId;

    private String categoryName;

    private Long creatorId;

    private String creatorName;

    /** DRAFT / PENDING / PUBLISHED / REJECTED / OFFLINE */
    private String status;

    private Integer viewCount;

    /** 审核意见，仅在我的作品与审核工作台中有意义 */
    private String reviewComment;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 关联的文章数量，列表页展示「关联 N 篇」 */
    private Integer articleCount;

    /** 关联文章标题，仅在我的作品与详情中填充（用于快速识别） */
    private List<String> articleTitles;
}
