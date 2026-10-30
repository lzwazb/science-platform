package com.science.platform.modules.interaction;

import com.science.platform.common.ApiResponse;
import com.science.platform.common.PageResult;
import com.science.platform.modules.interaction.dto.InteractionRequest;
import com.science.platform.modules.interaction.service.InteractionService;
import com.science.platform.modules.interaction.vo.FavoriteItemVO;
import com.science.platform.modules.interaction.vo.InteractionStatusVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 点赞与收藏接口。
 *
 * <p>写操作需要登录；状态查询对游客开放（游客能看计数，但 liked/favorited 恒为 false）。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/interactions")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    /**
     * 点赞（幂等：已点过不报错，也不会产生第二条记录）。
     */
    @PostMapping("/likes")
    public ApiResponse<Map<String, Object>> like(@Valid @RequestBody InteractionRequest request) {
        long count = interactionService.like(SecurityUtils.currentUserId(),
                request.getTargetType(), request.getTargetId());
        return ApiResponse.success("已点赞", Map.of("likeCount", count, "liked", true));
    }

    /**
     * 取消点赞（幂等）。
     */
    @DeleteMapping("/likes")
    public ApiResponse<Map<String, Object>> unlike(@Valid @RequestBody InteractionRequest request) {
        long count = interactionService.unlike(SecurityUtils.currentUserId(),
                request.getTargetType(), request.getTargetId());
        return ApiResponse.success("已取消点赞", Map.of("likeCount", count, "liked", false));
    }

    /**
     * 收藏（幂等）。
     */
    @PostMapping("/favorites")
    public ApiResponse<Map<String, Object>> favorite(@Valid @RequestBody InteractionRequest request) {
        long count = interactionService.favorite(SecurityUtils.currentUserId(),
                request.getTargetType(), request.getTargetId());
        return ApiResponse.success("已收藏", Map.of("favoriteCount", count, "favorited", true));
    }

    /**
     * 取消收藏（幂等）。
     */
    @DeleteMapping("/favorites")
    public ApiResponse<Map<String, Object>> unfavorite(@Valid @RequestBody InteractionRequest request) {
        long count = interactionService.unfavorite(SecurityUtils.currentUserId(),
                request.getTargetType(), request.getTargetId());
        return ApiResponse.success("已取消收藏", Map.of("favoriteCount", count, "favorited", false));
    }

    /**
     * 查询某目标的互动状态与计数，游客可访问。
     */
    @GetMapping("/status")
    public ApiResponse<InteractionStatusVO> status(
            @RequestParam @NotBlank(message = "请指定目标类型") String targetType,
            @RequestParam @jakarta.validation.constraints.NotNull(message = "请指定目标 ID") Long targetId) {
        return ApiResponse.success(interactionService.status(
                SecurityUtils.currentUserIdOrNull(), targetType, targetId));
    }

    /**
     * 我的收藏。
     */
    @GetMapping("/favorites/mine")
    public ApiResponse<PageResult<FavoriteItemVO>> myFavorites(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") long page,
            @RequestParam(defaultValue = "12") @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能超过 100") long size) {
        return ApiResponse.success(PageResult.of(
                interactionService.pageMyFavorites(SecurityUtils.currentUserId(), page, size)));
    }
}
