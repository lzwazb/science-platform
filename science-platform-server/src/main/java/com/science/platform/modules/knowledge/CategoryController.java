package com.science.platform.modules.knowledge;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.knowledge.service.CategoryService;
import com.science.platform.modules.knowledge.vo.CategoryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类公开接口，只返回启用中的分类。
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 公开分类列表。
     *
     * @param type ARTICLE 或 WORK，为空返回全部
     */
    @GetMapping
    public ApiResponse<List<CategoryVO>> list(@RequestParam(required = false) String type) {
        return ApiResponse.success(categoryService.listEnabled(type));
    }
}
