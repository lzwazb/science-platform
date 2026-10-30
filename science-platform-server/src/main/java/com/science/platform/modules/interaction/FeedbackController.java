package com.science.platform.modules.interaction;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.interaction.dto.FeedbackCreateRequest;
import com.science.platform.modules.interaction.service.FeedbackService;
import com.science.platform.modules.interaction.vo.FeedbackVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容反馈（用户侧）。
 */
@Validated
@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * 提交内容反馈。
     */
    @PostMapping
    public ApiResponse<FeedbackVO> create(@Valid @RequestBody FeedbackCreateRequest request) {
        return ApiResponse.success("反馈已提交，管理员会尽快处理",
                feedbackService.create(request, SecurityUtils.currentUserId()));
    }

    /**
     * 我的反馈记录。
     */
    @GetMapping("/mine")
    public ApiResponse<PageResult<FeedbackVO>> mine(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size) {
        return ApiResponse.success(PageResult.of(
                feedbackService.pageMine(SecurityUtils.currentUserId(), page, size)));
    }
}
