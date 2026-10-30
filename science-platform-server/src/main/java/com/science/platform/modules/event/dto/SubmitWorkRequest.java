package com.science.platform.modules.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 作品投稿请求。
 */
@Data
public class SubmitWorkRequest {

    @NotNull(message = "请选择要投稿的作品")
    private Long workId;
}
