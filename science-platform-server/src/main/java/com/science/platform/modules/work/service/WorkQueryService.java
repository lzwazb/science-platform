package com.science.platform.modules.work.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.entity.ScienceCategory;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import com.science.platform.modules.knowledge.mapper.ScienceCategoryMapper;
import com.science.platform.modules.user.entity.SysUser;
import com.science.platform.modules.user.mapper.SysUserMapper;
import com.science.platform.modules.work.entity.CreatorWork;
import com.science.platform.modules.work.entity.WorkArticleRelation;
import com.science.platform.modules.work.mapper.CreatorWorkMapper;
import com.science.platform.modules.work.mapper.WorkArticleRelationMapper;
import com.science.platform.modules.work.vo.ArticleBriefVO;
import com.science.platform.modules.work.vo.WorkBriefVO;
import com.science.platform.modules.work.vo.WorkDetailVO;
import com.science.platform.modules.work.vo.WorkListItemVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 作品只读查询：公开列表 / 详情、我的作品、审核列表，以及文章详情页需要的关联作品。
 *
 * <p>公开口径：只返回 {@code status = PUBLISHED} 且未删除的作品。
 * 草稿、待审核、已驳回、已下架的作品对游客与普通用户都不可见。</p>
 */
@Service
public class WorkQueryService {

    /** 公开可见的作品状态 */
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_PENDING = "PENDING";

    private final CreatorWorkMapper workMapper;
    private final WorkArticleRelationMapper relationMapper;
    private final ScienceArticleMapper articleMapper;
    private final ScienceCategoryMapper categoryMapper;
    private final SysUserMapper sysUserMapper;

    public WorkQueryService(CreatorWorkMapper workMapper,
                            WorkArticleRelationMapper relationMapper,
                            ScienceArticleMapper articleMapper,
                            ScienceCategoryMapper categoryMapper,
                            SysUserMapper sysUserMapper) {
        this.workMapper = workMapper;
        this.relationMapper = relationMapper;
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 公开作品分页。
     *
     * @param page       页码
     * @param size       每页条数
     * @param categoryId 分类筛选
     * @param keyword    关键词，匹配标题或简介
     */
    public IPage<WorkListItemVO> pagePublished(long page, long size, Long categoryId, String keyword) {
        LambdaQueryWrapper<CreatorWork> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreatorWork::getStatus, STATUS_PUBLISHED);

        if (categoryId != null) {
            wrapper.eq(CreatorWork::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String escaped = escapeLike(keyword.trim());
            wrapper.and(q -> q.like(CreatorWork::getTitle, escaped)
                    .or()
                    .like(CreatorWork::getDescription, escaped));
        }
        wrapper.orderByDesc(CreatorWork::getReviewedAt).orderByDesc(CreatorWork::getId);

        Page<CreatorWork> result = workMapper.selectPage(new Page<>(page, size), wrapper);
        Page<WorkListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toListItems(result.getRecords()));
        return voPage;
    }

    /**
     * 公开作品详情，阅读量 +1。
     */
    public WorkDetailVO getPublishedDetail(Long id) {
        CreatorWork work = workMapper.selectById(id);
        if (work == null || !STATUS_PUBLISHED.equals(work.getStatus())) {
            throw BusinessException.notFound("作品不存在或未发布");
        }

        CreatorWork viewUpdate = new CreatorWork();
        viewUpdate.setId(work.getId());
        viewUpdate.setViewCount(work.getViewCount() == null ? 1 : work.getViewCount() + 1);
        workMapper.updateById(viewUpdate);
        work.setViewCount(viewUpdate.getViewCount());

        return toDetail(work);
    }

    /**
     * 我的作品分页。
     *
     * @param creatorId 创作者 ID，必须来自认证上下文
     * @param status    状态筛选，可为空
     */
    public IPage<WorkListItemVO> pageMine(Long creatorId, long page, long size, String status) {
        LambdaQueryWrapper<CreatorWork> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreatorWork::getCreatorId, creatorId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(CreatorWork::getStatus, status);
        }
        wrapper.orderByDesc(CreatorWork::getId);

