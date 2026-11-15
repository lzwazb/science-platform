package com.science.platform.modules.interaction;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.interaction.service.AdminCommentService;
import com.science.platform.modules.interaction.service.CommentService;
import com.science.platform.modules.interaction.vo.AdminCommentVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论管理（后台）。
 *
 * <p>前台 {@code /api/v1/comments} 只能按单个目标查可见评论；
 * 后台需要跨全站检索、看到已删除评论并直接处理违规内容，因此单独开一组接口。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin/comments")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminCommentController {

    private final AdminCommentService adminCommentService;
    private final CommentService commentService;

    public AdminCommentController(AdminCommentService adminCommentService,
                                  CommentService commentService) {
        this.adminCommentService = adminCommentService;
        this.commentService = commentService;
    }

    /**
     * 评论列表。
     *
     * @param targetType 目标类型：ARTICLE / WORK，为空返回全部
     * @param targetId   目标 ID，为空返回全部
     * @param status     状态：VISIBLE / HIDDEN / DELETED，为空返回全部
     * @param keyword    评论内容关键词
     */
    @GetMapping
    public ApiResponse<PageResult<AdminCommentVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(PageResult.of(
                adminCommentService.pageAll(page, size, targetType, targetId, status, keyword)));
    }

    /**
     * 删除违规评论（逻辑删除，保留楼层结构）。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        commentService.delete(id, SecurityUtils.currentUserId(), true);
        return ApiResponse.success("评论已删除", null);
    }
}
