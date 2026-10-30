package com.science.platform.modules.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.interaction.entity.InteractionFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏 Mapper。
 */
@Mapper
public interface InteractionFavoriteMapper extends BaseMapper<InteractionFavorite> {
}