package com.science.platform.modules.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 科普文章 Mapper。
 */
@Mapper
public interface ScienceArticleMapper extends BaseMapper<ScienceArticle> {

    /**
     * 按主键加排他锁读取文章（{@code SELECT ... FOR UPDATE}）。
     *
     * <p>用于知识贡献审核：同一篇文章可能存在多条待审核建议，
     * 两位评审员同时点「通过」时，如果不加锁，两个事务都会读到同一个
     * {@code version_no}，进而写入相同版本号的历史快照——
     * 虽然唯一索引能拦下第二次插入，但报错是数据库异常，用户看不懂。</p>
     *
     * <p>加行锁后第二个事务会等待第一个提交完成，读到的就是最新版本号，
     * 快照与版本号自然正确递增。**必须在事务中调用才有效果。**</p>
     *
     * @param id 文章 ID
     * @return 文章实体；不存在或已逻辑删除时返回 null
     */
    @Select("SELECT * FROM science_article WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    ScienceArticle selectByIdForUpdate(@Param("id") Long id);

    /**
     * 阅读量原子自增。
     *
     * <p><b>为什么不更新 updated_at：</b>表定义里 {@code updated_at} 带
     * {@code ON UPDATE CURRENT_TIMESTAMP}，任何 UPDATE 都会把它刷新成当前时间——
     * 也就是说光避开 {@code updateById} 还不够，数据库自己会改。
     * 这里显式 {@code SET updated_at = updated_at}，MySQL 见到显式赋值就不会自动更新，
     * 于是「最后更新时间」保持语义正确，不会被「最后一次被点开」污染。</p>
     *
     * <p>自增交给数据库做 {@code view_count + 1}，并发下不会丢计数。</p>
     *
     * @param id 文章 ID
     * @return 受影响行数
     */
    @Update("UPDATE science_article "
            + "SET view_count = view_count + 1, updated_at = updated_at "
            + "WHERE id = #{id} AND deleted = 0")
    int increaseViewCount(@Param("id") Long id);
}
