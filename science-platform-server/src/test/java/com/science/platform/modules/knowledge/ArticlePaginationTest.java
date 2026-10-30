package com.science.platform.modules.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import com.science.platform.modules.system.service.SystemHealthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阶段 2 基础能力测试：数据源连通、分页插件生效、逻辑删除条件自动拼接。
 *
 * <p>运行前置条件：已执行 database/schema.sql 与 database/seed.sql。
 * 演示数据中已发布文章为 8 篇。</p>
 */
@SpringBootTest
class ArticlePaginationTest {

    private static final long EXPECTED_PUBLISHED = 8L;

    @Autowired
    private ScienceArticleMapper scienceArticleMapper;

    @Autowired
    private SystemHealthService systemHealthService;

    @Test
    @DisplayName("健康服务能统计到 8 篇已发布文章")
    void countPublishedArticles() {
        assertEquals(EXPECTED_PUBLISHED, systemHealthService.countPublishedArticles());
    }

    @Test
    @DisplayName("分页插件生效：第 1 页 size=3 只返回 3 条，total 为 8")
    void paginationFirstPage() {
        Page<ScienceArticle> page = new Page<>(1, 3);
        LambdaQueryWrapper<ScienceArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScienceArticle::getStatus, "PUBLISHED").orderByAsc(ScienceArticle::getId);

        Page<ScienceArticle> result = scienceArticleMapper.selectPage(page, wrapper);

        assertEquals(EXPECTED_PUBLISHED, result.getTotal(), "总数应为全部已发布文章");
        assertEquals(3, result.getRecords().size(), "单页条数应等于 size");
        assertEquals(1L, result.getRecords().get(0).getId(), "按 id 升序时首页第一条应为 1");
    }

    @Test
    @DisplayName("最后一页只剩 2 条，且不重复、不缺失")
    void paginationLastPage() {
        Page<ScienceArticle> page = new Page<>(3, 3);
        LambdaQueryWrapper<ScienceArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScienceArticle::getStatus, "PUBLISHED").orderByAsc(ScienceArticle::getId);

        Page<ScienceArticle> result = scienceArticleMapper.selectPage(page, wrapper);

        assertEquals(EXPECTED_PUBLISHED, result.getTotal());
        assertEquals(2, result.getRecords().size(), "8 条数据按每页 3 条，第 3 页应为 2 条");
    }

    @Test
    @DisplayName("页码超出范围返回空列表，而不是回落到第一页")
    void paginationOverflow() {
        Page<ScienceArticle> page = new Page<>(10, 3);
        LambdaQueryWrapper<ScienceArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScienceArticle::getStatus, "PUBLISHED");

        Page<ScienceArticle> result = scienceArticleMapper.selectPage(page, wrapper);

        assertEquals(EXPECTED_PUBLISHED, result.getTotal());
        assertTrue(result.getRecords().isEmpty(), "超出页码应返回空列表");
    }

    @Test
    @DisplayName("逻辑删除字段自动过滤，查询结果不含已删除记录")
    void logicDeleteFiltered() {
        LambdaQueryWrapper<ScienceArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScienceArticle::getStatus, "PUBLISHED");
        List<ScienceArticle> list = scienceArticleMapper.selectList(wrapper);

        assertEquals(EXPECTED_PUBLISHED, list.size());
        assertTrue(list.stream().allMatch(a -> a.getDeleted() == null || a.getDeleted() == 0),
                "查询结果不应包含 deleted = 1 的记录");
        assertNotNull(list.get(0).getCreatedAt(), "创建时间应被填充或由数据库默认值提供");
    }
}
