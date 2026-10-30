package com.science.platform.modules.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.knowledge.dto.TopicSaveRequest;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.entity.ScienceTopic;
import com.science.platform.modules.knowledge.entity.TopicArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import com.science.platform.modules.knowledge.mapper.ScienceTopicMapper;
import com.science.platform.modules.knowledge.mapper.TopicArticleMapper;
import com.science.platform.modules.knowledge.vo.ArticleListItemVO;
import com.science.platform.modules.knowledge.vo.TopicVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 专题业务逻辑。
 */
@Service
public class TopicService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_DRAFT = "DRAFT";

    private final ScienceTopicMapper topicMapper;
    private final TopicArticleMapper topicArticleMapper;
    private final ScienceArticleMapper articleMapper;
    private final ArticleQueryService articleQueryService;

    public TopicService(ScienceTopicMapper topicMapper,
                        TopicArticleMapper topicArticleMapper,
                        ScienceArticleMapper articleMapper,
                        ArticleQueryService articleQueryService) {
        this.topicMapper = topicMapper;
        this.topicArticleMapper = topicArticleMapper;
        this.articleMapper = articleMapper;
        this.articleQueryService = articleQueryService;
    }

    /**
     * 公开专题列表，只返回已发布专题，附带专题内已发布文章数。
     */
    public List<TopicVO> listPublished() {
        LambdaQueryWrapper<ScienceTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScienceTopic::getStatus, STATUS_PUBLISHED)
                .orderByAsc(ScienceTopic::getSortOrder)
                .orderByAsc(ScienceTopic::getId);

        List<ScienceTopic> topics = topicMapper.selectList(wrapper);
        if (topics.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> counts = countArticlesByTopic(topics.stream().map(ScienceTopic::getId).toList());

        return topics.stream().map(topic -> {
            TopicVO vo = toVO(topic);
            vo.setArticleCount(counts.getOrDefault(topic.getId(), 0L));
            return vo;
        }).toList();
    }

    /**
     * 公开专题详情，包含专题内已发布文章。
     *
     * @param id 专题 ID
     * @throws BusinessException 专题不存在或未发布时抛出 404
     */
    public TopicVO getPublishedDetail(Long id) {
        ScienceTopic topic = topicMapper.selectById(id);
        if (topic == null || !STATUS_PUBLISHED.equals(topic.getStatus())) {
            throw BusinessException.notFound("专题不存在或未发布");
        }

        List<ScienceArticle> articles = findPublishedArticlesOfTopic(id);

        TopicVO vo = toVO(topic);
        List<ArticleListItemVO> items = articleQueryService.toListItems(articles);
        vo.setArticles(items);
        vo.setArticleCount((long) items.size());
        return vo;
    }

    /**
     * 后台专题列表，包含草稿与已下架。
     */
    public List<TopicVO> listAll() {
        LambdaQueryWrapper<ScienceTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ScienceTopic::getSortOrder).orderByAsc(ScienceTopic::getId);

        List<ScienceTopic> topics = topicMapper.selectList(wrapper);
        if (topics.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Long> counts = countArticlesByTopic(topics.stream().map(ScienceTopic::getId).toList());

        return topics.stream().map(topic -> {
            TopicVO vo = toVO(topic);
            vo.setArticleCount(counts.getOrDefault(topic.getId(), 0L));
            return vo;
        }).toList();
    }

    /**
     * 后台专题详情，包含草稿状态的文章（便于管理端调整专题内容）。
     */
    public TopicVO getDetailForAdmin(Long id) {
        ScienceTopic topic = topicMapper.selectById(id);
        if (topic == null) {
            throw BusinessException.notFound("专题不存在");
        }
        List<ScienceArticle> articles = findArticlesOfTopic(id, false);
        TopicVO vo = toVO(topic);
        List<ArticleListItemVO> items = articleQueryService.toListItems(articles);
        vo.setArticles(items);
        vo.setArticleCount((long) items.size());
        return vo;
    }

    @Transactional
    public TopicVO create(TopicSaveRequest request) {
        ScienceTopic topic = new ScienceTopic();
        topic.setTitle(request.getTitle().trim());
        topic.setDescription(request.getDescription());
        topic.setCoverUrl(request.getCoverUrl());
        topic.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        topic.setStatus(request.getStatus() == null ? STATUS_DRAFT : request.getStatus());
        topicMapper.insert(topic);

        TopicVO vo = toVO(topic);
        vo.setArticleCount(0L);
        return vo;
    }

    @Transactional
    public TopicVO update(Long id, TopicSaveRequest request) {
        ScienceTopic topic = topicMapper.selectById(id);
        if (topic == null) {
            throw BusinessException.notFound("专题不存在");
        }

        topic.setTitle(request.getTitle().trim());
        topic.setDescription(request.getDescription());
        topic.setCoverUrl(request.getCoverUrl());
        if (request.getSortOrder() != null) {
            topic.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            topic.setStatus(request.getStatus());
        }
        topicMapper.updateById(topic);

        TopicVO vo = toVO(topic);
        vo.setArticleCount(countArticlesByTopic(List.of(id)).getOrDefault(id, 0L));
        return vo;
    }

    @Transactional
    public void delete(Long id) {
        ScienceTopic topic = topicMapper.selectById(id);
        if (topic == null) {
            throw BusinessException.notFound("专题不存在");
        }
        // 先清关联再删专题；关联表无逻辑删除，物理删除不会影响文章本身
        LambdaQueryWrapper<TopicArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TopicArticle::getTopicId, id);
        topicArticleMapper.delete(wrapper);

        topicMapper.deleteById(id);
    }

    /**
     * 整体替换专题内文章列表。
     *
     * @param id         专题 ID
     * @param articleIds 更新后的文章 ID 列表（按顺序）
     * @throws BusinessException 任意文章不存在时抛出 400 并整体回滚
     */
    @Transactional
    public TopicVO updateArticles(Long id, List<Long> articleIds) {
        ScienceTopic topic = topicMapper.selectById(id);
        if (topic == null) {
            throw BusinessException.notFound("专题不存在");
        }

        List<Long> distinctIds = articleIds == null
                ? Collections.emptyList()
                : articleIds.stream().filter(Objects::nonNull).distinct().toList();

        if (!distinctIds.isEmpty()) {
            long found = articleMapper.selectBatchIds(distinctIds).size();
            if (found != distinctIds.size()) {
                throw new BusinessException("部分文章不存在，请刷新后重试");
            }
        }

        LambdaQueryWrapper<TopicArticle> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(TopicArticle::getTopicId, id);
        topicArticleMapper.delete(deleteWrapper);

        List<TopicArticle> relations = new ArrayList<>();
        for (int i = 0; i < distinctIds.size(); i++) {
            TopicArticle relation = new TopicArticle();
            relation.setTopicId(id);
            relation.setArticleId(distinctIds.get(i));
            relation.setSortOrder(i + 1);
            relations.add(relation);
        }
        relations.forEach(topicArticleMapper::insert);

        TopicVO vo = toVO(topic);
        vo.setArticleCount((long) distinctIds.size());
        return vo;
    }

    /**
     * 查询专题下的文章。
     *
     * @param onlyPublished true 只取已发布文章（公开场景），false 取全部（后台场景）
     */
    private List<ScienceArticle> findArticlesOfTopic(Long topicId, boolean onlyPublished) {
        LambdaQueryWrapper<TopicArticle> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(TopicArticle::getTopicId, topicId).orderByAsc(TopicArticle::getSortOrder);
        List<TopicArticle> relations = topicArticleMapper.selectList(relWrapper);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> articleIds = relations.stream().map(TopicArticle::getArticleId).toList();

        LambdaQueryWrapper<ScienceArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ScienceArticle::getId, articleIds);
        if (onlyPublished) {
            wrapper.eq(ScienceArticle::getStatus, STATUS_PUBLISHED);
        }
        List<ScienceArticle> articles = articleMapper.selectList(wrapper);

        // 按关联表的 sort_order 排序，而不是按文章自身的发布时间
        Map<Long, ScienceArticle> byId = articles.stream()
                .collect(Collectors.toMap(ScienceArticle::getId, a -> a, (a, b) -> a));
        return articleIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ScienceArticle> findPublishedArticlesOfTopic(Long topicId) {
        return findArticlesOfTopic(topicId, true);
    }

    /**
     * 统计每个专题内已发布文章的数量。
     */
    private Map<Long, Long> countArticlesByTopic(List<Long> topicIds) {
        if (topicIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<TopicArticle> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.in(TopicArticle::getTopicId, topicIds);
        List<TopicArticle> relations = topicArticleMapper.selectList(relWrapper);
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> articleIds = relations.stream().map(TopicArticle::getArticleId).distinct().toList();
        LambdaQueryWrapper<ScienceArticle> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.in(ScienceArticle::getId, articleIds)
                .eq(ScienceArticle::getStatus, STATUS_PUBLISHED);
        List<ScienceArticle> published = articleMapper.selectList(articleWrapper);
        if (published.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> publishedIds = published.stream()
                .collect(Collectors.toMap(ScienceArticle::getId, a -> 1L, Long::sum));

        return relations.stream()
                .filter(rel -> publishedIds.containsKey(rel.getArticleId()))
                .collect(Collectors.groupingBy(TopicArticle::getTopicId, Collectors.counting()));
    }

    private TopicVO toVO(ScienceTopic topic) {
        TopicVO vo = new TopicVO();
        vo.setId(topic.getId());
        vo.setTitle(topic.getTitle());
        vo.setDescription(topic.getDescription());
        vo.setCoverUrl(topic.getCoverUrl());
        vo.setStatus(topic.getStatus());
        vo.setSortOrder(topic.getSortOrder());
        vo.setCreatedAt(topic.getCreatedAt());
        return vo;
    }
}