        Page<CreatorWork> result = workMapper.selectPage(new Page<>(page, size), wrapper);
        Page<WorkListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toListItems(result.getRecords()));
        return voPage;
    }

    /**
     * 后台 / 审核工作台的作品分页，可跨创作者查询。
     */
    public IPage<WorkListItemVO> pageForReview(long page, long size, String status, String keyword) {
        LambdaQueryWrapper<CreatorWork> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(CreatorWork::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            String escaped = escapeLike(keyword.trim());
            wrapper.and(q -> q.like(CreatorWork::getTitle, escaped)
                    .or()
                    .like(CreatorWork::getDescription, escaped));
        }
        wrapper.orderByAsc(CreatorWork::getCreatedAt).orderByAsc(CreatorWork::getId);

        Page<CreatorWork> result = workMapper.selectPage(new Page<>(page, size), wrapper);
        Page<WorkListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toListItems(result.getRecords()));
        return voPage;
    }

    /**
     * 查询作品详情，不限状态。调用方需自行保证权限（作者本人或评审/管理员）。
     */
    public WorkDetailVO getDetailAnyStatus(Long id) {
        CreatorWork work = workMapper.selectById(id);
        if (work == null) {
            throw BusinessException.notFound("作品不存在");
        }
        return toDetail(work);
    }

    /**
     * 查询某篇文章关联的、已发布的作品（用于文章详情页「相关创意作品」）。
     */
    public List<WorkBriefVO> findPublishedWorksOfArticle(Long articleId) {
        LambdaQueryWrapper<WorkArticleRelation> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(WorkArticleRelation::getArticleId, articleId);
        List<WorkArticleRelation> relations = relationMapper.selectList(relWrapper);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> workIds = relations.stream().map(WorkArticleRelation::getWorkId).distinct().toList();
        LambdaQueryWrapper<CreatorWork> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CreatorWork::getId, workIds)
                .eq(CreatorWork::getStatus, STATUS_PUBLISHED)
                .orderByDesc(CreatorWork::getId);

        List<CreatorWork> works = workMapper.selectList(wrapper);
        if (works.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> creatorNames = creatorNameMap(works);
        return works.stream().map(work -> {
            WorkBriefVO vo = new WorkBriefVO();
            vo.setId(work.getId());
            vo.setTitle(work.getTitle());
            vo.setDescription(work.getDescription());
            vo.setCoverUrl(work.getCoverUrl());
            vo.setCreatorName(creatorNames.get(work.getCreatorId()));
            return vo;
        }).toList();
    }

    /**
     * 查询作品关联的文章。
     */
    public List<ArticleBriefVO> findArticlesOfWork(Long workId) {
        LambdaQueryWrapper<WorkArticleRelation> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(WorkArticleRelation::getWorkId, workId).orderByAsc(WorkArticleRelation::getId);
        List<WorkArticleRelation> relations = relationMapper.selectList(relWrapper);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> articleIds = relations.stream().map(WorkArticleRelation::getArticleId).toList();
        List<ScienceArticle> articles = articleMapper.selectBatchIds(articleIds);
        if (articles.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ScienceArticle> byId = articles.stream()
                .collect(Collectors.toMap(ScienceArticle::getId, a -> a, (a, b) -> a));
        Map<Long, String> categoryNames = categoryNameMap(articles);

        return articleIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(article -> {
                    ArticleBriefVO vo = new ArticleBriefVO();
                    vo.setId(article.getId());
                    vo.setTitle(article.getTitle());
                    vo.setSummary(article.getSummary());
                    vo.setCoverUrl(article.getCoverUrl());
                    vo.setCategoryName(categoryNames.get(article.getCategoryId()));
                    return vo;
                }).toList();
    }

    /**
     * 批量转列表项：分类名、作者名、关联文章一次补齐。
     */
    public List<WorkListItemVO> toListItems(List<CreatorWork> works) {
        if (works == null || works.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> categoryNames = categoryNameMapOfWorks(works);
        Map<Long, String> creatorNames = creatorNameMap(works);

        // 关联文章：一次取全部作品的关联，再按作品分组
        List<Long> workIds = works.stream().map(CreatorWork::getId).toList();
        LambdaQueryWrapper<WorkArticleRelation> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.in(WorkArticleRelation::getWorkId, workIds);
        List<WorkArticleRelation> relations = relationMapper.selectList(relWrapper);

        Map<Long, List<Long>> articleIdsByWork = relations.stream()
                .collect(Collectors.groupingBy(
                        WorkArticleRelation::getWorkId,
                        Collectors.mapping(WorkArticleRelation::getArticleId, Collectors.toList())));

        Map<Long, String> articleTitles = Collections.emptyMap();
        Set<Long> allArticleIds = relations.stream().map(WorkArticleRelation::getArticleId).collect(Collectors.toSet());
        if (!allArticleIds.isEmpty()) {
            articleTitles = articleMapper.selectBatchIds(allArticleIds).stream()
                    .collect(Collectors.toMap(ScienceArticle::getId, ScienceArticle::getTitle));
        }
        Map<Long, String> finalArticleTitles = articleTitles;

        return works.stream().map(work -> {
            WorkListItemVO vo = new WorkListItemVO();
            vo.setId(work.getId());
            vo.setTitle(work.getTitle());
            vo.setDescription(work.getDescription());
            vo.setCoverUrl(work.getCoverUrl());
            vo.setCategoryId(work.getCategoryId());
            vo.setCategoryName(categoryNames.get(work.getCategoryId()));
            vo.setCreatorId(work.getCreatorId());
            vo.setCreatorName(creatorNames.get(work.getCreatorId()));
            vo.setStatus(work.getStatus());
            vo.setViewCount(work.getViewCount());
            vo.setReviewComment(work.getReviewComment());
            vo.setReviewedAt(work.getReviewedAt());
            vo.setCreatedAt(work.getCreatedAt());
            vo.setUpdatedAt(work.getUpdatedAt());

            List<Long> ids = articleIdsByWork.getOrDefault(work.getId(), Collections.emptyList());
            vo.setArticleCount(ids.size());
            vo.setArticleTitles(ids.stream()
                    .map(finalArticleTitles::get)
                    .filter(Objects::nonNull)
                    .toList());
            return vo;
        }).toList();
    }

    private WorkDetailVO toDetail(CreatorWork work) {
        WorkDetailVO vo = new WorkDetailVO();
        vo.setId(work.getId());
        vo.setTitle(work.getTitle());
        vo.setDescription(work.getDescription());
        vo.setContent(work.getContent());
        vo.setCoverUrl(work.getCoverUrl());
        vo.setCategoryId(work.getCategoryId());
        vo.setCreatorId(work.getCreatorId());
        vo.setStatus(work.getStatus());
        vo.setReviewComment(work.getReviewComment());
        vo.setReviewedAt(work.getReviewedAt());
        vo.setViewCount(work.getViewCount());
        vo.setCreatedAt(work.getCreatedAt());
        vo.setUpdatedAt(work.getUpdatedAt());

        if (work.getCategoryId() != null) {
            ScienceCategory category = categoryMapper.selectById(work.getCategoryId());
            vo.setCategoryName(category == null ? null : category.getName());
        }
        SysUser creator = sysUserMapper.selectById(work.getCreatorId());
        if (creator != null) {
            vo.setCreatorName(creator.getNickname());
            vo.setCreatorBio(creator.getBio());
        }
        vo.setArticles(findArticlesOfWork(work.getId()));
        return vo;
    }

    private Map<Long, String> categoryNameMapOfWorks(List<CreatorWork> works) {
        Set<Long> ids = works.stream()
                .map(CreatorWork::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ScienceCategory::getId, ScienceCategory::getName));
    }

    private Map<Long, String> categoryNameMap(List<ScienceArticle> articles) {
        Set<Long> ids = articles.stream()
                .map(ScienceArticle::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ScienceCategory::getId, ScienceCategory::getName));
    }

    private Map<Long, String> creatorNameMap(List<CreatorWork> works) {
        Set<Long> ids = works.stream()
                .map(CreatorWork::getCreatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));
    }

    /**
     * 转义 LIKE 通配符，与文章搜索保持一致的行为。
     */
    static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
