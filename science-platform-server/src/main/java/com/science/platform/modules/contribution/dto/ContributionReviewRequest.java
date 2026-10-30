package com.science.platform.modules.contribution.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审核知识贡献请求。
 *
 * <p>{@code finalContent} 是「最终采用的完整正文」。开发档案 5.1 要求
 * 「通过时必须明确最终采用的修改内容」，所以评审员可以在通过时先合并建议、
 * 编辑出完整正文再提交；不填则后端以 {@code suggestedContent} 作为最终内容。</p>
 */
@Data
public class ContributionReviewRequest {

    @Size(max = 1000, message = "审核意见不能超过 1000 个字符")
    private String comment;

    @Size(max = 100000, message = "最终正文过长")
    private String finalContent;
}
