package com.science.platform.modules.knowledge;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.knowledge.dto.CategorySaveRequest;
import com.science.platform.modules.knowledge.service.CategoryService;
import com.science.platform.modules.knowledge.vo.CategoryVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台分类维护接口。
 */
@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 分类列表，包含已停用分类。
     */
    @GetMapping
    public ApiResponse<List<CategoryVO>> list(@RequestParam(required = false) String type) {
        return ApiResponse.success(categoryService.listAll(type));
    }

    /**
     * 创建分类。
     */
    @PostMapping
    public ApiResponse<CategoryVO> create(@Valid @RequestBody CategorySaveRequest request) {
        return ApiResponse.success("分类已创建", categoryService.create(request));
    }

    /**
     * 编辑分类。
     */
    @PutMapping("/{id}")
    public ApiResponse<CategoryVO> update(@PathVariable Long id,
                                          @Valid @RequestBody CategorySaveRequest request) {
        return ApiResponse.success("分类已更新", categoryService.update(id, request));
    }

    /**
     * 删除分类。分类下仍有文章时会被拒绝。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success("分类已删除", null);
    }
}
