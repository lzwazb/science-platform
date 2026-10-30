package com.science.platform.modules.contribution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.science.platform.modules.contribution.dto.ContributionReviewRequest;
import com.science.platform.modules.contribution.entity.ArticleVersion;
import com.science.platform.modules.contribution.entity.KnowledgeContribution;
import com.science.platform.modules.contribution.mapper.ArticleVersionMapper;
import com.science.platform.modules.contribution.mapper.KnowledgeContributionMapper;
import com.science.platform.modules.contribution.service.ContributionReviewService;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 第二轮优化：同一篇文章多条贡献并发审核。
 *
 * <p><b>为什么这个测试类不加 {@code @Transactional}：</b>
 * Spring 的事务上下文基于 ThreadLocal，子线程里调用 Service 会开启独立事务。
 * 如果测试方法本身带事务，子线程看不到测试线程插入的未提交数据，
 * 而且行锁行为也和真实并发不一致。所以这里手动准备与清理数据。</p>
 *
 * <p><b>要验证的不变量：</b></p>
 * <ol>
 *   <li>不会出现两条相同 {@code (article_id, version_no)} 的历史快照</li>
 *   <li>文章最终版本号 = 初始版本号 + 成功采纳的贡献数</li>
 *   <li>每一条成功的贡献都对应一条可追溯的快照</li>
 * </ol>
 */
@SpringBootTest
class ContributionConcurrencyTest {

    /** 演示数据里没有历史版本记录的文章，避免与既有数据冲突 */
    private static final long TARGET_ARTICLE_ID = 6L;
    private static final long REVIEWER_ID = 3L;

    @Autowired
    private ContributionReviewService reviewService;

    @Autowired
    private KnowledgeContributionMapper contributionMapper;

    @Autowired
    private ArticleVersionMapper versionMapper;

    @Autowired
    private ScienceArticleMapper articleMapper;

    private final List<Long> createdContributionIds = new ArrayList<>();
    private int originalVersionNo;
    private String originalContent;

    @BeforeEach
    void rememberOriginalState() {
        ScienceArticle article = articleMapper.selectById(TARGET_ARTICLE_ID);
        originalVersionNo = article.getVersionNo();
        originalContent = article.getContent();
    }

    @AfterEach
    void cleanup() {
        // 删除本次测试产生的贡献与历史版本，恢复文章原状
        for (Long id : createdContributionIds) {
            contributionMapper.deleteById(id);
        }
        createdContributionIds.clear();

        LambdaQueryWrapper<ArticleVersion> versionQuery = new LambdaQueryWrapper<>();
        versionQuery.eq(ArticleVersion::getArticleId, TARGET_ARTICLE_ID)
                .ge(ArticleVersion::getVersionNo, originalVersionNo);
        versionMapper.delete(versionQuery);

        ScienceArticle restore = new ScienceArticle();
        restore.setId(TARGET_ARTICLE_ID);
        restore.setVersionNo(originalVersionNo);
        restore.setContent(originalContent);
        articleMapper.updateById(restore);
    }

    @Test
    @DisplayName("同一文章两条贡献并发通过：不产生重复版本号，版本号按成功数递增")
    void concurrentApproveDoesNotDuplicateVersions() throws Exception {
        long firstId = createPendingContribution("并发测试建议 A：补充了关于氨基酸序列的说明。");
        long secondId = createPendingContribution("并发测试建议 B：补充了关于结构预测方法的说明。");

        int successCount = runConcurrently(firstId, secondId);

        // 1. 版本号无重复
        LambdaQueryWrapper<ArticleVersion> query = new LambdaQueryWrapper<>();
        query.eq(ArticleVersion::getArticleId, TARGET_ARTICLE_ID);
        List<ArticleVersion> versions = versionMapper.selectList(query);
        List<Integer> versionNos = versions.stream().map(ArticleVersion::getVersionNo).toList();
        Set<Integer> unique = versionNos.stream().collect(Collectors.toSet());
        assertEquals(versionNos.size(), unique.size(),
                "同一文章不应出现重复的历史版本号，实际为 " + versionNos);

        // 2. 成功采纳的贡献都有对应快照
        assertEquals(successCount, versions.size(),
                "成功采纳 " + successCount + " 条，应恰好产生 " + successCount + " 条历史快照");

        // 3. 文章版本号 = 初始值 + 成功数
        ScienceArticle after = articleMapper.selectById(TARGET_ARTICLE_ID);
        assertEquals(originalVersionNo + successCount, after.getVersionNo(),
                "文章版本号应按成功采纳的数量递增");

        assertTrue(successCount >= 1, "至少应有一条贡献被成功采纳");
    }

    @Test
    @DisplayName("同一贡献被并发重复处理：只有一次生效")
    void concurrentApproveSameContributionOnlyOnce() throws Exception {
        long contributionId = createPendingContribution("并发重复处理测试：只应生效一次。");
        long firstId = contributionId;
        long secondId = contributionId;

        runConcurrently(firstId, secondId);

        LambdaQueryWrapper<ArticleVersion> query = new LambdaQueryWrapper<>();
        query.eq(ArticleVersion::getArticleId, TARGET_ARTICLE_ID);
        query.eq(ArticleVersion::getSourceContributionId, contributionId);
        long snapshotCount = versionMapper.selectCount(query);

        assertEquals(1, snapshotCount, "同一条贡献只应产生一条历史快照");

        ScienceArticle after = articleMapper.selectById(TARGET_ARTICLE_ID);
        assertEquals(originalVersionNo + 1, after.getVersionNo(), "文章只应升一个版本");
    }

    /**
     * 用两个线程同时调用审核通过。
     *
     * @return 成功次数
     */
    private int runConcurrently(long firstId, long secondId) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(2);
        AtomicInteger success = new AtomicInteger(0);
        List<Exception> failures = new ArrayList<>();

        for (long id : new long[]{firstId, secondId}) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    ContributionReviewRequest request = new ContributionReviewRequest();
                    request.setComment("并发审核测试通过");
                    request.setFinalContent("并发测试后的正文内容 #" + id);
                    reviewService.approve(id, request, REVIEWER_ID);
                    success.incrementAndGet();
                } catch (Exception ex) {
                    synchronized (failures) {
                        failures.add(ex);
                    }
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = doneGate.await(30, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertTrue(finished, "并发审核应在 30 秒内完成");
        return success.get();
    }

    private long createPendingContribution(String content) {
        KnowledgeContribution contribution = new KnowledgeContribution();
        contribution.setArticleId(TARGET_ARTICLE_ID);
        contribution.setSubmitterId(6L);
        contribution.setContributionType("SUPPLEMENT");
        contribution.setSuggestedContent(content);
        contribution.setReason("并发测试用建议");
        contribution.setReferenceSource("并发测试构造的数据");
        contribution.setStatus("PENDING");
        contributionMapper.insert(contribution);
        createdContributionIds.add(contribution.getId());
        return contribution.getId();
    }
}
