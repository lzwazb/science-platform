package com.science.platform.modules.interaction.vo;

import lombok.Data;

/**
 * 某个目标的互动状态汇总，用于详情页一次性渲染按钮状态与计数。
 */
@Data
public class InteractionStatusVO {

    private String targetType;

    private Long targetId;

    /** 当前用户是否已点赞（未登录恒为 false） */
    private boolean liked;

    /** 当前用户是否已收藏（未登录恒为 false） */
    private boolean favorited;

    private long likeCount;

    private long favoriteCount;

    private long commentCount;
}
