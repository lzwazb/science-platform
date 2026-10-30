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
 * 后台作品日常管理（管理员及以上）。
 *
 * <p>与 {@code /review/works} 的分工：</p>
 * <ul>
 *   <li>审核工作台负责「待审核 → 通过 / 驳回」的流程</li>
 *   <li>这里是日常管理：跨状态查看全部作品、查看详情、下架违规内容</li>
 * </ul>
 * <p>两者共用同一套 Service，不重复实现状态流转逻辑。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin/works")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminWorkController {

    private final WorkQueryService workQueryService;
    private final WorkReviewService workReviewService;

    public AdminWorkController(WorkQueryService workQueryService,
                               WorkReviewService workReviewService) {
        this.workQueryService = workQueryService;
        this.workReviewService = workReviewService;
    }

    /**
     * 全部作品分页，含草稿与已下架。
     *
     * @param status 状态筛选，为空返回全部
     */
    @GetMapping
    public ApiResponse<PageResult<WorkListItemVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        return ApiResponse.success(PageResult.of(
                workQueryService.pageForReview(page, size, status, keyword)));
    }

    /**
     * 作品详情，任意状态可查。
     */
    @GetMapping("/{id}")
    public ApiResponse<WorkDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(workQueryService.getDetailAnyStatus(id));
    }

    /**
     * 下架违规作品。会通知作者并记入操作日志。
     */
    @PostMapping("/{id}/offline")
    public ApiResponse<WorkDetailVO> offline(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) WorkReviewRequest request) {

        String comment = request == null ? null : request.getComment();
        return ApiResponse.success("作品已下架",
                workReviewService.offline(id, comment, SecurityUtils.currentUserId()));
    }
}
