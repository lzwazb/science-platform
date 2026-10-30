package com.science.platform.modules.contribution;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.contribution.dto.ContributionReviewRequest;
import com.science.platform.modules.contribution.service.ContributionReviewService;
import com.science.platform.modules.contribution.vo.ContributionDetailVO;
import com.science.platform.modules.contribution.vo.ContributionListItemVO;
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
 * 知识贡献审核工作台。
 *
 * <p>可访问角色：评审员、管理员、超级管理员。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/review/contributions")
@PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN', 'SUPER_ADMIN')")
public class ContributionReviewController {

    private final ContributionReviewService reviewService;

    public ContributionReviewController(ContributionReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 待审核建议列表。
     *
     * @param status 状态筛选，默认 PENDING
     */
    @GetMapping
    public ApiResponse<PageResult<ContributionListItemVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long articleId) {

        return ApiResponse.success(PageResult.of(
                reviewService.pageForReview(page, size, status, articleId)));
    }

    /**
     * 审核详情：文章当前正文 + 用户建议 + 理由与来源。
     */
    @GetMapping("/{id}")
    public ApiResponse<ContributionDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(reviewService.getReviewDetail(id));
    }

    /**
     * 通过：更新正式文章、保存历史版本、写通知。
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<ContributionDetailVO> approve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ContributionReviewRequest request) {

        ContributionReviewRequest payload = request == null ? new ContributionReviewRequest() : request;
        return ApiResponse.success("已通过，文章已更新",
                reviewService.approve(id, payload, SecurityUtils.currentUserId()));
    }

    /**
     * 驳回：必须填写审核意见。
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<ContributionDetailVO> reject(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ContributionReviewRequest request) {

        ContributionReviewRequest payload = request == null ? new ContributionReviewRequest() : request;
        return ApiResponse.success("已驳回",
                reviewService.reject(id, payload, SecurityUtils.currentUserId()));
    }
}
