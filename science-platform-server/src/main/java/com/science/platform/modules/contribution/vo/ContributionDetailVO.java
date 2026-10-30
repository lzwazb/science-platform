package com.science.platform.modules.contribution.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识贡献详情。
 *
 * <p>额外带上文章当前正文，方便评审员把建议与原文对照着看——
 * 审核工作台的核心就是「原文 vs 建议」两栏比对。</p>
 */
@Data
public class ContributionDetailVO {

    private Long id;

    private Long articleId;

    private String articleTitle;

    /** 文章当前正文，供评审对照 */
    private String articleContent;

    private Integer articleVersionNo;

    private Long submitterId;

    private String submitterName;

    private String contributionType;

    private String suggestedContent;

    private String reason;

    private String referenceSource;

    private String status;

    private Long reviewerId;

    private String reviewerName;

    private String reviewComment;

    private String finalContent;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
