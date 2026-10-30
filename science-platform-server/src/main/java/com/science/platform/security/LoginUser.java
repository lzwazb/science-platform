package com.science.platform.security;

import lombok.Data;

import java.util.List;

/**
 * 放入 Spring Security 上下文的登录主体。
 *
 * <p>只保留鉴权与展示需要的字段，不含密码哈希。</p>
 */
@Data
public class LoginUser {

    private Long id;

    private String username;

    private String nickname;

    /** 角色编码，如 [USER, CREATOR] */
    private List<String> roles;

    public LoginUser() {
    }

    public LoginUser(Long id, String username, String nickname, List<String> roles) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.roles = roles;
    }
}
