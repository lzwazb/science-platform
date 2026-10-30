package com.science.platform.modules.user.vo;

import lombok.Data;

/**
 * 个人中心的概况统计。
 *
 * <p>个人中心有「我的作品 / 我的贡献 / 我的收藏 / 我的投稿 / 我的反馈」几个 tab，
 * 顶部需要一眼看到各类数量，避免用户逐个 tab 去点。</p>
 */
@Data
public class UserOverviewVO {

    /** 我提交的知识贡献总数 */
    private long contributionCount;

    /** 其中待审核 */
    private long pendingContributionCount;

    /** 其中已通过 */
    private long approvedContributionCount;

    /** 我的作品总数 */
    private long workCount;

    /** 其中已发布 */
    private long publishedWorkCount;

    /** 我的收藏总数 */
    private long favoriteCount;

    /** 我的活动投稿总数 */
    private long submissionCount;

    /** 我提交的内容反馈总数 */
    private long feedbackCount;

    /** 未读消息数 */
    private long unreadNotificationCount;
}
