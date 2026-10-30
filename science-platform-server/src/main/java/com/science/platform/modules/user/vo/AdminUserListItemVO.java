package com.science.platform.modules.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台用户列表项（超级管理员可见）。
 */
@Data
public class AdminUserListItemVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    /** ACTIVE / DISABLED */
    private String status;

    private List<String> roles;

    private LocalDateTime createdAt;
}
