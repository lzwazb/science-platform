package com.science.platform.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.science.platform.modules.contribution.entity.KnowledgeContribution;
import com.science.platform.modules.contribution.mapper.KnowledgeContributionMapper;
import com.science.platform.modules.event.entity.CreativeEvent;
import com.science.platform.modules.event.mapper.CreativeEventMapper;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import com.science.platform.modules.system.vo.SiteStatsVO;
import com.science.platform.modules.work.entity.CreatorWork;
import com.science.platform.modules.work.mapper.CreatorWorkMapper;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 首页数据概览。
 *
 * <p>只统计「前台可见」的内容：草稿、待审、已下架的一律不计入，
 * 否则首页会出现「显示 10 篇但点进去只有 8 篇」的落差。</p>
 */
@Service
public class SiteStatsService {

    private final ScienceArticleMapper articleMapper;
    private final CreatorWorkMapper workMapper;
    private final CreativeEventMapper eventMapper;
    private final KnowledgeContributionMapper contributionMapper;

    public SiteStatsService(ScienceArticleMapper articleMapper,
                            CreatorWorkMapper workMapper,
                            CreativeEventMapper eventMapper,
                            KnowledgeContributionMapper contributionMapper) {
        this.articleMapper = articleMapper;
        this.workMapper = workMapper;
        this.eventMapper = eventMapper;
        this.contributionMapper = contributionMapper;
    }

    /**
     * 统计首页展示用的五项数字。
     */
    public SiteStatsVO stats() {
        SiteStatsVO vo = new SiteStatsVO();

        vo.setArticleCount(articleMapper.selectCount(
                new LambdaQueryWrapper<ScienceArticle>()
                        .eq(ScienceArticle::getStatus, "PUBLISHED")));

        vo.setWorkCount(workMapper.selectCount(
                new LambdaQueryWrapper<CreatorWork>()
                        .eq(CreatorWork::getStatus, "PUBLISHED")));

        // 活动统计「前台可见」的口径：已结束的活动在活动列表里仍然可见，
        // 所以要算进来，否则首页显示 1 个、点进去看到 2 个会对不上
        vo.setEventCount(eventMapper.selectCount(
                new LambdaQueryWrapper<CreativeEvent>()
                        .in(CreativeEvent::getStatus, "PUBLISHED", "CLOSED")));

        vo.setAdoptedContributionCount(contributionMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeContribution>()
                        .eq(KnowledgeContribution::getStatus, "APPROVED")));

        // 创作者人数：按已发布作品的作者去重，反映「真正产出了内容的人」
        Set<Long> creatorIds = workMapper.selectList(
                        new LambdaQueryWrapper<CreatorWork>()
                                .eq(CreatorWork::getStatus, "PUBLISHED")
                                .select(CreatorWork::getCreatorId))
                .stream()
                .map(CreatorWork::getCreatorId)
                .collect(Collectors.toSet());
        vo.setCreatorCount(creatorIds.size());

        return vo;
    }
}
