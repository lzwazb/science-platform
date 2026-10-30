package com.science.platform.modules.auth.vo;

import com.science.platform.modules.user.vo.UserProfileVO;
import lombok.Data;

/**
 * 登录成功响应。
 */
@Data
public class LoginResponse {

    /** JWT 访问令牌，前端放在 Authorization: Bearer 头中 */
    private String token;

    /** 令牌有效期（秒） */
    private long expiresIn;

    /** 令牌类型，固定 Bearer */
    private String tokenType = "Bearer";

    /** 登录用户信息，免去前端再发一次 /auth/me */
    private UserProfileVO user;
}
