package com.science.platform.modules.system.vo;

import lombok.Data;

/**
 * 首页数据概览。
 *
 * <p>首页要让访客一眼看出「这个平台有多少内容、有多少人参与」，
 * 而不是只有一句简介。五个数字都只统计前台可见的内容。</p>
 */
@Data
public class SiteStatsVO {

    /** 已发布科普文章数 */
    private long articleCount;

    /** 已发布创意作品数 */
    private long workCount;

    /** 已发布活动数 */
    private long eventCount;

    /** 已采纳的知识贡献数（体现「共建」的活跃度） */
    private long adoptedContributionCount;

    /** 有已发布作品的创作者人数 */
    private long creatorCount;
}
