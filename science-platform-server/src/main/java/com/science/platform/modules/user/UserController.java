package com.science.platform.modules.user;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.user.dto.UpdateProfileRequest;
import com.science.platform.modules.user.service.UserOverviewService;
import com.science.platform.modules.user.service.UserService;
import com.science.platform.modules.user.vo.UserOverviewVO;
import com.science.platform.modules.user.vo.UserProfileVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人资料与个人中心概况接口。
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserOverviewService userOverviewService;

    public UserController(UserService userService, UserOverviewService userOverviewService) {
        this.userService = userService;
        this.userOverviewService = userOverviewService;
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

    /**
     * 个人中心概况：各类数量一次返回，避免前端逐个 tab 请求。
     */
    @GetMapping("/me/overview")
    public ApiResponse<UserOverviewVO> overview() {
        return ApiResponse.success(userOverviewService.overviewOf(SecurityUtils.currentUserId()));
    }
}
