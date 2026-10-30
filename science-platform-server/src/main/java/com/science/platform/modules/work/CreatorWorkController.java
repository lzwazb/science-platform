package com.science.platform.modules.work;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.knowledge.dto.TopicArticlesRequest;
import com.science.platform.modules.work.dto.WorkSaveRequest;
import com.science.platform.modules.work.service.WorkQueryService;
import com.science.platform.modules.work.service.WorkService;
import com.science.platform.modules.work.vo.WorkDetailVO;
import com.science.platform.modules.work.vo.WorkListItemVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 创作者中心接口。
 *
 * <p>所有操作都以「当前登录用户」为创作者，作品归属校验在 Service 层完成，
 * 因此用户 A 无法通过改 URL 里的 id 去操作用户 B 的作品。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/creator/works")
@PreAuthorize("hasAnyRole('CREATOR', 'ADMIN', 'SUPER_ADMIN')")
public class CreatorWorkController {

    private final WorkService workService;
    private final WorkQueryService workQueryService;

    public CreatorWorkController(WorkService workService, WorkQueryService workQueryService) {
        this.workService = workService;
        this.workQueryService = workQueryService;
    }

    /**
     * 我的作品分页，含草稿与驳回。
     *
     * @param status 状态筛选，为空返回全部
     */
    @GetMapping
    public ApiResponse<PageResult<WorkListItemVO>> mine(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size,
            @RequestParam(required = false) String status) {

        return ApiResponse.success(PageResult.of(
                workQueryService.pageMine(SecurityUtils.currentUserId(), page, size, status)));
    }

    /**
     * 我的作品详情，允许草稿状态。
     */
    @GetMapping("/{id}")
    public ApiResponse<WorkDetailVO> detail(@PathVariable Long id) {
        WorkDetailVO detail = workQueryService.getDetailAnyStatus(id);
        // 归属校验：非本人作品一律拒绝，不区分「不存在」与「不是你的」，避免探测他人作品 ID
        if (!detail.getCreatorId().equals(SecurityUtils.currentUserId())) {
            throw com.science.platform.common.exception.BusinessException.forbidden("只能查看自己的作品");
        }
        return ApiResponse.success(detail);
    }

    /**
     * 创建作品，初始为草稿。
     */
    @PostMapping
    public ApiResponse<WorkDetailVO> create(@Valid @RequestBody WorkSaveRequest request) {
        return ApiResponse.success("作品已保存为草稿",
                workService.create(request, SecurityUtils.currentUserId()));
    }

    /**
     * 编辑作品。已发布的作品编辑后会回到待审核状态。
     */
    @PutMapping("/{id}")
    public ApiResponse<WorkDetailVO> update(@PathVariable Long id,
                                            @Valid @RequestBody WorkSaveRequest request) {
        return ApiResponse.success("作品已更新", workService.update(id, request, SecurityUtils.currentUserId()));
    }

    /**
     * 提交审核：草稿或已驳回的作品变为待审核。
     */
    @PostMapping("/{id}/submit")
    public ApiResponse<WorkDetailVO> submit(@PathVariable Long id) {
        return ApiResponse.success("已提交审核", workService.submit(id, SecurityUtils.currentUserId()));
    }

    /**
     * 删除作品（逻辑删除）。已被活动投稿引用的作品不允许删除。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        workService.delete(id, SecurityUtils.currentUserId());
        return ApiResponse.success("作品已删除", null);
    }

    /**
     * 更新作品关联的科普文章。
     */
    @PutMapping("/{id}/articles")
    public ApiResponse<WorkDetailVO> updateArticles(@PathVariable Long id,
                                                    @Valid @RequestBody TopicArticlesRequest request) {
        return ApiResponse.success("关联文章已更新",
                workService.updateArticles(id, request.getArticleIds(), SecurityUtils.currentUserId()));
    }
}
