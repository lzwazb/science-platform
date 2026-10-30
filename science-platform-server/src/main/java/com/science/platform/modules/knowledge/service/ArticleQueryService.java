package com.science.platform.modules.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.entity.ScienceCategory;
import com.science.platform.modules.knowledge.entity.ScienceTopic;
import com.science.platform.modules.knowledge.entity.TopicArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import com.science.platform.modules.knowledge.mapper.ScienceCategoryMapper;
import com.science.platform.modules.knowledge.mapper.ScienceTopicMapper;
import com.science.platform.modules.knowledge.mapper.TopicArticleMapper;
import com.science.platform.modules.knowledge.vo.ArticleDetailVO;
import com.science.platform.modules.knowledge.vo.ArticleListItemVO;
import com.science.platform.modules.knowledge.vo.TopicBriefVO;
import com.science.platform.modules.user.entity.SysUser;
import com.science.platform.modules.user.mapper.SysUserMapper;
import com.science.platform.modules.work.service.WorkQueryService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 科普文章公开查询。
 *
 * <p>公开口径：只返回 {@code status = PUBLISHED} 且未逻辑删除的文章。
 * 草稿与已下架文章对游客和普通用户都不可见，这一点由后端保证，而不是靠前端过滤。</p>
 */
@Service
public class ArticleQueryService {

    /** 公开可见的文章状态 */
    static final String STATUS_PUBLISHED = "PUBLISHED";

    private final ScienceArticleMapper articleMapper;
    private final ScienceCategoryMapper categoryMapper;
    private final ScienceTopicMapper topicMapper;
    private final TopicArticleMapper topicArticleMapper;
    private final SysUserMapper sysUserMapper;
    private final WorkQueryService workQueryService;

    public ArticleQueryService(ScienceArticleMapper articleMapper,
                               ScienceCategoryMapper categoryMapper,
                               ScienceTopicMapper topicMapper,
                               TopicArticleMapper topicArticleMapper,
                               SysUserMapper sysUserMapper,
                               WorkQueryService workQueryService) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.topicMapper = topicMapper;
        this.topicArticleMapper = topicArticleMapper;
        this.sysUserMapper = sysUserMapper;
        this.workQueryService = workQueryService;
    }

    /**
     * 分页查询已发布文章，支持分类筛选与关键词搜索。
     *
     * @param page       页码，从 1 开始
     * @param size       每页条数
     * @param categoryId 分类 ID，可为空
     * @param keyword    关键词，匹配标题或摘要，可为空
     */
    public IPage<ArticleListItemVO> pagePublished(long page, long size, Long categoryId, String keyword) {
        LambdaQueryWrapper<ScienceArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScienceArticle::getStatus, STATUS_PUBLISHED);

        if (categoryId != null) {
            wrapper.eq(ScienceArticle::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String escaped = escapeLike(keyword.trim());
            wrapper.and(q -> q.like(ScienceArticle::getTitle, escaped)
                    .or()
                    .like(ScienceArticle::getSummary, escaped));
        }

        // 排序：发布时间倒序，同一时间按 id 倒序保证分页稳定
        wrapper.orderByDesc(ScienceArticle::getPublishedAt).orderByDesc(ScienceArticle::getId);

        Page<ScienceArticle> result = articleMapper.selectPage(new Page<>(page, size), wrapper);

        Page<ArticleListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toListItems(result.getRecords()));
        return voPage;
    }

    /**
     * 查询已发布文章详情。
     *
     * <p>读取时把阅读量 +1，属于「浏览计数」这一可选功能的最小实现。</p>
     *
     * @param id 文章 ID
     * @throws BusinessException 文章不存在或不是已发布状态时抛出 404
     */
    public ArticleDetailVO getPublishedDetail(Long id) {
        ScienceArticle article = articleMapper.selectById(id);
        if (article == null || !STATUS_PUBLISHED.equals(article.getStatus())) {
            throw BusinessException.notFound("文章不存在或未发布");
        }

        // 增加阅读量：交给数据库做原子自增。
        // 不用「读出来 +1 再写回」——并发下会丢计数，
        // 而且 updateById 会连带刷新 updated_at，把「最后更新时间」变成「最后被点开的时间」。
        articleMapper.increaseViewCount(article.getId());
        article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);

        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setCoverUrl(article.getCoverUrl());
        vo.setCategoryId(article.getCategoryId());
        vo.setAuthorId(article.getAuthorId());
        vo.setStatus(article.getStatus());
        vo.setPublishedAt(article.getPublishedAt());
        vo.setViewCount(article.getViewCount());
        vo.setVersionNo(article.getVersionNo());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());

        // 分类名
        if (article.getCategoryId() != null) {
            ScienceCategory category = categoryMapper.selectById(article.getCategoryId());
            vo.setCategoryName(category == null ? null : category.getName());
        }
        // 作者昵称
        SysUser author = sysUserMapper.selectById(article.getAuthorId());
        vo.setAuthorName(author == null ? null : author.getNickname());
        // 所属专题
        vo.setTopics(findTopicsOfArticle(article.getId()));
        // 关联的已发布创意作品
        vo.setWorks(workQueryService.findPublishedWorksOfArticle(article.getId()));

        return vo;
    }

    /**
     * 查询文章所属的已发布专题。
     */
    private List<TopicBriefVO> findTopicsOfArticle(Long articleId) {
        LambdaQueryWrapper<TopicArticle> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(TopicArticle::getArticleId, articleId);
        List<TopicArticle> relations = topicArticleMapper.selectList(relWrapper);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> topicIds = relations.stream().map(TopicArticle::getTopicId).collect(Collectors.toSet());
        LambdaQueryWrapper<ScienceTopic> topicWrapper = new LambdaQueryWrapper<>();
        topicWrapper.in(ScienceTopic::getId, topicIds)
                .eq(ScienceTopic::getStatus, "PUBLISHED")
                .orderByAsc(ScienceTopic::getSortOrder);

        return topicMapper.selectList(topicWrapper).stream().map(topic -> {
            TopicBriefVO brief = new TopicBriefVO();
            brief.setId(topic.getId());
            brief.setTitle(topic.getTitle());
            return brief;
        }).toList();
    }

    /**
     * 把文章实体批量转成列表项，关联的分类名与作者昵称用批量查询补齐，避免逐条查询。
     */
    List<ArticleListItemVO> toListItems(List<ScienceArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> categoryIds = articles.stream()
                .map(ScienceArticle::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> authorIds = articles.stream()
                .map(ScienceArticle::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> categoryNames = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(ScienceCategory::getId, ScienceCategory::getName));

        Map<Long, String> authorNames = authorIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(authorIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));

        return articles.stream().map(article -> {
            ArticleListItemVO vo = new ArticleListItemVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setCoverUrl(article.getCoverUrl());
            vo.setCategoryId(article.getCategoryId());
            vo.setCategoryName(categoryNames.get(article.getCategoryId()));
            vo.setAuthorId(article.getAuthorId());
            vo.setAuthorName(authorNames.get(article.getAuthorId()));
            vo.setStatus(article.getStatus());
            vo.setPublishedAt(article.getPublishedAt());
            vo.setViewCount(article.getViewCount());
            vo.setVersionNo(article.getVersionNo());
            return vo;
        }).toList();
    }

    /**
     * 转义 LIKE 通配符，避免用户输入的 {@code %} 或 {@code _} 被当作通配符匹配全部。
     *
     * <p>这不是防注入（查询已参数化），而是让搜索结果符合用户预期。</p>
     */
    static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
