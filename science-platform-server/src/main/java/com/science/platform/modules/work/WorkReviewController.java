package com.science.platform.modules.work;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.work.dto.WorkReviewRequest;
import com.science.platform.modules.work.service.WorkQueryService;
import com.science.platform.modules.work.service.WorkReviewService;
import com.science.platform.modules.work.vo.WorkDetailVO;
import com.science.platform.modules.work.vo.WorkListItemVO;
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
 * 作品审核工作台接口。
 *
 * <p>可访问角色：评审员、管理员、超级管理员。普通用户与创作者即使知道路径也拿不到数据（403）。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/review/works")
@PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN', 'SUPER_ADMIN')")
public class WorkReviewController {

    private final WorkReviewService workReviewService;
    private final WorkQueryService workQueryService;

    public WorkReviewController(WorkReviewService workReviewService, WorkQueryService workQueryService) {
        this.workReviewService = workReviewService;
        this.workQueryService = workQueryService;
    }

    /**
     * 作品列表，默认只看待审核。
     *
     * @param status 状态筛选，默认 PENDING
     */
    @GetMapping
    public ApiResponse<PageResult<WorkListItemVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        return ApiResponse.success(PageResult.of(
                workReviewService.pageWorks(page, size, status, keyword)));
    }

    /**
     * 审核详情，含作品全文与关联文章。
     */
    @GetMapping("/{id}")
    public ApiResponse<WorkDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(workQueryService.getDetailAnyStatus(id));
    }

    /**
     * 审核通过。
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<WorkDetailVO> approve(@PathVariable Long id,
                                             @Valid @RequestBody(required = false) WorkReviewRequest request) {
        String comment = request == null ? null : request.getComment();
        return ApiResponse.success("作品已通过审核",
                workReviewService.approve(id, comment, SecurityUtils.currentUserId()));
    }

    /**
     * 驳回，必须填写审核意见。
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<WorkDetailVO> reject(@PathVariable Long id,
                                            @Valid @RequestBody(required = false) WorkReviewRequest request) {
        String comment = request == null ? null : request.getComment();
        return ApiResponse.success("作品已驳回",
                workReviewService.reject(id, comment, SecurityUtils.currentUserId()));
    }

    /**
     * 下架已发布作品（处理违规内容）。
     */
    @PostMapping("/{id}/offline")
    public ApiResponse<WorkDetailVO> offline(@PathVariable Long id,
                                             @Valid @RequestBody(required = false) WorkReviewRequest request) {
        String comment = request == null ? null : request.getComment();
        return ApiResponse.success("作品已下架",
                workReviewService.offline(id, comment, SecurityUtils.currentUserId()));
    }
}
