package com.science.platform.modules.work;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.work.service.WorkQueryService;
import com.science.platform.modules.work.vo.WorkDetailVO;
import com.science.platform.modules.work.vo.WorkListItemVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 创意作品公开接口，游客可访问。
 *
 * <p>只返回 {@code status = PUBLISHED} 的作品；草稿、待审核、已驳回、已下架的作品
 * 在这里一律不可见，也不会通过详情接口泄漏。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/works")
public class WorkController {

    private final WorkQueryService workQueryService;

    public WorkController(WorkQueryService workQueryService) {
        this.workQueryService = workQueryService;
    }

    /**
     * 分页查询已发布作品。
     */
    @GetMapping
    public ApiResponse<PageResult<WorkListItemVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "12") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {

        return ApiResponse.success(PageResult.of(
                workQueryService.pagePublished(page, size, categoryId, keyword)));
    }

    /**
     * 作品详情，阅读量 +1。
     */
    @GetMapping("/{id}")
    public ApiResponse<WorkDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(workQueryService.getPublishedDetail(id));
    }
}
