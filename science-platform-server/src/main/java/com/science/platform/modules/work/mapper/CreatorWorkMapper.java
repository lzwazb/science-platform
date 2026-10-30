package com.science.platform.modules.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.work.entity.CreatorWork;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 创意作品 Mapper。
 */
@Mapper
public interface CreatorWorkMapper extends BaseMapper<CreatorWork> {

    /**
     * 统计作品被活动投稿引用的次数。
     *
     * <p>活动模块在阶段 8 才有实体与 Mapper，这里用一条原生查询避免提前建表映射；
     * 删除作品前用它判断是否已被投稿引用（开发档案 FR-WK-02）。</p>
     *
     * @param workId 作品 ID
     * @return 引用该作品的投稿条数
     */
    @Select("SELECT COUNT(*) FROM event_submission WHERE work_id = #{workId}")
    long countEventSubmissions(@Param("workId") Long workId);
}
