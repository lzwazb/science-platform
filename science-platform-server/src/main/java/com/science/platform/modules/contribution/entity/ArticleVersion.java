package com.science.platform.modules.contribution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章历史版本实体。
 *
 * <p>语义：{@code version_no} 记录的是**被覆盖之前的旧内容**。
 * 审核通过时先把当前正文以当前 {@code version_no} 存入本表，再把文章的
 * {@code version_no} 加一。因此 {@code science_article.version_no = N} 时，
 * 本表中最多存在到 {@code N-1} 号快照。</p>
 */
@Data
@TableName("article_version")
public class ArticleVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Integer versionNo;

    private String titleSnapshot;

    private String summarySnapshot;

    private String contentSnapshot;

    /** 变更说明，例如「根据知识贡献 #12 采纳读者纠错」 */
    private String changeNote;

    /** 操作者（审核员）ID */
    private Long operatorId;

    /** 来源知识贡献 ID，实现「从历史版本反查是哪条贡献导致的修改」 */
    private Long sourceContributionId;

    private LocalDateTime createdAt;
}
