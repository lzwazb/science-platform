package com.science.platform.modules.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.knowledge.entity.TopicArticle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 专题文章关联 Mapper。
 */
@Mapper
public interface TopicArticleMapper extends BaseMapper<TopicArticle> {
}
