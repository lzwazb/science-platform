package com.science.platform.modules.knowledge.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 更新专题包含的文章请求。
 *
 * <p>整体替换语义：传入的列表就是更新后专题内的全部文章（按列表顺序排序）。
 * 传空列表表示清空专题。</p>
 */
@Data
public class TopicArticlesRequest {

    @NotNull(message = "文章 ID 列表不能为空")
    private List<Long> articleIds;
}
