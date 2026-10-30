package com.science.platform.modules.notification.service;

import com.science.platform.modules.notification.entity.SysNotification;
import com.science.platform.modules.notification.mapper.SysNotificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 站内通知写入。
 *
 * <p>阶段 6 只提供写入能力：审核结果需要与「更新文章、写版本快照」在**同一事务**内落库，
 * 否则会出现「文章改了但用户没收到通知」的不一致。查询与已读在阶段 9 补。</p>
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
     * @param recipientId 接收人 ID
     * @param type        通知类型，如 REVIEW_RESULT
     * @param title       标题
     * @param content     内容
     * @param relatedType 关联对象类型，如 CONTRIBUTION
     * @param relatedId   关联对象 ID，便于前端从消息跳回业务详情
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
}
