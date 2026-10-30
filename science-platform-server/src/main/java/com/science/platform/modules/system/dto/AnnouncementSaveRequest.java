package com.science.platform.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公告新增 / 编辑请求。
 */
@Data
public class AnnouncementSaveRequest {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 150, message = "公告标题不能超过 150 个字符")
    private String title;

    @NotBlank(message = "公告正文不能为空")
    @Size(max = 5000, message = "公告正文不能超过 5000 个字符")
    private String content;

    /** 排序，数值越大越靠前 */
    private Integer sortOrder;

    @Pattern(regexp = "DRAFT|PUBLISHED|OFFLINE", message = "公告状态只能是草稿、已发布或已下架")
    private String status;
}
