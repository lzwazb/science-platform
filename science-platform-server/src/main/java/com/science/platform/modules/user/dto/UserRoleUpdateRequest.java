package com.science.platform.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 分配用户角色请求（超级管理员）。
 */
@Data
public class UserRoleUpdateRequest {

    @NotEmpty(message = "请至少选择一个角色")
    private List<String> roleCodes;

    @NotBlank(message = "请说明调整原因")
    private String reason;
}
