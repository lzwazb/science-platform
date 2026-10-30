package com.science.platform.modules.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.work.entity.WorkArticleRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作品文章关联 Mapper。
 */
@Mapper
public interface WorkArticleRelationMapper extends BaseMapper<WorkArticleRelation> {
}
