package com.science.platform.modules.notification;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.notification.service.NotificationService;
import com.science.platform.modules.notification.vo.NotificationVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 站内消息接口。
 *
 * <p>所有操作都基于当前登录用户，不接受前端传入 recipientId。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 我的消息分页。
     *
     * @param onlyUnread 只看未读
     */
    @GetMapping
    public ApiResponse<PageResult<NotificationVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size,
            @RequestParam(defaultValue = "false") boolean onlyUnread) {

        return ApiResponse.success(PageResult.of(
                notificationService.pageMine(SecurityUtils.currentUserId(), page, size, onlyUnread)));
    }

    /**
     * 未读消息数，用于顶栏小红点。
     */
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> unreadCount() {
        long count = notificationService.countUnread(SecurityUtils.currentUserId());
        return ApiResponse.success(Map.of("unreadCount", count));
    }

    /**
     * 标记单条消息已读。
     */
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id, SecurityUtils.currentUserId());
        return ApiResponse.success("已标记为已读", null);
    }

    /**
     * 全部标记已读。
     */
    @PutMapping("/read-all")
    public ApiResponse<Map<String, Object>> markAllRead() {
        int affected = notificationService.markAllRead(SecurityUtils.currentUserId());
        return ApiResponse.success("已全部标记为已读", Map.of("updated", affected));
    }
}
