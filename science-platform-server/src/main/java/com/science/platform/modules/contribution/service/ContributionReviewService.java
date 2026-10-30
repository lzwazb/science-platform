package com.science.platform.modules.contribution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.contribution.dto.ContributionReviewRequest;
import com.science.platform.modules.contribution.entity.ArticleVersion;
import com.science.platform.modules.contribution.entity.KnowledgeContribution;
import com.science.platform.modules.contribution.mapper.ArticleVersionMapper;
import com.science.platform.modules.contribution.mapper.KnowledgeContributionMapper;
import com.science.platform.modules.contribution.vo.ContributionDetailVO;
import com.science.platform.modules.contribution.vo.ContributionListItemVO;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import com.science.platform.modules.notification.service.NotificationService;
import com.science.platform.modules.system.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 知识贡献审核 —— 项目的核心业务。
 *
 * <p>审核通过要同时完成六件事，且**必须在一个事务内**（开发档案 5.1）：
 * <ol>
 *   <li>读取当前正式文章</li>
 *   <li>创建历史版本快照</li>
 *   <li>更新正式文章正文与版本号</li>
 *   <li>贡献状态置为 APPROVED</li>
 *   <li>写审核记录（操作日志）</li>
 *   <li>生成站内通知</li>
 * </ol>
 * 任一步失败整体回滚，不允许出现「文章已改但没有历史版本」或
 * 「贡献显示通过但文章没更新」的中间状态。</p>
 */
@Service
public class ContributionReviewService {

    private static final Logger log = LoggerFactory.getLogger(ContributionReviewService.class);

    private final KnowledgeContributionMapper contributionMapper;
    private final ArticleVersionMapper articleVersionMapper;
    private final ScienceArticleMapper articleMapper;
    private final ContributionService contributionService;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    public ContributionReviewService(KnowledgeContributionMapper contributionMapper,
                                     ArticleVersionMapper articleVersionMapper,
                                     ScienceArticleMapper articleMapper,
                                     ContributionService contributionService,
                                     NotificationService notificationService,
                                     OperationLogService operationLogService) {
        this.contributionMapper = contributionMapper;
        this.articleVersionMapper = articleVersionMapper;
        this.articleMapper = articleMapper;
        this.contributionService = contributionService;
        this.notificationService = notificationService;
        this.operationLogService = operationLogService;
    }

    /**
     * 审核工作台的分页查询。
     *
     * @param status 状态筛选，默认只查待审核
     */
    public IPage<ContributionListItemVO> pageForReview(long page, long size, String status, Long articleId) {
        String effectiveStatus = StringUtils.hasText(status)
                ? status
                : ContributionService.STATUS_PENDING;

        LambdaQueryWrapper<KnowledgeContribution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeContribution::getStatus, effectiveStatus);
        if (articleId != null) {
            wrapper.eq(KnowledgeContribution::getArticleId, articleId);
        }
        // 先提交先处理，避免新建议插队
        wrapper.orderByAsc(KnowledgeContribution::getCreatedAt)
                .orderByAsc(KnowledgeContribution::getId);

