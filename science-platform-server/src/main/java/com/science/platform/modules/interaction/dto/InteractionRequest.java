package com.science.platform.modules.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 点赞 / 收藏请求。取消操作复用同一结构。
 */
@Data
public class InteractionRequest {

    @NotBlank(message = "请指定目标类型")
    @Pattern(regexp = "ARTICLE|WORK", message = "目标类型只能是 ARTICLE 或 WORK")
    private String targetType;

    @NotNull(message = "请指定目标 ID")
    private Long targetId;
}
