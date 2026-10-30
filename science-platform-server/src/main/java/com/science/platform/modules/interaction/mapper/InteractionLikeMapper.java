package com.science.platform.modules.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.interaction.entity.InteractionLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞 Mapper。
 */
@Mapper
public interface InteractionLikeMapper extends BaseMapper<InteractionLike> {
}