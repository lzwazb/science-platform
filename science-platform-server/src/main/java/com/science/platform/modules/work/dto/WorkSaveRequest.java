package com.science.platform.modules.work.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

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

    /** 作品形态分类 ID（科普图文 / 海报 / 漫画 / 短视频），可为空 */
    private Long categoryId;

    /**
     * 所属知识分类 ID，可为空。
     *
     * <p>原来作品要挂到具体文章上（articleIds），但创作往往先于文章、或同时覆盖多篇文章，
     * 挂学科分类更自然：作品归属某个领域的知识，读者从该学科里能找到它。
     * 传 null 表示不关联任何学科。</p>
     */
    private Long knowledgeCategoryId;
}
