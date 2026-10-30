package com.science.platform.modules.interaction.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论展示对象。
 *
 * <p>{@code replies} 存放对该评论的回复，只有一层，不做递归。</p>
 */
@Data
public class CommentVO {

    private Long id;

    private Long userId;

    private String userName;

    private String userAvatar;

    private Long parentId;

    private String content;

    /** VISIBLE / HIDDEN / DELETED */
    private String status;

    /** 当前登录用户是否为作者，前端据此决定是否显示删除按钮 */
    private boolean mine;

    private LocalDateTime createdAt;

    private List<CommentVO> replies;
}
