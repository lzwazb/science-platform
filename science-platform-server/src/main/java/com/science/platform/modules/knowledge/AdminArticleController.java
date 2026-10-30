package com.science.platform.modules.knowledge;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.knowledge.dto.ArticleSaveRequest;
import com.science.platform.modules.knowledge.service.ArticleAdminService;
import com.science.platform.modules.knowledge.vo.ArticleDetailVO;
import com.science.platform.modules.knowledge.vo.ArticleListItemVO;
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
 * 后台文章维护接口。
 *
 * <p>路径规则已在 SecurityConfig 中限定为 {@code ADMIN} 及以上，
 * 这里再加方法级注解形成双重保险，也便于单独阅读某个接口的权限要求。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin/articles")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminArticleController {

    private final ArticleAdminService articleAdminService;

    public AdminArticleController(ArticleAdminService articleAdminService) {
        this.articleAdminService = articleAdminService;
    }

    /**
     * 后台文章分页查询，包含草稿与已下架。
     *
     * @param status 状态筛选：DRAFT / PUBLISHED / OFFLINE，为空表示全部
     */
    @GetMapping
    public ApiResponse<PageResult<ArticleListItemVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {

        return ApiResponse.success(PageResult.of(
                articleAdminService.pageAll(page, size, status, keyword, categoryId)));
    }

    /**
     * 后台查看文章详情，允许草稿状态，不增加阅读量。
     */
    @GetMapping("/{id}")
    public ApiResponse<ArticleDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(articleAdminService.getDetailForAdmin(id));
    }

    /**
     * 创建文章，初始状态为草稿。
     */
    @PostMapping
    public ApiResponse<ArticleDetailVO> create(@Valid @RequestBody ArticleSaveRequest request) {
        return ApiResponse.success("文章已保存为草稿",
                articleAdminService.create(request, SecurityUtils.currentUserId()));
    }

    /**
     * 编辑文章。
     */
    @PutMapping("/{id}")
    public ApiResponse<ArticleDetailVO> update(@PathVariable Long id,
                                               @Valid @RequestBody ArticleSaveRequest request) {
        return ApiResponse.success("文章已更新", articleAdminService.update(id, request));
    }

    /**
     * 发布文章。
     */
    @PostMapping("/{id}/publish")
    public ApiResponse<ArticleDetailVO> publish(@PathVariable Long id) {
        return ApiResponse.success("文章已发布", articleAdminService.publish(id));
    }

    /**
     * 下架文章。
     */
    @PostMapping("/{id}/offline")
    public ApiResponse<ArticleDetailVO> offline(@PathVariable Long id) {
        return ApiResponse.success("文章已下架", articleAdminService.offline(id));
    }

    /**
     * 删除文章（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        articleAdminService.delete(id);
        return ApiResponse.success("文章已删除", null);
    }
}
