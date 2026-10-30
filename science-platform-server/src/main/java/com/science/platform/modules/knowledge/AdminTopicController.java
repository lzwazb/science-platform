package com.science.platform.modules.knowledge;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.knowledge.dto.TopicArticlesRequest;
import com.science.platform.modules.knowledge.dto.TopicSaveRequest;
import com.science.platform.modules.knowledge.service.TopicService;
import com.science.platform.modules.knowledge.vo.TopicVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台专题维护接口。
 */
@RestController
@RequestMapping("/api/v1/admin/topics")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminTopicController {

    private final TopicService topicService;

    public AdminTopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    /**
     * 专题列表，包含草稿与已下架。
     */
    @GetMapping
    public ApiResponse<List<TopicVO>> list() {
        return ApiResponse.success(topicService.listAll());
    }

    /**
     * 专题详情，含全部文章（包含草稿）。
     */
    @GetMapping("/{id}")
    public ApiResponse<TopicVO> detail(@PathVariable Long id) {
        return ApiResponse.success(topicService.getDetailForAdmin(id));
    }

    /**
     * 创建专题。
     */
    @PostMapping
    public ApiResponse<TopicVO> create(@Valid @RequestBody TopicSaveRequest request) {
        return ApiResponse.success("专题已创建", topicService.create(request));
    }

    /**
     * 编辑专题。
     */
    @PutMapping("/{id}")
    public ApiResponse<TopicVO> update(@PathVariable Long id,
                                       @Valid @RequestBody TopicSaveRequest request) {
        return ApiResponse.success("专题已更新", topicService.update(id, request));
    }

    /**
     * 删除专题，同时清理其与文章的关联。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        topicService.delete(id);
        return ApiResponse.success("专题已删除", null);
    }

    /**
     * 整体替换专题内的文章列表。
     */
    @PutMapping("/{id}/articles")
    public ApiResponse<TopicVO> updateArticles(@PathVariable Long id,
                                               @Valid @RequestBody TopicArticlesRequest request) {
        return ApiResponse.success("专题文章已更新",
                topicService.updateArticles(id, request.getArticleIds()));
    }
}
