package com.science.platform.modules.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类创建 / 编辑请求（后台使用）。
 */
@Data
public class CategorySaveRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过 50 个字符")
    private String name;

    @NotBlank(message = "分类类型不能为空")
    @Pattern(regexp = "ARTICLE|WORK", message = "分类类型只能是 ARTICLE 或 WORK")
    private String type;

    @Size(max = 255, message = "分类说明不能超过 255 个字符")
    private String description;

    private Integer sortOrder;

    @Pattern(regexp = "ENABLED|DISABLED", message = "分类状态只能是 ENABLED 或 DISABLED")
    private String status;
}
