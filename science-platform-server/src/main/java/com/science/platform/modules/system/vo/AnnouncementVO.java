package com.science.platform.modules.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统公告展示对象。
 */
@Data
public class AnnouncementVO {

    private Long id;

    private String title;

    private String content;

    /** DRAFT / PUBLISHED / OFFLINE */
    private String status;

    /** 状态中文名，前端直接展示 */
    private String statusLabel;

    private Integer sortOrder;

    private Long publisherId;

    private String publisherName;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
