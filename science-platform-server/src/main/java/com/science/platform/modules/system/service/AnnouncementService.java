package com.science.platform.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.system.dto.AnnouncementSaveRequest;
import com.science.platform.modules.system.entity.SysAnnouncement;
import com.science.platform.modules.system.mapper.SysAnnouncementMapper;
import com.science.platform.modules.system.vo.AnnouncementVO;
import com.science.platform.modules.user.entity.SysUser;
import com.science.platform.modules.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统公告的维护与展示。
 *
 * <p>状态流转：DRAFT → PUBLISHED → OFFLINE，已下架的公告可以重新发布。</p>
 */
@Service
public class AnnouncementService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_OFFLINE = "OFFLINE";

    private final SysAnnouncementMapper announcementMapper;
    private final SysUserMapper sysUserMapper;
    private final OperationLogService operationLogService;

    public AnnouncementService(SysAnnouncementMapper announcementMapper,
                               SysUserMapper sysUserMapper,
                               OperationLogService operationLogService) {
        this.announcementMapper = announcementMapper;
        this.sysUserMapper = sysUserMapper;
        this.operationLogService = operationLogService;
    }

    /**
     * 前台展示的公告：只含已发布，按排序值倒序、发布时间倒序。
     *
     * @param limit 最多返回几条，null 表示不限制
     */
    public List<AnnouncementVO> listPublished(Integer limit) {
        LambdaQueryWrapper<SysAnnouncement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAnnouncement::getStatus, STATUS_PUBLISHED)
                .orderByDesc(SysAnnouncement::getSortOrder)
                .orderByDesc(SysAnnouncement::getPublishedAt)
                .orderByDesc(SysAnnouncement::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return toVOs(announcementMapper.selectList(wrapper));
    }

    /**
     * 后台公告列表，含草稿与已下架。
     *
     * @param status 状态筛选，可为空
     */
    public List<AnnouncementVO> listAll(String status) {
        LambdaQueryWrapper<SysAnnouncement> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysAnnouncement::getStatus, status);
        }
        wrapper.orderByDesc(SysAnnouncement::getSortOrder).orderByDesc(SysAnnouncement::getId);
        return toVOs(announcementMapper.selectList(wrapper));
    }

    /**
     * 新增公告。
     */
    @Transactional
    public AnnouncementVO create(AnnouncementSaveRequest request, Long operatorId) {
        SysAnnouncement announcement = new SysAnnouncement();
        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent());
        announcement.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        announcement.setPublisherId(operatorId);

        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus() : STATUS_DRAFT;
        announcement.setStatus(status);
        // 直接以已发布状态创建时，补上发布时间
        if (STATUS_PUBLISHED.equals(status)) {
            announcement.setPublishedAt(LocalDateTime.now());
        }
        announcementMapper.insert(announcement);

        operationLogService.record(operatorId, "ANNOUNCEMENT_CREATE", "ANNOUNCEMENT",
                announcement.getId(), "新增公告《" + announcement.getTitle() + "》");

        return toVO(announcement);
    }

    /**
     * 编辑公告。不改变状态（状态用发布/下架接口单独控制）。
     */
    @Transactional
    public AnnouncementVO update(Long id, AnnouncementSaveRequest request, Long operatorId) {
        SysAnnouncement announcement = requireAnnouncement(id);

        LambdaUpdateWrapper<SysAnnouncement> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysAnnouncement::getId, id)
                .set(SysAnnouncement::getTitle, request.getTitle().trim())
                .set(SysAnnouncement::getContent, request.getContent())
                .set(SysAnnouncement::getSortOrder,
                        request.getSortOrder() == null ? announcement.getSortOrder() : request.getSortOrder())
                .set(SysAnnouncement::getUpdatedAt, LocalDateTime.now());
        announcementMapper.update(null, wrapper);

        operationLogService.record(operatorId, "ANNOUNCEMENT_UPDATE", "ANNOUNCEMENT", id,
                "编辑公告《" + request.getTitle().trim() + "》");

        return toVO(announcementMapper.selectById(id));
    }

    /**
     * 发布公告：草稿或已下架 → 已发布，首次发布记录发布时间。
     */
    @Transactional
    public AnnouncementVO publish(Long id, Long operatorId) {
        SysAnnouncement announcement = requireAnnouncement(id);
        if (STATUS_PUBLISHED.equals(announcement.getStatus())) {
            throw new BusinessException("公告已是发布状态");
        }

        LambdaUpdateWrapper<SysAnnouncement> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysAnnouncement::getId, id)
                .set(SysAnnouncement::getStatus, STATUS_PUBLISHED)
                .set(SysAnnouncement::getPublishedAt,
                        announcement.getPublishedAt() == null ? LocalDateTime.now() : announcement.getPublishedAt())
                .set(SysAnnouncement::getUpdatedAt, LocalDateTime.now());
        announcementMapper.update(null, wrapper);

        operationLogService.record(operatorId, "ANNOUNCEMENT_PUBLISH", "ANNOUNCEMENT", id,
                "发布公告《" + announcement.getTitle() + "》");

        return toVO(announcementMapper.selectById(id));
    }

    /**
     * 下架公告：前台立即不可见，数据保留。
     */
    @Transactional
    public AnnouncementVO offline(Long id, Long operatorId) {
        SysAnnouncement announcement = requireAnnouncement(id);
        if (STATUS_OFFLINE.equals(announcement.getStatus())) {
            throw new BusinessException("公告已是下架状态");
        }

        LambdaUpdateWrapper<SysAnnouncement> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysAnnouncement::getId, id)
                .set(SysAnnouncement::getStatus, STATUS_OFFLINE)
                .set(SysAnnouncement::getUpdatedAt, LocalDateTime.now());
        announcementMapper.update(null, wrapper);

        operationLogService.record(operatorId, "ANNOUNCEMENT_OFFLINE", "ANNOUNCEMENT", id,
                "下架公告《" + announcement.getTitle() + "》");

        return toVO(announcementMapper.selectById(id));
    }

    /**
     * 删除公告（逻辑删除）。
     */
    @Transactional
    public void delete(Long id, Long operatorId) {
        SysAnnouncement announcement = requireAnnouncement(id);
        announcementMapper.deleteById(id);
        operationLogService.record(operatorId, "ANNOUNCEMENT_DELETE", "ANNOUNCEMENT", id,
                "删除公告《" + announcement.getTitle() + "》");
    }

    private SysAnnouncement requireAnnouncement(Long id) {
        SysAnnouncement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw BusinessException.notFound("公告不存在");
        }
        return announcement;
    }

    private List<AnnouncementVO> toVOs(List<SysAnnouncement> rows) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = rows.stream()
                .map(SysAnnouncement::getPublisherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = userIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));

        return rows.stream().map(row -> {
            AnnouncementVO vo = toVO(row);
            vo.setPublisherName(names.get(row.getPublisherId()));
            return vo;
        }).toList();
    }

    private AnnouncementVO toVO(SysAnnouncement row) {
        AnnouncementVO vo = new AnnouncementVO();
        vo.setId(row.getId());
        vo.setTitle(row.getTitle());
        vo.setContent(row.getContent());
        vo.setStatus(row.getStatus());
        vo.setStatusLabel(statusLabel(row.getStatus()));
        vo.setSortOrder(row.getSortOrder());
        vo.setPublisherId(row.getPublisherId());
        vo.setPublishedAt(row.getPublishedAt());
        vo.setCreatedAt(row.getCreatedAt());
        vo.setUpdatedAt(row.getUpdatedAt());
        return vo;
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case STATUS_DRAFT -> "草稿";
            case STATUS_PUBLISHED -> "已发布";
            case STATUS_OFFLINE -> "已下架";
            default -> status;
        };
    }
}
