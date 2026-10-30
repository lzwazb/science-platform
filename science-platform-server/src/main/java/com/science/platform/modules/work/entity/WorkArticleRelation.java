package com.science.platform.modules.work.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作品与科普文章的关联实体。
 */
@Data
@TableName("work_article_relation")
public class WorkArticleRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workId;

    private Long articleId;

    private LocalDateTime createdAt;
}
