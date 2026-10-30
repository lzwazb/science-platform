package com.science.platform.modules.system;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.system.dto.AnnouncementSaveRequest;
import com.science.platform.modules.system.dto.SiteSettingsRequest;
import com.science.platform.modules.system.service.AnnouncementService;
import com.science.platform.modules.system.service.SiteConfigService;
import com.science.platform.modules.system.vo.AnnouncementVO;
import com.science.platform.modules.system.vo.SiteInfoVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统基础设置与公告维护（仅超级管理员）。
 *
 * <p>开发档案 V1.1 要求「只有超级管理员能够访问和修改系统基础设置」，
 * 因此这里用 {@code hasRole('SUPER_ADMIN')} 而不是 ADMIN——
 * 普通管理员即使持有登录令牌也拿不到这些接口。</p>
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminSettingsController {

    private final SiteConfigService siteConfigService;
    private final AnnouncementService announcementService;

    public AdminSettingsController(SiteConfigService siteConfigService,
                                   AnnouncementService announcementService) {
        this.siteConfigService = siteConfigService;
        this.announcementService = announcementService;
    }

    // ---------------- 平台基础信息 ----------------

    /**
     * 读取平台基础信息（含未发布的公告不算在内，这里只回配置）。
     */
    @GetMapping("/settings")
    public ApiResponse<SiteInfoVO> settings() {
        return ApiResponse.success(siteConfigService.loadSiteInfo());
    }

    /**
     * 更新平台基础信息。
     */
    @PutMapping("/settings")
    public ApiResponse<SiteInfoVO> updateSettings(@Valid @RequestBody SiteSettingsRequest request) {
        return ApiResponse.success("平台设置已保存",
                siteConfigService.update(request, SecurityUtils.currentUserId()));
    }

    // ---------------- 公告 ----------------

    /**
     * 公告列表，含草稿与已下架。
     *
     * @param status 状态筛选，可为空
     */
    @GetMapping("/announcements")
    public ApiResponse<List<AnnouncementVO>> announcements(
            @RequestParam(required = false) String status) {
        return ApiResponse.success(announcementService.listAll(status));
    }

    /**
     * 新增公告。
     */
    @PostMapping("/announcements")
    public ApiResponse<AnnouncementVO> createAnnouncement(
            @Valid @RequestBody AnnouncementSaveRequest request) {
        return ApiResponse.success("公告已保存",
                announcementService.create(request, SecurityUtils.currentUserId()));
    }

    /**
     * 编辑公告。
     */
    @PutMapping("/announcements/{id}")
    public ApiResponse<AnnouncementVO> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementSaveRequest request) {
        return ApiResponse.success("公告已更新",
                announcementService.update(id, request, SecurityUtils.currentUserId()));
    }

    /**
     * 发布公告。
     */
    @PostMapping("/announcements/{id}/publish")
    public ApiResponse<AnnouncementVO> publishAnnouncement(@PathVariable Long id) {
        return ApiResponse.success("公告已发布",
                announcementService.publish(id, SecurityUtils.currentUserId()));
    }

    /**
     * 下架公告。
     */
    @PostMapping("/announcements/{id}/offline")
    public ApiResponse<AnnouncementVO> offlineAnnouncement(@PathVariable Long id) {
        return ApiResponse.success("公告已下架",
                announcementService.offline(id, SecurityUtils.currentUserId()));
    }

    /**
     * 删除公告（逻辑删除）。
     */
    @DeleteMapping("/announcements/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.delete(id, SecurityUtils.currentUserId());
        return ApiResponse.success("公告已删除", null);
    }
}
