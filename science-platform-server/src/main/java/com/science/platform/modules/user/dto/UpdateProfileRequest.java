package com.science.platform.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新个人资料请求。
 *
 * <p>只允许改昵称、头像、邮箱、简介。用户名、角色、账号状态一律不可经由本接口修改
 * （开发档案 FR-ME-01：用户不能自行修改角色和账号状态）。</p>
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 20, message = "昵称长度不能超过 20 个字符")
    private String nickname;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
    private String email;

    @Size(max = 500, message = "个人简介不能超过 500 个字符")
    private String bio;
}
