package com.science.platform.modules.interaction;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.interaction.dto.CommentCreateRequest;
import com.science.platform.modules.interaction.service.CommentService;
import com.science.platform.modules.interaction.service.InteractionTargetResolver;
import com.science.platform.modules.interaction.vo.CommentVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论接口。
 *
 * <p>查看评论对游客开放；发表与删除需要登录。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentService commentService;
    private final InteractionTargetResolver targetResolver;

    public CommentController(CommentService commentService,
                             InteractionTargetResolver targetResolver) {
        this.commentService = commentService;
        this.targetResolver = targetResolver;
    }

    /**
     * 按目标查询评论（一级评论 + 回复）。
     */
    @GetMapping
    public ApiResponse<PageResult<CommentVO>> list(
            @RequestParam @NotBlank(message = "请指定目标类型") String targetType,
            @RequestParam @NotNull(message = "请指定目标 ID") Long targetId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size) {

        return ApiResponse.success(PageResult.of(commentService.pageComments(
                targetType, targetId, page, size, SecurityUtils.currentUserIdOrNull())));
    }

    /**
     * 发表评论或回复。
     */
    @PostMapping
    public ApiResponse<CommentVO> create(@Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.success("评论已发布", commentService.create(
                request, SecurityUtils.currentUserId(), targetResolver));
    }

    /**
     * 删除评论：作者本人或管理员。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        boolean isAdmin = SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN");
        commentService.delete(id, SecurityUtils.currentUserId(), isAdmin);
        return ApiResponse.success("评论已删除", null);
    }
}
