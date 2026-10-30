package com.science.platform.modules.interaction.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的收藏项。文章与作品混排，用 targetType 区分跳转路径。
 */
@Data
public class FavoriteItemVO {

    private String targetType;

    private Long targetId;

    private String title;

    private String summary;

    private String coverUrl;

    /** 目标是否仍然可访问（文章被下架 / 作品被删除时为 false） */
    private boolean available;

    private LocalDateTime createdAt;
}
