package com.science.platform.modules.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 专题展示对象。
 *
 * <p>列表场景下 {@code articles} 为空，{@code articleCount} 有值；
 * 详情场景下两者都填充。</p>
 */
@Data
public class TopicVO {

    private Long id;

    private String title;

    private String description;

    private String coverUrl;

    /** DRAFT / PUBLISHED / OFFLINE */
    private String status;

    private Integer sortOrder;

    /** 专题内已发布文章数 */
    private Long articleCount;

    private LocalDateTime createdAt;

    /** 详情场景才有值 */
    private List<ArticleListItemVO> articles;
}
