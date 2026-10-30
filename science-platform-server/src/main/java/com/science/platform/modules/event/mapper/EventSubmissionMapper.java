package com.science.platform.modules.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.event.entity.EventSubmission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 活动投稿 Mapper。
 */
@Mapper
public interface EventSubmissionMapper extends BaseMapper<EventSubmission> {
}