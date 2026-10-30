package com.science.platform.modules.user;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.user.dto.UpdateProfileRequest;
import com.science.platform.modules.user.service.UserService;
import com.science.platform.modules.user.vo.UserProfileVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人资料接口。
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 查看本人资料。
     */
    @GetMapping("/me")
    public ApiResponse<UserProfileVO> me() {
        return ApiResponse.success(userService.getProfile(SecurityUtils.currentUserId()));
    }

    /**
     * 更新本人资料。
     */
    @PutMapping("/me")
    public ApiResponse<UserProfileVO> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("资料已更新",
                userService.updateProfile(SecurityUtils.currentUserId(), request));
    }
}
