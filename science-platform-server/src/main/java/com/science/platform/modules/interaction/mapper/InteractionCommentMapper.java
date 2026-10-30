package com.science.platform.modules.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.interaction.entity.InteractionComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论 Mapper。
 */
@Mapper
public interface InteractionCommentMapper extends BaseMapper<InteractionComment> {
}