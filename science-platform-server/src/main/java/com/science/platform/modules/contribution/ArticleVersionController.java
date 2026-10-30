package com.science.platform.modules.contribution;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.contribution.service.ArticleVersionService;
import com.science.platform.modules.contribution.vo.ArticleVersionDetailVO;
import com.science.platform.modules.contribution.vo.ArticleVersionListItemVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文章历史版本接口。
 *
 * <p>已发布文章的版本历史对所有人可读（体现内容可追溯这一项目特色）；
 * 未发布文章的版本历史要求评审员及以上角色，避免提前泄漏草稿。</p>
 */
@RestController
@RequestMapping("/api/v1/articles")
public class ArticleVersionController {

    private final ArticleVersionService versionService;

    public ArticleVersionController(ArticleVersionService versionService) {
        this.versionService = versionService;
    }

    /**
     * 版本列表，按版本号倒序。
     */
    @GetMapping("/{id}/versions")
    public ApiResponse<List<ArticleVersionListItemVO>> list(@PathVariable Long id) {
        return ApiResponse.success(versionService.listVersions(id));
    }

    /**
     * 指定版本的内容快照。
     */
    @GetMapping("/{id}/versions/{versionNo}")
    public ApiResponse<ArticleVersionDetailVO> detail(@PathVariable Long id,
                                                      @PathVariable Integer versionNo) {
        return ApiResponse.success(versionService.getVersion(id, versionNo));
    }
}
