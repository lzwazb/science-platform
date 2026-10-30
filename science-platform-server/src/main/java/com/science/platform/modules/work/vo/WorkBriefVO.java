package com.science.platform.modules.work.vo;

import lombok.Data;

/**
 * 作品简要信息，用于文章详情页展示「相关创意作品」。
 */
@Data
public class WorkBriefVO {

    private Long id;

    private String title;

    private String description;

    private String coverUrl;

    private String creatorName;
}
