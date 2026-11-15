package com.science.platform.modules.interaction.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台评论管理列表项。
 *
 * <p>与前台 {@link CommentVO} 的区别：后台需要看到评论挂在哪个内容上、
 * 内容标题是什么，以及已删除评论（追查违规内容用），所以单独建一个 VO，
 * 不把后台字段混进前台结构。</p>
 */
@Data
public class AdminCommentVO {

    private Long id;

    private Long userId;

    private String userName;

    private String userAvatar;

    /** ARTICLE / WORK */
    private String targetType;

    private Long targetId;

    /** 目标内容标题；目标已不存在时为「已删除的文章/作品」 */
    private String targetTitle;

    /** 父评论 ID；为空表示一级评论 */
    private Long parentId;

    private String content;

    /** VISIBLE / HIDDEN / DELETED */
    private String status;

    private LocalDateTime createdAt;
}
