package com.science.platform.modules.contribution.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章历史版本详情。
 */
@Data
public class ArticleVersionDetailVO {

    private Long id;

    private Long articleId;

    private String articleTitle;

    private Integer versionNo;

    private String titleSnapshot;

    private String summarySnapshot;

    private String contentSnapshot;

    private String changeNote;

    private Long operatorId;

    private String operatorName;

    private Long sourceContributionId;

    private LocalDateTime createdAt;
}
