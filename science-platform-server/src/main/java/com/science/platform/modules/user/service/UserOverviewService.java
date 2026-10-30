package com.science.platform.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.science.platform.modules.contribution.entity.KnowledgeContribution;
import com.science.platform.modules.contribution.mapper.KnowledgeContributionMapper;
import com.science.platform.modules.event.entity.EventSubmission;
import com.science.platform.modules.event.mapper.EventSubmissionMapper;
import com.science.platform.modules.interaction.entity.InteractionFavorite;
import com.science.platform.modules.interaction.entity.ContentFeedback;
import com.science.platform.modules.interaction.mapper.ContentFeedbackMapper;
import com.science.platform.modules.interaction.mapper.InteractionFavoriteMapper;
import com.science.platform.modules.notification.service.NotificationService;
import com.science.platform.modules.user.vo.UserOverviewVO;
import com.science.platform.modules.work.entity.CreatorWork;
import com.science.platform.modules.work.mapper.CreatorWorkMapper;
import org.springframework.stereotype.Service;

/**
 * 个人中心概况统计。
 *
 * <p>为了少写五个接口，把各类计数聚合成一个对象一次返回。
 * 这里跨模块直接查各表计数（只读），比让每个模块各自提供 count 接口更简单。</p>
 */
@Service
public class UserOverviewService {

    private final KnowledgeContributionMapper contributionMapper;
    private final CreatorWorkMapper workMapper;
    private final InteractionFavoriteMapper favoriteMapper;
    private final EventSubmissionMapper submissionMapper;
    private final ContentFeedbackMapper feedbackMapper;
    private final NotificationService notificationService;

    public UserOverviewService(KnowledgeContributionMapper contributionMapper,
                               CreatorWorkMapper workMapper,
                               InteractionFavoriteMapper favoriteMapper,
                               EventSubmissionMapper submissionMapper,
                               ContentFeedbackMapper feedbackMapper,
                               NotificationService notificationService) {
        this.contributionMapper = contributionMapper;
        this.workMapper = workMapper;
        this.favoriteMapper = favoriteMapper;
        this.submissionMapper = submissionMapper;
        this.feedbackMapper = feedbackMapper;
        this.notificationService = notificationService;
    }

    /**
     * 统计当前用户的各类数量。
     */
    public UserOverviewVO overviewOf(Long userId) {
        UserOverviewVO vo = new UserOverviewVO();

        vo.setContributionCount(contributionMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeContribution>()
                        .eq(KnowledgeContribution::getSubmitterId, userId)));
        vo.setPendingContributionCount(contributionMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeContribution>()
                        .eq(KnowledgeContribution::getSubmitterId, userId)
                        .eq(KnowledgeContribution::getStatus, "PENDING")));
        vo.setApprovedContributionCount(contributionMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeContribution>()
                        .eq(KnowledgeContribution::getSubmitterId, userId)
                        .eq(KnowledgeContribution::getStatus, "APPROVED")));

        vo.setWorkCount(workMapper.selectCount(
                new LambdaQueryWrapper<CreatorWork>()
                        .eq(CreatorWork::getCreatorId, userId)));
        vo.setPublishedWorkCount(workMapper.selectCount(
                new LambdaQueryWrapper<CreatorWork>()
                        .eq(CreatorWork::getCreatorId, userId)
                        .eq(CreatorWork::getStatus, "PUBLISHED")));

        vo.setFavoriteCount(favoriteMapper.selectCount(
                new LambdaQueryWrapper<InteractionFavorite>()
                        .eq(InteractionFavorite::getUserId, userId)));

        vo.setSubmissionCount(submissionMapper.selectCount(
                new LambdaQueryWrapper<EventSubmission>()
                        .eq(EventSubmission::getSubmitterId, userId)));

        vo.setFeedbackCount(feedbackMapper.selectCount(
                new LambdaQueryWrapper<ContentFeedback>()
                        .eq(ContentFeedback::getUserId, userId)));

        vo.setUnreadNotificationCount(notificationService.countUnread(userId));
        return vo;
    }
}
