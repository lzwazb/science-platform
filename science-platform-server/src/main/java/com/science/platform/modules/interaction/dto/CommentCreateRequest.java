package com.science.platform.modules.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论请求。
 */
@Data
public class CommentCreateRequest {

    @NotBlank(message = "请指定评论目标类型")
    @Pattern(regexp = "ARTICLE|WORK", message = "评论目标类型只能是 ARTICLE 或 WORK")
    private String targetType;

    @NotNull(message = "请指定评论目标")
    private Long targetId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过 1000 个字符")
    private String content;

    /** 父评论 ID，用于回复；可为空 */
    private Long parentId;
}
