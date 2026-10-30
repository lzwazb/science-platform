package com.science.platform.modules.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 专题创建 / 编辑请求（后台使用）。
 */
@Data
public class TopicSaveRequest {

    @NotBlank(message = "专题标题不能为空")
    @Size(max = 100, message = "专题标题不能超过 100 个字符")
    private String title;

    @Size(max = 500, message = "专题简介不能超过 500 个字符")
    private String description;

    @Size(max = 255, message = "封面地址过长")
    private String coverUrl;

    private Integer sortOrder;

    @Pattern(regexp = "DRAFT|PUBLISHED|OFFLINE", message = "专题状态只能是 DRAFT、PUBLISHED 或 OFFLINE")
    private String status;
}
