package com.science.platform.modules.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.work.entity.CreatorWork;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 浏览量原子自增。
     *
     * <p>与文章的阅读量同理：自增交给数据库做，并发不丢计数；
     * 并显式 {@code SET updated_at = updated_at} 抑制
     * {@code ON UPDATE CURRENT_TIMESTAMP}，避免「最后更新」被浏览行为污染。</p>
     *
     * @param id 作品 ID
     * @return 受影响行数
     */
    @Update("UPDATE creator_work "
            + "SET view_count = view_count + 1, updated_at = updated_at "
            + "WHERE id = #{id} AND deleted = 0")
    int increaseViewCount(@Param("id") Long id);

}
