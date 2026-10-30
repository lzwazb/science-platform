package com.science.platform.modules.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 前台首页需要的平台信息（公开接口，不含内部字段）。
 */
@Data
public class SiteInfoVO {

    private String name;

    private String subtitle;

    private String description;

    private String banner;

    private String contact;

    /** 最近发布的公告，按 sortOrder 倒序、发布时间倒序 */
    private List<AnnouncementVO> announcements;
}
