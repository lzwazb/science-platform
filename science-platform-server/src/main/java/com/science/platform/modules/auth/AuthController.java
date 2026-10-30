package com.science.platform.modules.auth;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.auth.dto.LoginRequest;
import com.science.platform.modules.auth.dto.RegisterRequest;
import com.science.platform.modules.auth.service.AuthService;
import com.science.platform.modules.auth.vo.LoginResponse;
import com.science.platform.modules.user.service.UserService;
import com.science.platform.modules.user.vo.UserProfileVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 *
 * <p>只做参数接收与响应包装，业务规则在 {@link AuthService}。</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * 公开注册普通用户。
     */
    @PostMapping("/register")
    public ApiResponse<UserProfileVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功，请登录", authService.register(request));
    }

    /**
     * 登录并获取令牌。
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    /**
     * 获取当前登录用户。
     *
     * <p>用户 ID 取自认证上下文，不接受前端传参。</p>
     */
    @GetMapping("/me")
    public ApiResponse<UserProfileVO> me() {
        return ApiResponse.success(userService.getProfile(SecurityUtils.currentUserId()));
    }
}
