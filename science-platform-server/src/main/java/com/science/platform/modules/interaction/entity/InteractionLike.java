package com.science.platform.modules.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点赞实体。
 *
 * <p>表上有 {@code uk_like(user_id, target_type, target_id)} 唯一约束，
 * 同一用户对同一目标最多一条记录——防重复靠数据库，不靠应用层先查后插。</p>
 */
@Data
@TableName("interaction_like")
public class InteractionLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String targetType;

    private Long targetId;

    private LocalDateTime createdAt;
}
