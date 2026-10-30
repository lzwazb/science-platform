package com.science.platform.modules.contribution.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识贡献实体（知识补充 / 纠错 / 修改建议）。
 *
 * <p>这是项目的差异化核心：读者提的建议不直接改文章，而是走一条带审核、
 * 带参考来源、带处理意见的正式流程。</p>
 */
@Data
@TableName("knowledge_contribution")
public class KnowledgeContribution {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Long submitterId;

    /** SUPPLEMENT 补充 / CORRECTION 纠错 / MODIFICATION 修改建议 */
    private String contributionType;

    /** 建议内容 */
    private String suggestedContent;

    /** 提交理由 */
    private String reason;

    /** 参考来源 */
    private String referenceSource;

    /** PENDING / APPROVED / REJECTED / WITHDRAWN */
    private String status;

    private Long reviewerId;

    private String reviewComment;

    /** 审核通过时最终采用的正文（可能经过评审员合并修改） */
    private String finalContent;

    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
