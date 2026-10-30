package com.science.platform.modules.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.knowledge.dto.ArticleSaveRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台文章维护。
 *
 * <p>权限由 Controller 层的 {@code @PreAuthorize} 与 SecurityConfig 的路径规则共同保证，
 * 这里只处理业务规则。</p>
 */
@Service
public class ArticleAdminService {

    static final String STATUS_DRAFT = "DRAFT";
    static final String STATUS_PUBLISHED = "PUBLISHED";
    static final String STATUS_OFFLINE = "OFFLINE";

    private final ScienceArticleMapper articleMapper;
    private final ScienceCategoryMapper categoryMapper;
    private final ScienceTopicMapper topicMapper;
    private final TopicArticleMapper topicArticleMapper;
    private final SysUserMapper sysUserMapper;
    private final ArticleQueryService articleQueryService;

    public ArticleAdminService(ScienceArticleMapper articleMapper,
                               ScienceCategoryMapper categoryMapper,
                               ScienceTopicMapper topicMapper,
                               TopicArticleMapper topicArticleMapper,
                               SysUserMapper sysUserMapper,
                               ArticleQueryService articleQueryService) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.topicMapper = topicMapper;
        this.topicArticleMapper = topicArticleMapper;
        this.sysUserMapper = sysUserMapper;
        this.articleQueryService = articleQueryService;
    }

    /**
     * 后台文章分页查询，能看到草稿与已下架。
     *
     * @param status 状态筛选，可为空表示全部
     */
    public IPage<ArticleListItemVO> pageAll(long page, long size, String status, String keyword, Long categoryId) {
        LambdaQueryWrapper<ScienceArticle> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(status)) {
            wrapper.eq(ScienceArticle::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.eq(ScienceArticle::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String escaped = ArticleQueryService.escapeLike(keyword.trim());
            wrapper.and(q -> q.like(ScienceArticle::getTitle, escaped)
                    .or()
                    .like(ScienceArticle::getSummary, escaped));
        }
        wrapper.orderByDesc(ScienceArticle::getId);

        Page<ScienceArticle> result = articleMapper.selectPage(new Page<>(page, size), wrapper);

        Page<ArticleListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(articleQueryService.toListItems(result.getRecords()));
        return voPage;
    }

    /**
     * 后台查看文章详情，允许草稿与已下架状态；不增加阅读量。
     */
    public ArticleDetailVO getDetailForAdmin(Long id) {
        ScienceArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }

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

        if (article.getCategoryId() != null) {
            ScienceCategory category = categoryMapper.selectById(article.getCategoryId());
            vo.setCategoryName(category == null ? null : category.getName());
        }
        SysUser author = sysUserMapper.selectById(article.getAuthorId());
        vo.setAuthorName(author == null ? null : author.getNickname());

        vo.setTopics(findTopicsOfArticle(article.getId()));
        return vo;
    }

    /**
     * 创建文章，默认保存为草稿。
     *
     * @param request  文章内容
     * @param authorId 当前登录的管理员 ID，由 Controller 从认证上下文取得
     */
    @Transactional
    public ArticleDetailVO create(ArticleSaveRequest request, Long authorId) {
        validateCategory(request.getCategoryId());

        ScienceArticle article = new ScienceArticle();
        article.setTitle(request.getTitle().trim());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCoverUrl(request.getCoverUrl());
        article.setCategoryId(request.getCategoryId());
        article.setAuthorId(authorId);
        article.setStatus(STATUS_DRAFT);
        article.setViewCount(0);
        article.setVersionNo(1);

        articleMapper.insert(article);
        return getDetailForAdmin(article.getId());
    }

    /**
     * 编辑文章。
     *
     * <p>已发布文章被编辑后保持 PUBLISHED 状态（文章由管理员直接维护，
     * 与「作品需重新审核」的规则不同）；正文变更不改动 {@code version_no}——
     * 版本号只在「知识贡献被采纳」的审核流程中递增，保证版本历史与审核记录一一对应。</p>
     */
    @Transactional
    public ArticleDetailVO update(Long id, ArticleSaveRequest request) {
        ScienceArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }
        validateCategory(request.getCategoryId());

        article.setTitle(request.getTitle().trim());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCoverUrl(request.getCoverUrl());
        article.setCategoryId(request.getCategoryId());

        articleMapper.updateById(article);
        return getDetailForAdmin(id);
    }

    /**
     * 发布文章：草稿或已下架文章变为 PUBLISHED，首次发布时记录发布时间。
     */
    @Transactional
    public ArticleDetailVO publish(Long id) {
        ScienceArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }
        if (STATUS_PUBLISHED.equals(article.getStatus())) {
            throw new BusinessException("文章已是发布状态");
        }
        if (!StringUtils.hasText(article.getContent())) {
            throw new BusinessException("正文为空，无法发布");
        }

        article.setStatus(STATUS_PUBLISHED);
        if (article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        articleMapper.updateById(article);
        return getDetailForAdmin(id);
    }

    /**
     * 下架文章：前台立即不可见，数据保留。
     */
    @Transactional
    public ArticleDetailVO offline(Long id) {
        ScienceArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }
        if (STATUS_OFFLINE.equals(article.getStatus())) {
            throw new BusinessException("文章已是下架状态");
        }

        article.setStatus(STATUS_OFFLINE);
        articleMapper.updateById(article);
        return getDetailForAdmin(id);
    }

    /**
     * 删除文章（逻辑删除）。
     *
     * <p>优先软删除：文章可能已被知识贡献、评论、收藏引用，
     * 物理删除会让这些历史数据变成悬空引用。</p>
     */
    @Transactional
    public void delete(Long id) {
        ScienceArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }
        articleMapper.deleteById(id);
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        ScienceCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        if (!"ARTICLE".equals(category.getType())) {
            throw new BusinessException("所选分类不是文章分类");
        }
    }

    private List<TopicBriefVO> findTopicsOfArticle(Long articleId) {
        LambdaQueryWrapper<TopicArticle> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(TopicArticle::getArticleId, articleId);
        List<TopicArticle> relations = topicArticleMapper.selectList(relWrapper);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> topicIds = relations.stream().map(TopicArticle::getTopicId).collect(Collectors.toSet());
        LambdaQueryWrapper<ScienceTopic> topicWrapper = new LambdaQueryWrapper<>();
        topicWrapper.in(ScienceTopic::getId, topicIds).orderByAsc(ScienceTopic::getSortOrder);

        return topicMapper.selectList(topicWrapper).stream()
                .filter(Objects::nonNull)
                .map(topic -> {
                    TopicBriefVO brief = new TopicBriefVO();
                    brief.setId(topic.getId());
                    brief.setTitle(topic.getTitle());
                    return brief;
                }).toList();
    }
}
