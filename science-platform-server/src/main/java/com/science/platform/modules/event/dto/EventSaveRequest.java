package com.science.platform.modules.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动创建 / 编辑请求。
 *
 * <p>起止时间由 Service 校验先后顺序（Bean Validation 表达不了跨字段比较）。</p>
 */
@Data
public class EventSaveRequest {

    @NotBlank(message = "活动标题不能为空")
    @Size(max = 150, message = "活动标题不能超过 150 个字符")
    private String title;

    @Size(max = 2000, message = "活动说明不能超过 2000 个字符")
    private String description;

    @Size(max = 3000, message = "参与规则不能超过 3000 个字符")
    private String rules;

    @Size(max = 255, message = "封面地址过长")
    private String coverUrl;

    @NotNull(message = "请设置开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startAt;

    @NotNull(message = "请设置结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endAt;

    @Pattern(regexp = "DRAFT|PUBLISHED|CLOSED|CANCELLED",
            message = "活动状态只能是草稿、已发布、已结束或已取消")
    private String status;
}
