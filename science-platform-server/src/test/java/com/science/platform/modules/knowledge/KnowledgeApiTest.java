package com.science.platform.modules.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 阶段 4 知识库公开接口测试。
 *
 * <p>类级 {@code @Transactional} 让每个用例结束后回滚，避免测试期间产生
 * 的草稿、阅读量变化污染演示数据。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KnowledgeApiTest {

    private static final long PUBLISHED_TOTAL = 8L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScienceArticleMapper scienceArticleMapper;

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String adminToken() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("username", "admin01", "password", "123456"));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("token").asText();
    }

    @Test
    @DisplayName("游客可浏览文章列表，分页字段符合约定")
    void guestCanListArticles() throws Exception {
        mockMvc.perform(get("/api/v1/articles").param("page", "1").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(PUBLISHED_TOTAL))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(3))
                // 列表项不应包含正文，避免响应体膨胀
                .andExpect(jsonPath("$.data.records[0].content").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].categoryName").isNotEmpty())
                .andExpect(jsonPath("$.data.records[0].authorName").isNotEmpty());
    }

    @Test
    @DisplayName("T04 关键词搜索只返回匹配项，且 % 被转义不匹配全部")
    void searchMatchesKeywordAndEscapesWildcard() throws Exception {
        mockMvc.perform(get("/api/v1/articles/search").param("keyword", "DNA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("DNA 双螺旋：生命如何储存信息"));

        // 未转义时 "%" 会匹配全部 8 篇；转义后应命中 0 篇
        mockMvc.perform(get("/api/v1/articles/search").param("keyword", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("按分类筛选只返回该分类下的文章")
    void filterByCategory() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/articles")
                        .param("categoryId", "1")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode records = readJson(result).path("data").path("records");
        assertEquals(2, records.size(), "天文宇宙分类下有 2 篇已发布文章");
        records.forEach(node -> assertEquals("天文宇宙", node.path("categoryName").asText()));
    }

    @Test
    @DisplayName("文章详情返回正文、分类、作者与所属专题")
    void articleDetailContainsRelations() throws Exception {
        mockMvc.perform(get("/api/v1/articles/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("DNA 双螺旋：生命如何储存信息"))
                .andExpect(jsonPath("$.data.content").isNotEmpty())
                .andExpect(jsonPath("$.data.categoryName").value("生命科学"))
                .andExpect(jsonPath("$.data.authorName").value("内容管理员"))
                .andExpect(jsonPath("$.data.versionNo").value(2))
                .andExpect(jsonPath("$.data.topics[0].title").value("生命的密码：遗传与演化"));
    }

    @Test
    @DisplayName("阅读量随详情访问递增")
    void viewCountIncreases() throws Exception {
        int before = scienceArticleMapper.selectById(3L).getViewCount();

        mockMvc.perform(get("/api/v1/articles/3")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/articles/3")).andExpect(status().isOk());

        int after = scienceArticleMapper.selectById(3L).getViewCount();
        assertEquals(before + 2, after, "两次访问应使阅读量 +2");
    }

    @Test
    @DisplayName("不存在的文章返回 404")
    void missingArticleReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/articles/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("草稿对游客完全不可见：列表不含、详情 404，但后台可见")
    void draftInvisibleToPublic() throws Exception {
        ScienceArticle draft = new ScienceArticle();
        draft.setTitle("【测试】草稿不应公开");
        draft.setSummary("测试用草稿");
        draft.setContent("正文");
        draft.setAuthorId(2L);
        draft.setCategoryId(3L);
        draft.setStatus("DRAFT");
        draft.setViewCount(0);
        draft.setVersionNo(1);
        draft.setPublishedAt(LocalDateTime.now());
        scienceArticleMapper.insert(draft);

        // 公开列表总数不变
        mockMvc.perform(get("/api/v1/articles").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(PUBLISHED_TOTAL));

        // 公开详情 404
        mockMvc.perform(get("/api/v1/articles/" + draft.getId()))
                .andExpect(status().isNotFound());

        // 后台列表能看到（总数 = 8 + 1）
        String token = adminToken();
        MvcResult adminResult = mockMvc.perform(get("/api/v1/admin/articles")
                        .param("size", "100")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(PUBLISHED_TOTAL + 1, readJson(adminResult).path("data").path("total").asLong());
    }

    @Test
    @DisplayName("普通用户访问后台文章接口返回 403")
    void normalUserCannotAccessAdminArticles() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("username", "user01", "password", "123456"));
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        String token = readJson(login).path("data").path("token").asText();

        mockMvc.perform(get("/api/v1/admin/articles").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("专题列表返回已发布专题及其文章数")
    void topicListWithCounts() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode topics = readJson(result).path("data");
        assertEquals(3, topics.size());

        long total = 0;
        for (JsonNode topic : topics) {
            total += topic.path("articleCount").asLong();
        }
        assertEquals(6, total, "演示数据中共 6 条专题文章关联");
    }

    @Test
    @DisplayName("专题详情按 sort_order 返回文章，而不是按发布时间")
    void topicDetailKeepsOrder() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/topics/2"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode articles = readJson(result).path("data").path("articles");
        assertEquals(3, articles.size());
        // seed 中专题 2 的顺序：DNA(3) → 光合作用(4) → 疫苗(8)，与发布时间顺序不同
        assertEquals(3L, articles.get(0).path("id").asLong());
        assertEquals(4L, articles.get(1).path("id").asLong());
        assertEquals(8L, articles.get(2).path("id").asLong());
    }

    @Test
    @DisplayName("分类接口只返回启用中的分类，且按类型过滤")
    void categoriesOnlyEnabled() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/categories").param("type", "ARTICLE"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode categories = readJson(result).path("data");
        assertEquals(6, categories.size());
        for (JsonNode node : categories) {
            assertEquals("ARTICLE", node.path("type").asText());
            assertEquals("ENABLED", node.path("status").asText());
        }
    }

    @Test
    @DisplayName("分页参数越界被校验拦截")
    void invalidPagingRejected() throws Exception {
        mockMvc.perform(get("/api/v1/articles").param("page", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/articles").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("未发布的专题详情返回 404（公开口径）")
    void unpublishedTopicInvisible() throws Exception {
        MvcResult adminResult = mockMvc.perform(post("/api/v1/admin/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "【测试】草稿专题",
                                "status", "DRAFT")))
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andReturn();

        long topicId = readJson(adminResult).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/topics/" + topicId))
                .andExpect(status().isNotFound());

        String raw = mockMvc.perform(get("/api/v1/topics")).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(raw.contains("草稿专题"), "公开专题列表不应出现草稿专题");
    }
}
