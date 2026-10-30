package com.science.platform.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改账号状态请求（超级管理员）。
 */
@Data
public class UserStatusUpdateRequest {

    @NotBlank(message = "请选择账号状态")
    @Pattern(regexp = "ACTIVE|DISABLED", message = "账号状态只能是 ACTIVE 或 DISABLED")
    private String status;

    @Size(max = 200, message = "说明不能超过 200 个字符")
    private String reason;
}
