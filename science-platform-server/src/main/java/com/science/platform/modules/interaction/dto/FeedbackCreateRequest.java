package com.science.platform.modules.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交内容反馈请求。
 */
@Data
public class FeedbackCreateRequest {

    @NotBlank(message = "请指定反馈目标类型")
    @Pattern(regexp = "ARTICLE|WORK|COMMENT", message = "反馈目标类型只能是 ARTICLE、WORK 或 COMMENT")
    private String targetType;

    @NotNull(message = "请指定反馈目标")
    private Long targetId;

    @NotBlank(message = "请选择反馈类型")
    @Pattern(regexp = "WRONG_CONTENT|COPYRIGHT|SPAM|SUGGESTION|OTHER",
            message = "反馈类型不合法")
    private String feedbackType;

    @NotBlank(message = "反馈说明不能为空")
    @Size(max = 1000, message = "反馈说明不能超过 1000 个字符")
    private String description;
}
