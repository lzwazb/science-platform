package com.science.platform.modules.contribution.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章版本列表项。
 */
@Data
public class ArticleVersionListItemVO {

    private Long id;

    private Long articleId;

    private Integer versionNo;

    private String changeNote;

    private Long operatorId;

    private String operatorName;

    /** 来源知识贡献 ID，可追溯到是哪条建议导致的这次修改 */
    private Long sourceContributionId;

    private LocalDateTime createdAt;
}
