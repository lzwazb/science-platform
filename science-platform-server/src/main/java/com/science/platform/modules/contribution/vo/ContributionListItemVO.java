package com.science.platform.modules.contribution.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识贡献列表项。
 */
@Data
public class ContributionListItemVO {

    private Long id;

    private Long articleId;

    private String articleTitle;

    private Long submitterId;

    private String submitterName;

    /** SUPPLEMENT / CORRECTION / MODIFICATION */
    private String contributionType;

    /** PENDING / APPROVED / REJECTED / WITHDRAWN */
    private String status;

    private String reason;

    private String reviewComment;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;
}
