package com.science.platform.modules.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.knowledge.entity.ScienceTopic;
import org.apache.ibatis.annotations.Mapper;

/**
 * 专题 Mapper。
 */
@Mapper
public interface ScienceTopicMapper extends BaseMapper<ScienceTopic> {
}
