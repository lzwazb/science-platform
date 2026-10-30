package com.science.platform.modules.knowledge.vo;

import lombok.Data;

/**
 * 专题简要信息，用于文章详情里展示「属于哪些专题」。
 */
@Data
public class TopicBriefVO {

    private Long id;

    private String title;
}
