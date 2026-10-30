package com.science.platform.modules.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 科普文章 Mapper。
 */
@Mapper
public interface ScienceArticleMapper extends BaseMapper<ScienceArticle> {
}
