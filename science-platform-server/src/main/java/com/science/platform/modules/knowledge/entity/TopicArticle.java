package com.science.platform.modules.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专题与文章关联实体。
 */
@Data
@TableName("topic_article")
public class TopicArticle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long topicId;

    private Long articleId;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