        Page<KnowledgeContribution> result = contributionMapper.selectPage(new Page<>(page, size), wrapper);
        Page<ContributionListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(contributionService.toListItems(result.getRecords()));
        return voPage;
    }

    /**
     * 审核详情：同时返回文章当前正文与用户建议，供评审对照。
     */
    public ContributionDetailVO getReviewDetail(Long id) {
        KnowledgeContribution contribution = contributionMapper.selectById(id);
        if (contribution == null) {
            throw BusinessException.notFound("贡献记录不存在");
        }
        return contributionService.toDetail(contribution);
    }

    /**
     * 审核通过：更新正式文章并保存历史版本。
     *
     * @param id         贡献 ID
     * @param request    审核意见与最终正文
     * @param reviewerId 当前登录的评审员 ID
     */
    @Transactional
    public ContributionDetailVO approve(Long id, ContributionReviewRequest request, Long reviewerId) {
        // ---- 1. 校验贡献存在且仍是待审核 ----
        KnowledgeContribution contribution = contributionMapper.selectById(id);
        if (contribution == null) {
            throw BusinessException.notFound("贡献记录不存在");
        }
        if (!ContributionService.STATUS_PENDING.equals(contribution.getStatus())) {
            throw new BusinessException("该建议当前状态为 " + statusText(contribution.getStatus()) + "，不能重复处理");
        }

        // ---- 2. 读取当前正式文章 ----
        ScienceArticle article = articleMapper.selectById(contribution.getArticleId());
        if (article == null) {
            throw BusinessException.notFound("目标文章已被删除，无法更新");
        }

        // ---- 3. 确定最终采用的正文 ----
        String comment = StringUtils.hasText(request.getComment()) ? request.getComment().trim() : null;
        String finalContent = StringUtils.hasText(request.getFinalContent())
                ? request.getFinalContent()
                : contribution.getSuggestedContent();
        if (!StringUtils.hasText(finalContent)) {
            throw new BusinessException("最终采用的正文不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        int currentVersion = article.getVersionNo() == null ? 1 : article.getVersionNo();

        // ---- 4. 创建历史版本快照（保存被覆盖之前的旧内容）----
        ArticleVersion version = new ArticleVersion();
        version.setArticleId(article.getId());
        version.setVersionNo(currentVersion);
        version.setTitleSnapshot(article.getTitle());
        version.setSummarySnapshot(article.getSummary());
        version.setContentSnapshot(article.getContent());
        version.setChangeNote("根据知识贡献 #" + contribution.getId() + " 采纳读者"
                + typeText(contribution.getContributionType()));
        version.setOperatorId(reviewerId);
        version.setSourceContributionId(contribution.getId());
        version.setCreatedAt(now);
        articleVersionMapper.insert(version);

        // ---- 5. 更新正式文章：正文替换 + 版本号加一 ----
        ScienceArticle articleUpdate = new ScienceArticle();
        articleUpdate.setId(article.getId());
        articleUpdate.setContent(finalContent);
        articleUpdate.setVersionNo(currentVersion + 1);
        articleMapper.updateById(articleUpdate);

        // ---- 6. 贡献状态置为 APPROVED（带状态条件的更新，防并发重复处理）----
        LambdaUpdateWrapper<KnowledgeContribution> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(KnowledgeContribution::getId, id)
                .eq(KnowledgeContribution::getStatus, ContributionService.STATUS_PENDING)
                .set(KnowledgeContribution::getStatus, ContributionService.STATUS_APPROVED)
                .set(KnowledgeContribution::getReviewerId, reviewerId)
                .set(KnowledgeContribution::getReviewComment, comment)
                .set(KnowledgeContribution::getFinalContent, finalContent)
                .set(KnowledgeContribution::getReviewedAt, now)
                .set(KnowledgeContribution::getUpdatedAt, now);
        int affected = contributionMapper.update(null, wrapper);
        if (affected == 0) {
            // 并发下已被别人处理：抛异常让整个事务回滚，版本快照与文章更新一起撤销
            throw new BusinessException("该建议已被其他评审处理，请刷新后重试");
        }

        // ---- 7. 写操作日志 ----
        operationLogService.record(reviewerId, "CONTRIBUTION_APPROVE", "CONTRIBUTION", id,
                "通过知识贡献 #" + id + "，文章 #" + article.getId() + " 由第 "
                        + currentVersion + " 版更新至第 " + (currentVersion + 1) + " 版");

        // ---- 8. 生成站内通知 ----
        notificationService.send(
                contribution.getSubmitterId(),
                "REVIEW_RESULT",
                "知识贡献已通过",
                "你提交的对《" + article.getTitle() + "》的" + typeText(contribution.getContributionType())
                        + "建议已通过审核，文章已更新至第 " + (currentVersion + 1) + " 版。",
                "CONTRIBUTION",
                id);

        log.info("知识贡献审核通过: contribution={}, article={}, version {} -> {}",
                id, article.getId(), currentVersion, currentVersion + 1);

        return contributionService.toDetail(contributionMapper.selectById(id));
    }

    /**
     * 审核驳回：必须填写意见，供提交者了解原因。
     */
    @Transactional
    public ContributionDetailVO reject(Long id, ContributionReviewRequest request, Long reviewerId) {
        String comment = request == null || !StringUtils.hasText(request.getComment())
                ? null
                : request.getComment().trim();
        if (comment == null) {
            throw new BusinessException("驳回时必须填写审核意见");
        }

        KnowledgeContribution contribution = contributionMapper.selectById(id);
        if (contribution == null) {
            throw BusinessException.notFound("贡献记录不存在");
        }
        if (!ContributionService.STATUS_PENDING.equals(contribution.getStatus())) {
            throw new BusinessException("该建议当前状态为 " + statusText(contribution.getStatus()) + "，不能重复处理");
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<KnowledgeContribution> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(KnowledgeContribution::getId, id)
                .eq(KnowledgeContribution::getStatus, ContributionService.STATUS_PENDING)
                .set(KnowledgeContribution::getStatus, ContributionService.STATUS_REJECTED)
                .set(KnowledgeContribution::getReviewerId, reviewerId)
                .set(KnowledgeContribution::getReviewComment, comment)
                .set(KnowledgeContribution::getReviewedAt, now)
                .set(KnowledgeContribution::getUpdatedAt, now);
        int affected = contributionMapper.update(null, wrapper);
        if (affected == 0) {
            throw new BusinessException("该建议已被其他评审处理，请刷新后重试");
        }

        operationLogService.record(reviewerId, "CONTRIBUTION_REJECT", "CONTRIBUTION", id,
                "驳回知识贡献 #" + id + "：" + comment);

        ScienceArticle article = articleMapper.selectById(contribution.getArticleId());
        String articleTitle = article == null ? "目标文章" : "《" + article.getTitle() + "》";
        notificationService.send(
                contribution.getSubmitterId(),
                "REVIEW_RESULT",
                "知识贡献未通过",
                "你提交的对" + articleTitle + "的" + typeText(contribution.getContributionType())
                        + "建议未通过审核，审核意见：" + comment,
                "CONTRIBUTION",
                id);

        log.info("知识贡献被驳回: contribution={}, reviewer={}", id, reviewerId);

        return contributionService.toDetail(contributionMapper.selectById(id));
    }

    private String typeText(String type) {
        return switch (type == null ? "" : type) {
            case "SUPPLEMENT" -> "补充";
            case "CORRECTION" -> "纠错";
            case "MODIFICATION" -> "修改";
            default -> "改进";
        };
    }

    private String statusText(String status) {
        return switch (status == null ? "" : status) {
            case "PENDING" -> "待审核";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            case "WITHDRAWN" -> "已撤回";
            default -> status;
        };
    }
}
