package com.science.platform.modules.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.knowledge.entity.ScienceCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类 Mapper。
 */
@Mapper
public interface ScienceCategoryMapper extends BaseMapper<ScienceCategory> {
}
