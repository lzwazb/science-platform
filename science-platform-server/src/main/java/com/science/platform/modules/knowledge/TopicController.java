package com.science.platform.modules.knowledge;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.knowledge.service.TopicService;
import com.science.platform.modules.knowledge.vo.TopicVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 专题公开接口，只返回已发布专题。
 */
@RestController
@RequestMapping("/api/v1/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    /**
     * 专题列表，附带已发布文章数。
     */
    @GetMapping
    public ApiResponse<List<TopicVO>> list() {
        return ApiResponse.success(topicService.listPublished());
    }

    /**
     * 专题详情，含专题内按顺序排列的已发布文章。
     */
    @GetMapping("/{id}")
    public ApiResponse<TopicVO> detail(@PathVariable Long id) {
        return ApiResponse.success(topicService.getPublishedDetail(id));
    }
}
