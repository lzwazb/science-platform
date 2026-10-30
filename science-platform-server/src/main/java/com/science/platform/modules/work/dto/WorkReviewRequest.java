package com.science.platform.modules.work.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 作品审核请求。
 *
 * <p>驳回时 {@code comment} 必填（由 Service 校验，因为同一 DTO 也用于通过场景）。</p>
 */
@Data
public class WorkReviewRequest {

    @Size(max = 1000, message = "审核意见不能超过 1000 个字符")
    private String comment;
}
