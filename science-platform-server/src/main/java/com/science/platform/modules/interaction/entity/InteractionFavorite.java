package com.science.platform.modules.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏实体，结构与点赞一致，同样有唯一约束防重复。
 */
@Data
@TableName("interaction_favorite")
public class InteractionFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String targetType;

    private Long targetId;

    private LocalDateTime createdAt;
}
