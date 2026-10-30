package com.science.platform.modules.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 处理内容反馈请求（后台）。
 */
@Data
public class FeedbackHandleRequest {

    @NotBlank(message = "请选择处理结果")
    @Pattern(regexp = "PROCESSING|RESOLVED|REJECTED",
            message = "处理结果只能是处理中、已处理或驳回")
    private String status;

    @Size(max = 500, message = "处理说明不能超过 500 个字符")
    private String handleComment;
}
