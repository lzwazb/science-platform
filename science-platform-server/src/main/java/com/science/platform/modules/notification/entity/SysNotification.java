package com.science.platform.modules.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体。
 *
 * <p>阶段 6 只做「写入」（审核结果通知），查询与已读在阶段 9 的站内消息模块里补。</p>
 */
@Data
@TableName("sys_notification")
public class SysNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recipientId;

    /** REVIEW_RESULT / COMMENT_REPLY / EVENT_NOTICE / SYSTEM */
    private String type;

    private String title;

    private String content;

    /** ARTICLE / WORK / CONTRIBUTION / EVENT */
    private String relatedType;

    private Long relatedId;

    /** 0 未读 / 1 已读 */
    private Integer isRead;

    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}
