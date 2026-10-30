package com.science.platform.modules.contribution;

import com.science.platform.modules.contribution.dto.ContributionReviewRequest;
import com.science.platform.modules.contribution.entity.ArticleVersion;
import com.science.platform.modules.contribution.entity.KnowledgeContribution;
import com.science.platform.modules.contribution.mapper.ArticleVersionMapper;
import com.science.platform.modules.contribution.mapper.KnowledgeContributionMapper;
import com.science.platform.modules.contribution.service.ContributionReviewService;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import com.science.platform.modules.notification.mapper.SysNotificationMapper;
import com.science.platform.modules.system.mapper.SysOperationLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * T09 事务回滚测试。
 *
 * <p>验证开发档案 5.1 的关键约束：审核通过要同时完成「写历史快照 → 更新文章 →
 * 改贡献状态 → 写日志 → 发通知」，**任一步失败必须整体回滚**，
 * 不能出现「文章已改但没有历史版本」或「贡献显示通过但文章未更新」的中间状态。</p>
 *
 * <p>实现方式：用 {@code @MockitoBean} 替换 {@link ArticleVersionMapper}，
 * 让插入快照时抛异常，然后验证文章与贡献记录都保持原样。</p>
 *
 * <p>注意本类**不能**加类级 {@code @Transactional}——那会把业务事务合并进测试事务，
 * 就测不到真实的提交/回滚行为了。因此测试数据在用例结束后手动清理。</p>
 */
@SpringBootTest
class ContributionTransactionTest {

    private static final long ARTICLE_ID = 6L;
    private static final long SUBMITTER_ID = 6L;
    private static final long REVIEWER_ID = 3L;

    @Autowired
    private ContributionReviewService reviewService;

    @Autowired
    private ScienceArticleMapper articleMapper;

    @Autowired
    private KnowledgeContributionMapper contributionMapper;

    @Autowired
    private SysNotificationMapper notificationMapper;

    @Autowired
    private SysOperationLogMapper operationLogMapper;

    /** 替换掉真实的版本 Mapper，用于模拟「写快照失败」 */
    @MockitoBean
    private ArticleVersionMapper articleVersionMapper;

    private Long insertContribution(String status) {
        KnowledgeContribution contribution = new KnowledgeContribution();
        contribution.setArticleId(ARTICLE_ID);
        contribution.setSubmitterId(SUBMITTER_ID);
        contribution.setContributionType("SUPPLEMENT");
        contribution.setSuggestedContent("这条建议不会被真正采纳。");
        contribution.setReason("测试事务回滚");
        contribution.setReferenceSource("测试用来源");
        contribution.setStatus(status);
        contribution.setCreatedAt(LocalDateTime.now());
        contribution.setUpdatedAt(LocalDateTime.now());
        contributionMapper.insert(contribution);
        return contribution.getId();
    }

    @Test
    @DisplayName("T09 写历史快照失败时，文章与贡献状态都不应被修改")
    void rollbackWhenVersionSnapshotFails() {
        Long contributionId = insertContribution("PENDING");

        ScienceArticle before = articleMapper.selectById(ARTICLE_ID);
        int versionBefore = before.getVersionNo();
        String contentBefore = before.getContent();
        long notificationRowsBefore = notificationMapper.selectCount(null);
        long logRowsBefore = operationLogMapper.selectCount(null);

        // 让写快照抛异常（BaseMapper 有 insert(T) 与 insert(Collection) 两个重载，需指明参数类型）
        Mockito.when(articleVersionMapper.insert(ArgumentMatchers.any(ArticleVersion.class)))
                .thenThrow(new RuntimeException("模拟写快照失败"));

        ContributionReviewRequest request = new ContributionReviewRequest();
        request.setComment("通过");
        request.setFinalContent("这是本该写入的正文。");

        assertThrows(RuntimeException.class,
                () -> reviewService.approve(contributionId, request, REVIEWER_ID),
                "写快照失败时审核应整体失败");

        // 文章：版本号与正文都保持原样
        ScienceArticle after = articleMapper.selectById(ARTICLE_ID);
        assertEquals(versionBefore, after.getVersionNo(), "文章版本号不应被修改");
        assertEquals(contentBefore, after.getContent(), "文章正文不应被替换");

        // 贡献：仍是待审核，且没有写入审核痕迹
        KnowledgeContribution afterContribution = contributionMapper.selectById(contributionId);
        assertEquals("PENDING", afterContribution.getStatus(), "贡献状态应仍是待审核");
        assertNull(afterContribution.getReviewerId(), "不应写入审核人");
        assertNull(afterContribution.getReviewedAt(), "不应写入审核时间");
        assertNull(afterContribution.getFinalContent(), "不应写入最终正文");

        // 通知与日志：都不应产生
        assertEquals(notificationRowsBefore, notificationMapper.selectCount(null), "不应生成站内通知");
        assertEquals(logRowsBefore, operationLogMapper.selectCount(null), "不应写入操作日志");

        contributionMapper.deleteById(contributionId);
    }

    @Test
    @DisplayName("T09 补充：非待审核状态的建议被拒绝时，不产生任何副作用")
    void rejectNonPendingHasNoSideEffects() {
        Long contributionId = insertContribution("APPROVED");

        ScienceArticle before = articleMapper.selectById(ARTICLE_ID);
        int versionBefore = before.getVersionNo();
        String contentBefore = before.getContent();
        long notificationRowsBefore = notificationMapper.selectCount(null);

        ContributionReviewRequest request = new ContributionReviewRequest();
        request.setComment("重复通过");

        assertThrows(RuntimeException.class,
                () -> reviewService.approve(contributionId, request, REVIEWER_ID),
                "已通过的贡献不能再被审核");

        ScienceArticle after = articleMapper.selectById(ARTICLE_ID);
        assertEquals(versionBefore, after.getVersionNo(), "被拒绝的审核不应改动文章版本号");
        assertEquals(contentBefore, after.getContent(), "被拒绝的审核不应改动正文");
        assertEquals(notificationRowsBefore, notificationMapper.selectCount(null),
                "被拒绝的审核不应生成通知");

        // 原状态保持不变
        assertEquals("APPROVED", contributionMapper.selectById(contributionId).getStatus(),
                "原状态不应被改写");

        contributionMapper.deleteById(contributionId);
    }
}
