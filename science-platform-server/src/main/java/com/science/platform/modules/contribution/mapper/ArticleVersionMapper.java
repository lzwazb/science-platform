package com.science.platform.modules.contribution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.contribution.entity.ArticleVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章历史版本 Mapper。
 */
@Mapper
public interface ArticleVersionMapper extends BaseMapper<ArticleVersion> {
}
