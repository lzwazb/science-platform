package com.science.platform.modules.event.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 活动投稿评审请求。
 */
@Data
public class SubmissionReviewRequest {

    @NotNull(message = "请填写评分")
    @Min(value = 1, message = "评分最低 1 分")
    @Max(value = 5, message = "评分最高 5 分")
    private Integer score;

    @NotBlank(message = "请填写评语")
    @Size(max = 1000, message = "评语不能超过 1000 个字符")
    private String comment;
}
