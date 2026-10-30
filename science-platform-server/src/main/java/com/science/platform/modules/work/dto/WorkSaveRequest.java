package com.science.platform.modules.work.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 作品创建 / 编辑请求。
 */
@Data
public class WorkSaveRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 150, message = "标题不能超过 150 个字符")
    private String title;

    @Size(max = 500, message = "简介不能超过 500 个字符")
    private String description;

    @NotBlank(message = "作品内容不能为空")
    private String content;

    @Size(max = 255, message = "封面地址过长")
    private String coverUrl;

    /** 作品分类 ID，可为空 */
    private Long categoryId;

    /**
     * 关联的科普文章 ID 列表，可为空。
     * 传 null 表示不修改关联（编辑场景），传空数组表示清空关联。
     */
    private List<Long> articleIds;
}
