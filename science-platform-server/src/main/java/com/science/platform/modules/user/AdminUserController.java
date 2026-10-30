package com.science.platform.modules.user;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.user.dto.UserRoleUpdateRequest;
import com.science.platform.modules.user.dto.UserStatusUpdateRequest;
import com.science.platform.modules.user.service.AdminUserService;
import com.science.platform.modules.user.vo.AdminUserListItemVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号与角色管理（仅超级管理员）。
 *
 * <p>路径 {@code /api/v1/admin/users/**} 在安全配置里已限定 SUPER_ADMIN，
 * 这里再加 {@code @PreAuthorize} 形成双重保险。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 用户分页查询。
     */
    @GetMapping
    public ApiResponse<PageResult<AdminUserListItemVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String status) {

        return ApiResponse.success(PageResult.of(
                adminUserService.pageUsers(page, size, keyword, roleCode, status)));
    }

    /**
     * 重新分配用户角色。
     */
    @PutMapping("/{id}/roles")
    public ApiResponse<AdminUserListItemVO> updateRoles(@PathVariable Long id,
                                                        @Valid @RequestBody UserRoleUpdateRequest request) {
        return ApiResponse.success("角色已更新",
                adminUserService.updateRoles(id, request, SecurityUtils.currentUserId()));
    }

    /**
     * 启用 / 禁用账号。
     */
    @PutMapping("/{id}/status")
    public ApiResponse<AdminUserListItemVO> updateStatus(@PathVariable Long id,
                                                         @Valid @RequestBody UserStatusUpdateRequest request) {
        return ApiResponse.success("账号状态已更新",
                adminUserService.updateStatus(id, request, SecurityUtils.currentUserId()));
    }
}
