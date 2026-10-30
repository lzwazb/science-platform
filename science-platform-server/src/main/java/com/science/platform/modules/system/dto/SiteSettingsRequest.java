package com.science.platform.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 平台基础信息维护请求（超级管理员）。
 */
@Data
public class SiteSettingsRequest {

    @NotBlank(message = "平台名称不能为空")
    @Size(max = 60, message = "平台名称不能超过 60 个字符")
    private String name;

    @Size(max = 120, message = "副标题不能超过 120 个字符")
    private String subtitle;

    @NotBlank(message = "平台简介不能为空")
    @Size(max = 500, message = "平台简介不能超过 500 个字符")
    private String description;

    @Size(max = 120, message = "首页横幅文案不能超过 120 个字符")
    private String banner;

    @Size(max = 100, message = "联系邮箱不能超过 100 个字符")
    private String contact;
}
