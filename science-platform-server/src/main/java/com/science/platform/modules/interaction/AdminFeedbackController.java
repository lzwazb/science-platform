package com.science.platform.modules.interaction;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.interaction.dto.FeedbackHandleRequest;
import com.science.platform.modules.interaction.service.FeedbackService;
import com.science.platform.modules.interaction.vo.FeedbackVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容反馈处理（后台）。
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin/feedback")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    public AdminFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * 反馈列表。
     *
     * @param status 状态筛选：PENDING / PROCESSING / RESOLVED / REJECTED，为空返回全部
     */
    @GetMapping
    public ApiResponse<PageResult<FeedbackVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(PageResult.of(feedbackService.pageAll(page, size, status)));
    }

    /**
     * 处理反馈。
     */
    @PostMapping("/{id}/handle")
    public ApiResponse<FeedbackVO> handle(@PathVariable Long id,
                                          @Valid @RequestBody FeedbackHandleRequest request) {
        return ApiResponse.success("反馈已处理",
                feedbackService.handle(id, request, SecurityUtils.currentUserId()));
    }
}
