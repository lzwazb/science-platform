package com.science.platform.modules.contribution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交知识贡献请求。
 */
@Data
public class ContributionSubmitRequest {

    @NotNull(message = "请指定要改进的文章")
    private Long articleId;

    @NotBlank(message = "请选择建议类型")
    @Pattern(regexp = "SUPPLEMENT|CORRECTION|MODIFICATION",
            message = "建议类型只能是补充、纠错或修改建议")
    private String contributionType;

    @NotBlank(message = "建议内容不能为空")
    @Size(max = 5000, message = "建议内容不能超过 5000 个字符")
    private String suggestedContent;

    @NotBlank(message = "提交理由不能为空")
    @Size(max = 1000, message = "提交理由不能超过 1000 个字符")
    private String reason;

    /** 参考来源按开发档案「推荐必填」实现为必填 */
    @NotBlank(message = "参考来源不能为空")
    @Size(max = 500, message = "参考来源不能超过 500 个字符")
    private String referenceSource;
}
