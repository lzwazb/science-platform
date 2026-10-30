package com.science.platform.modules.knowledge;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.knowledge.service.ArticleQueryService;
import com.science.platform.modules.knowledge.vo.ArticleDetailVO;
import com.science.platform.modules.knowledge.vo.ArticleListItemVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 科普文章公开接口，游客可访问。
 */
@Validated
@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {

    private final ArticleQueryService articleQueryService;

    public ArticleController(ArticleQueryService articleQueryService) {
        this.articleQueryService = articleQueryService;
    }

    /**
     * 分页查询已发布文章，支持按分类筛选与关键词搜索。
     *
     * @param page       页码，从 1 开始
     * @param size       每页条数，上限 100
     * @param categoryId 分类 ID，可选
     * @param keyword    关键词，匹配标题或摘要，可选
     */
    @GetMapping
    public ApiResponse<PageResult<ArticleListItemVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {

        return ApiResponse.success(PageResult.of(
                articleQueryService.pagePublished(page, size, categoryId, keyword)));
    }

    /**
     * 关键词搜索，等价于带 keyword 的列表查询，单独提供以便前端语义清晰。
     */
    @GetMapping("/search")
    public ApiResponse<PageResult<ArticleListItemVO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size) {

        return ApiResponse.success(PageResult.of(
                articleQueryService.pagePublished(page, size, null, keyword)));
    }

    /**
     * 查看文章详情，阅读量 +1。
     */
    @GetMapping("/{id}")
    public ApiResponse<ArticleDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(articleQueryService.getPublishedDetail(id));
    }
}
