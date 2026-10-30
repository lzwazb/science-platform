package com.science.platform.modules.knowledge;

import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import com.science.platform.modules.work.entity.CreatorWork;
import com.science.platform.modules.work.mapper.CreatorWorkMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 阅读量自增的正确性。
 *
 * <p>这里锁住两件事，都是真实修过的 bug：</p>
 * <ol>
 *   <li>自增必须走 SQL 原子操作，不能「读出来 +1 再写回」，否则并发丢计数</li>
 *   <li>自增不能改动 {@code updated_at}——表上有 {@code ON UPDATE CURRENT_TIMESTAMP}，
 *       光避开 {@code updateById} 不够，必须显式 {@code SET updated_at = updated_at}</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ViewCountTest {

    private static final long ARTICLE_ID = 4L;
    private static final long WORK_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScienceArticleMapper articleMapper;

    @Autowired
    private CreatorWorkMapper workMapper;

    @Test
    @DisplayName("文章阅读量 +1，updated_at 保持不变")
    void articleViewCountDoesNotTouchUpdatedAt() throws Exception {
        ScienceArticle before = articleMapper.selectById(ARTICLE_ID);
        Integer viewBefore = before.getViewCount();
        LocalDateTime updatedBefore = before.getUpdatedAt();
        assertNotNull(updatedBefore);

        mockMvc.perform(get("/api/v1/articles/" + ARTICLE_ID))
                .andExpect(status().isOk());

        ScienceArticle after = articleMapper.selectById(ARTICLE_ID);
        assertEquals(viewBefore + 1, after.getViewCount(), "阅读量应 +1");
        assertEquals(updatedBefore, after.getUpdatedAt(),
                "阅读不应刷新 updated_at——「最后更新时间」不能被「最后被点开的时间」污染");
    }

    @Test
    @DisplayName("作品浏览量 +1，updated_at 保持不变")
    void workViewCountDoesNotTouchUpdatedAt() throws Exception {
        CreatorWork before = workMapper.selectById(WORK_ID);
        Integer viewBefore = before.getViewCount();
        LocalDateTime updatedBefore = before.getUpdatedAt();
        assertNotNull(updatedBefore);

        mockMvc.perform(get("/api/v1/works/" + WORK_ID))
                .andExpect(status().isOk());

        CreatorWork after = workMapper.selectById(WORK_ID);
        assertEquals(viewBefore + 1, after.getViewCount(), "浏览量应 +1");
        assertEquals(updatedBefore, after.getUpdatedAt(), "浏览不应刷新 updated_at");
    }

    @Test
    @DisplayName("连续多次阅读只累加计数，不改 updated_at")
    void repeatedViewsOnlyChangeCount() throws Exception {
        ScienceArticle before = articleMapper.selectById(ARTICLE_ID);
        int viewBefore = before.getViewCount();
        LocalDateTime updatedBefore = before.getUpdatedAt();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/articles/" + ARTICLE_ID)).andExpect(status().isOk());
        }

        ScienceArticle after = articleMapper.selectById(ARTICLE_ID);
        assertEquals(viewBefore + 5, after.getViewCount());
        assertEquals(updatedBefore, after.getUpdatedAt());
    }
}
