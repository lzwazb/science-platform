package com.science.platform.modules.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章创建 / 编辑请求（后台使用）。
 */
@Data
public class ArticleSaveRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 150, message = "标题不能超过 150 个字符")
    private String title;

    @Size(max = 500, message = "摘要不能超过 500 个字符")
    private String summary;

    @NotBlank(message = "正文不能为空")
    private String content;

    @Size(max = 255, message = "封面地址过长")
    private String coverUrl;

    /** 分类 ID，可为空；非空时必须是存在的分类 */
    private Long categoryId;
}
