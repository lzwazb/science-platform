package com.science.platform.modules.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.event.entity.EventSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 活动投稿 Mapper。
 */
@Mapper
public interface EventSubmissionMapper extends BaseMapper<EventSubmission> {

    /**
     * 统计某活动的投稿数。
     *
     * <p>用于删除活动前的引用检查：已有投稿的活动不应被直接删除，
     * 否则 {@code event_submission} 会变成悬空引用。</p>
     *
     * @param eventId 活动 ID
     * @return 投稿条数
     */
    @Select("SELECT COUNT(*) FROM event_submission WHERE event_id = #{eventId}")
    long countByEventId(@Param("eventId") Long eventId);
}
