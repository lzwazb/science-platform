package com.science.platform.modules.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.knowledge.dto.CategorySaveRequest;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.entity.ScienceCategory;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import com.science.platform.modules.knowledge.mapper.ScienceCategoryMapper;
import com.science.platform.modules.knowledge.vo.CategoryVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 分类业务逻辑。
 */
@Service
public class CategoryService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";

    private final ScienceCategoryMapper categoryMapper;
    private final ScienceArticleMapper articleMapper;

    public CategoryService(ScienceCategoryMapper categoryMapper, ScienceArticleMapper articleMapper) {
        this.categoryMapper = categoryMapper;
        this.articleMapper = articleMapper;
    }

    /**
     * 公开分类列表，只返回启用中的分类。
     *
     * @param type ARTICLE / WORK，为空则返回全部类型
     */
    public List<CategoryVO> listEnabled(String type) {
        LambdaQueryWrapper<ScienceCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScienceCategory::getStatus, STATUS_ENABLED);
        if (StringUtils.hasText(type)) {
            wrapper.eq(ScienceCategory::getType, type.toUpperCase());
        }
        wrapper.orderByAsc(ScienceCategory::getSortOrder).orderByAsc(ScienceCategory::getId);
        return categoryMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    /**
     * 后台分类列表，包含已停用的分类。
     */
    public List<CategoryVO> listAll(String type) {
        LambdaQueryWrapper<ScienceCategory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) {
            wrapper.eq(ScienceCategory::getType, type.toUpperCase());
        }
        wrapper.orderByAsc(ScienceCategory::getType)
                .orderByAsc(ScienceCategory::getSortOrder)
                .orderByAsc(ScienceCategory::getId);
        return categoryMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    /**
     * 创建分类。
     */
    @Transactional
    public CategoryVO create(CategorySaveRequest request) {
        ScienceCategory category = new ScienceCategory();
        category.setName(request.getName().trim());
        category.setType(request.getType());
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setStatus(request.getStatus() == null ? STATUS_ENABLED : request.getStatus());

        try {
            categoryMapper.insert(category);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("同一类型下已存在同名分类");
        }
        return toVO(category);
    }

    /**
     * 编辑分类。
     */
    @Transactional
    public CategoryVO update(Long id, CategorySaveRequest request) {
        ScienceCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在");
        }

        category.setName(request.getName().trim());
        category.setType(request.getType());
        category.setDescription(request.getDescription());
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        try {
            categoryMapper.updateById(category);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("同一类型下已存在同名分类");
        }
        return toVO(category);
    }

    /**
     * 删除分类。
     *
     * <p>分类被文章引用时不允许删除（正确做法是改为 DISABLED 停用），
     * 否则会出现「文章还在但分类消失」的悬空引用。</p>
     */
    @Transactional
    public void delete(Long id) {
        ScienceCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在");
        }

        LambdaQueryWrapper<ScienceArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScienceArticle::getCategoryId, id);
        long used = articleMapper.selectCount(wrapper);
        if (used > 0) {
            throw new BusinessException("该分类下还有 " + used + " 篇文章，请先移动文章或将分类改为停用");
        }

        categoryMapper.deleteById(id);
    }

    private CategoryVO toVO(ScienceCategory category) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setType(category.getType());
        vo.setDescription(category.getDescription());
        vo.setSortOrder(category.getSortOrder());
        vo.setStatus(category.getStatus());
        return vo;
    }
}
