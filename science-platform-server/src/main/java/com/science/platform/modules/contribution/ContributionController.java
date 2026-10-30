package com.science.platform.modules.contribution;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.contribution.dto.ContributionSubmitRequest;
import com.science.platform.modules.contribution.service.ContributionService;
import com.science.platform.modules.contribution.vo.ContributionDetailVO;
import com.science.platform.modules.contribution.vo.ContributionListItemVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识贡献接口（提交与个人查询）。
 *
 * <p>需要登录：游客不能提交贡献（开发档案第 3 章）。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/contributions")
public class ContributionController {

    private final ContributionService contributionService;

    public ContributionController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    /**
     * 提交知识补充 / 纠错 / 修改建议。
     */
    @PostMapping
    public ApiResponse<ContributionDetailVO> submit(@Valid @RequestBody ContributionSubmitRequest request) {
        return ApiResponse.success("建议已提交，等待评审员处理",
                contributionService.submit(request, SecurityUtils.currentUserId()));
    }

    /**
     * 我的贡献记录。
     */
    @GetMapping("/mine")
    public ApiResponse<PageResult<ContributionListItemVO>> mine(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size,
            @RequestParam(required = false) String status) {

        return ApiResponse.success(PageResult.of(
                contributionService.pageMine(SecurityUtils.currentUserId(), page, size, status)));
    }

    /**
     * 查看本人的某条贡献详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<ContributionDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(
                contributionService.getDetailForSubmitter(id, SecurityUtils.currentUserId()));
    }

    /**
     * 撤回仍在待审核状态的建议。
     */
    @PostMapping("/{id}/withdraw")
    public ApiResponse<ContributionDetailVO> withdraw(@PathVariable Long id) {
        return ApiResponse.success("已撤回",
                contributionService.withdraw(id, SecurityUtils.currentUserId()));
    }
}
