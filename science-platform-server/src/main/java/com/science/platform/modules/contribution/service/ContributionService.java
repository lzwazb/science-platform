package com.science.platform.modules.contribution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.contribution.dto.ContributionSubmitRequest;
import com.science.platform.modules.contribution.entity.KnowledgeContribution;
import com.science.platform.modules.contribution.mapper.KnowledgeContributionMapper;
import com.science.platform.modules.contribution.vo.ContributionDetailVO;
import com.science.platform.modules.contribution.vo.ContributionListItemVO;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import com.science.platform.modules.user.entity.SysUser;
import com.science.platform.modules.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识贡献的提交与个人查询。
 */
@Service
public class ContributionService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";

    private final KnowledgeContributionMapper contributionMapper;
    private final ScienceArticleMapper articleMapper;
    private final SysUserMapper sysUserMapper;

    public ContributionService(KnowledgeContributionMapper contributionMapper,
                               ScienceArticleMapper articleMapper,
                               SysUserMapper sysUserMapper) {
        this.contributionMapper = contributionMapper;
        this.articleMapper = articleMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 提交知识贡献。
     *
     * <p>只能针对已发布的正式文章提交——草稿还没有读者，纠错没有意义。
     * 参考来源必填：科普内容要经得起查证，这是本平台与普通评论的核心区别。</p>
     *
     * @param request     提交内容
     * @param submitterId 当前登录用户 ID
     */
    @Transactional
    public ContributionDetailVO submit(ContributionSubmitRequest request, Long submitterId) {
        ScienceArticle article = articleMapper.selectById(request.getArticleId());
        if (article == null || !"PUBLISHED".equals(article.getStatus())) {
            throw BusinessException.notFound("文章不存在或未发布");
        }

        KnowledgeContribution contribution = new KnowledgeContribution();
        contribution.setArticleId(article.getId());
        contribution.setSubmitterId(submitterId);
        contribution.setContributionType(request.getContributionType());
        contribution.setSuggestedContent(request.getSuggestedContent().trim());
        contribution.setReason(request.getReason().trim());
        contribution.setReferenceSource(request.getReferenceSource().trim());
        contribution.setStatus(STATUS_PENDING);
        contributionMapper.insert(contribution);

        return getDetailForSubmitter(contribution.getId(), submitterId);
    }

    /**
     * 我的贡献记录，按提交时间倒序。
     */
    public IPage<ContributionListItemVO> pageMine(Long submitterId, long page, long size, String status) {
        LambdaQueryWrapper<KnowledgeContribution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeContribution::getSubmitterId, submitterId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(KnowledgeContribution::getStatus, status);
        }
        wrapper.orderByDesc(KnowledgeContribution::getCreatedAt).orderByDesc(KnowledgeContribution::getId);

        Page<KnowledgeContribution> result = contributionMapper.selectPage(new Page<>(page, size), wrapper);
        Page<ContributionListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toListItems(result.getRecords()));
        return voPage;
    }

    /**
     * 查看本人的某条贡献。
     */
    public ContributionDetailVO getDetailForSubmitter(Long id, Long submitterId) {
        KnowledgeContribution contribution = contributionMapper.selectById(id);
        if (contribution == null) {
            throw BusinessException.notFound("贡献记录不存在");
        }
        if (!Objects.equals(contribution.getSubmitterId(), submitterId)) {
            throw BusinessException.forbidden("只能查看自己提交的贡献");
        }
        return toDetail(contribution);
    }

    /**
     * 撤回自己的贡献：只允许撤回仍在待审核状态的记录。
     */
    @Transactional
    public ContributionDetailVO withdraw(Long id, Long submitterId) {
        KnowledgeContribution contribution = contributionMapper.selectById(id);
        if (contribution == null) {
            throw BusinessException.notFound("贡献记录不存在");
        }
        if (!Objects.equals(contribution.getSubmitterId(), submitterId)) {
            throw BusinessException.forbidden("只能撤回自己提交的贡献");
        }
        if (!STATUS_PENDING.equals(contribution.getStatus())) {
            throw new BusinessException("只有待审核状态的建议可以撤回");
        }

        LambdaUpdateWrapper<KnowledgeContribution> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(KnowledgeContribution::getId, id)
                .eq(KnowledgeContribution::getStatus, STATUS_PENDING)
                .set(KnowledgeContribution::getStatus, STATUS_WITHDRAWN)
                .set(KnowledgeContribution::getUpdatedAt, LocalDateTime.now());
        int affected = contributionMapper.update(null, wrapper);
        if (affected == 0) {
            // 并发下被评审员抢先处理了
            throw new BusinessException("该建议已被处理，无法撤回");
        }

        return toDetail(contributionMapper.selectById(id));
    }

    /**
     * 批量转列表项：文章标题与提交人昵称一次补齐。
     */
    List<ContributionListItemVO> toListItems(List<KnowledgeContribution> contributions) {
        if (contributions == null || contributions.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> articleIds = contributions.stream()
                .map(KnowledgeContribution::getArticleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> userIds = contributions.stream()
                .map(KnowledgeContribution::getSubmitterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> articleTitles = articleIds.isEmpty()
                ? Collections.emptyMap()
                : articleMapper.selectBatchIds(articleIds).stream()
                        .collect(Collectors.toMap(ScienceArticle::getId, ScienceArticle::getTitle));
        Map<Long, String> userNames = userIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));

        return contributions.stream().map(contribution -> {
            ContributionListItemVO vo = new ContributionListItemVO();
            vo.setId(contribution.getId());
            vo.setArticleId(contribution.getArticleId());
            vo.setArticleTitle(articleTitles.get(contribution.getArticleId()));
            vo.setSubmitterId(contribution.getSubmitterId());
            vo.setSubmitterName(userNames.get(contribution.getSubmitterId()));
            vo.setContributionType(contribution.getContributionType());
            vo.setStatus(contribution.getStatus());
            vo.setReason(contribution.getReason());
            vo.setReviewComment(contribution.getReviewComment());
            vo.setReviewedAt(contribution.getReviewedAt());
            vo.setCreatedAt(contribution.getCreatedAt());
            return vo;
        }).toList();
    }

    ContributionDetailVO toDetail(KnowledgeContribution contribution) {
        ContributionDetailVO vo = new ContributionDetailVO();
        vo.setId(contribution.getId());
        vo.setArticleId(contribution.getArticleId());
        vo.setSubmitterId(contribution.getSubmitterId());
        vo.setContributionType(contribution.getContributionType());
        vo.setSuggestedContent(contribution.getSuggestedContent());
        vo.setReason(contribution.getReason());
        vo.setReferenceSource(contribution.getReferenceSource());
        vo.setStatus(contribution.getStatus());
        vo.setReviewerId(contribution.getReviewerId());
        vo.setReviewComment(contribution.getReviewComment());
        vo.setFinalContent(contribution.getFinalContent());
        vo.setReviewedAt(contribution.getReviewedAt());
        vo.setCreatedAt(contribution.getCreatedAt());
        vo.setUpdatedAt(contribution.getUpdatedAt());

        ScienceArticle article = articleMapper.selectById(contribution.getArticleId());
        if (article != null) {
            vo.setArticleTitle(article.getTitle());
            vo.setArticleContent(article.getContent());
            vo.setArticleVersionNo(article.getVersionNo());
        }
        SysUser submitter = sysUserMapper.selectById(contribution.getSubmitterId());
        if (submitter != null) {
            vo.setSubmitterName(submitter.getNickname());
        }
        if (contribution.getReviewerId() != null) {
            SysUser reviewer = sysUserMapper.selectById(contribution.getReviewerId());
            if (reviewer != null) {
                vo.setReviewerName(reviewer.getNickname());
            }
        }
        return vo;
    }
}
