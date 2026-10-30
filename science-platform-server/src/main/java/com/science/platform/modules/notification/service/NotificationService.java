package com.science.platform.modules.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.notification.entity.SysNotification;
import com.science.platform.modules.notification.mapper.SysNotificationMapper;
import com.science.platform.modules.notification.vo.NotificationVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内通知：写入（阶段 6 起）与查询、已读（阶段 9 补）。
 *
 * <p>所有查询都强制带 {@code recipient_id} 条件，避免「改个 id 就能读别人消息」
 * 这类越权——开发档案 FR-ME-04 要求只允许读取或更新自己的消息。</p>
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SysNotificationMapper notificationMapper;

    public NotificationService(SysNotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /**
     * 写入一条站内通知。
     *
     * <p>由审核、获奖确认等业务在同一事务内调用，保证「业务成功即通知送达」。</p>
     */
    public void send(Long recipientId, String type, String title, String content,
                     String relatedType, Long relatedId) {
        SysNotification notification = new SysNotification();
        notification.setRecipientId(recipientId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedType(relatedType);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);

        log.debug("已发送站内通知: recipient={}, type={}, related={}#{}",
                recipientId, type, relatedType, relatedId);
    }

    /**
     * 我的消息分页。
     *
     * @param onlyUnread 只看未读
     */
    public IPage<NotificationVO> pageMine(Long recipientId, long page, long size, boolean onlyUnread) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotification::getRecipientId, recipientId);
        if (onlyUnread) {
            wrapper.eq(SysNotification::getIsRead, 0);
        }
        wrapper.orderByDesc(SysNotification::getCreatedAt).orderByDesc(SysNotification::getId);

        Page<SysNotification> result = notificationMapper.selectPage(new Page<>(page, size), wrapper);
        Page<NotificationVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 未读消息数，用于顶栏小红点。
     */
    public long countUnread(Long recipientId) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotification::getRecipientId, recipientId)
                .eq(SysNotification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper);
    }

    /**
     * 标记单条消息已读。
     *
     * @throws BusinessException 消息不存在或不属于当前用户时返回 404（不区分二者，避免探测他人消息 ID）
     */
    @Transactional
    public void markRead(Long id, Long recipientId) {
        LambdaUpdateWrapper<SysNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotification::getId, id)
                .eq(SysNotification::getRecipientId, recipientId)
                .eq(SysNotification::getIsRead, 0)
                .set(SysNotification::getIsRead, 1)
                .set(SysNotification::getReadAt, LocalDateTime.now());
        int affected = notificationMapper.update(null, wrapper);

        if (affected == 0) {
            // 可能是已读、也可能不属于本人：统一按 404 处理
            SysNotification exist = notificationMapper.selectById(id);
            if (exist == null || !exist.getRecipientId().equals(recipientId)) {
                throw BusinessException.notFound("消息不存在");
            }
            // 已经读过，视为幂等成功
        }
    }

    /**
     * 全部标记已读。
     *
     * @return 受影响条数
     */
    @Transactional
    public int markAllRead(Long recipientId) {
        LambdaUpdateWrapper<SysNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotification::getRecipientId, recipientId)
                .eq(SysNotification::getIsRead, 0)
                .set(SysNotification::getIsRead, 1)
                .set(SysNotification::getReadAt, LocalDateTime.now());
        return notificationMapper.update(null, wrapper);
    }

    private NotificationVO toVO(SysNotification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setType(notification.getType());
        vo.setTypeLabel(typeLabel(notification.getType()));
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setRelatedType(notification.getRelatedType());
        vo.setRelatedId(notification.getRelatedId());
        vo.setLinkPath(linkPath(notification.getRelatedType(), notification.getRelatedId()));
        vo.setRead(notification.getIsRead() != null && notification.getIsRead() == 1);
        vo.setCreatedAt(notification.getCreatedAt());
        vo.setReadAt(notification.getReadAt());
        return vo;
    }

    private String typeLabel(String type) {
        return switch (type == null ? "" : type) {
            case "REVIEW_RESULT" -> "审核结果";
            case "COMMENT_REPLY" -> "评论回复";
            case "EVENT_NOTICE" -> "活动通知";
            case "SYSTEM" -> "系统消息";
            default -> "消息";
        };
    }

    /**
     * 由消息反推业务详情页路径，前端点击即可跳转。
     */
    private String linkPath(String relatedType, Long relatedId) {
        if (relatedType == null || relatedId == null) {
            return null;
        }
        return switch (relatedType) {
            case "ARTICLE" -> "/knowledge/" + relatedId;
            case "WORK" -> "/works/" + relatedId;
            case "EVENT" -> "/events/" + relatedId;
            // 贡献没有独立的公开详情页，跳到「我的贡献」由用户自行对照
            case "CONTRIBUTION" -> "/contributions/mine";
            default -> null;
        };
    }

    /**
     * 供其他模块批量补通知（预留）。
     */
    static List<String> supportedTypes() {
        return List.of("REVIEW_RESULT", "COMMENT_REPLY", "EVENT_NOTICE", "SYSTEM");
    }
}
