package com.science.platform.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import org.springframework.stereotype.Service;

/**
 * 系统级健康检查业务逻辑。
 *
 * <p>阶段 2 只做一件事：真实访问数据库，确认数据源、MyBatis-Plus 与示例数据可用。</p>
 */
@Service
public class SystemHealthService {

    private final ScienceArticleMapper scienceArticleMapper;

    public SystemHealthService(ScienceArticleMapper scienceArticleMapper) {
        this.scienceArticleMapper = scienceArticleMapper;
    }

    /**
     * 统计已发布文章数量，用于健康检查中证明数据库连通。
     *
     * @return 已发布且未删除的文章数
     */
    public long countPublishedArticles() {
        LambdaQueryWrapper<ScienceArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScienceArticle::getStatus, "PUBLISHED");
        return scienceArticleMapper.selectCount(wrapper);
    }
}
