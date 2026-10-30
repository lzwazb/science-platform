package com.science.platform.modules.system;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.system.service.AnnouncementService;
import com.science.platform.modules.system.service.SiteConfigService;
import com.science.platform.modules.system.service.SiteStatsService;
import com.science.platform.modules.system.vo.AnnouncementVO;
import com.science.platform.modules.system.vo.SiteInfoVO;
import com.science.platform.modules.system.vo.SiteStatsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台公开信息接口，供首页等前台页面读取后台维护的配置。
 */
@RestController
@RequestMapping("/api/v1")
public class SiteController {

    private static final int HOME_ANNOUNCEMENT_LIMIT = 5;

    private final SiteConfigService siteConfigService;
    private final AnnouncementService announcementService;
    private final SiteStatsService siteStatsService;

    public SiteController(SiteConfigService siteConfigService,
                          AnnouncementService announcementService,
                          SiteStatsService siteStatsService) {
        this.siteConfigService = siteConfigService;
        this.announcementService = announcementService;
        this.siteStatsService = siteStatsService;
    }

    /**
     * 首页平台信息：名称、简介、横幅文案 + 最近公告。
     *
     * <p>无需认证。首页拿到这一份数据即可渲染，不用再单独请求公告。</p>
     */
    @GetMapping("/site/info")
    public ApiResponse<SiteInfoVO> siteInfo() {
        SiteInfoVO vo = siteConfigService.loadSiteInfo();
        vo.setAnnouncements(announcementService.listPublished(HOME_ANNOUNCEMENT_LIMIT));
        return ApiResponse.success(vo);
    }

    /**
     * 首页数据概览：文章、作品、活动、采纳贡献、创作者人数。
     */
    @GetMapping("/site/stats")
    public ApiResponse<SiteStatsVO> siteStats() {
        return ApiResponse.success(siteStatsService.stats());
    }

    /**
     * 已发布公告列表。
     *
     * @param limit 最多返回几条，为空返回全部已发布公告
     */
    @GetMapping("/announcements")
    public ApiResponse<List<AnnouncementVO>> announcements(
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(announcementService.listPublished(limit));
    }
}
