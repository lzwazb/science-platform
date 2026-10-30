package com.science.platform.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 统一分页响应结构（开发档案 8.1）。
 *
 * <pre>
 * { "records": [], "total": 0, "pageNum": 1, "pageSize": 10 }
 * </pre>
 *
 * <p>刻意不直接返回 MyBatis-Plus 的 {@code IPage}：它的默认字段名是 {@code current/size}，
 * 与文档约定的 {@code pageNum/pageSize} 不一致，直接返回会让前端两套命名并存。</p>
 */
@Data
public class PageResult<T> {

    private List<T> records;

    private long total;

    private long pageNum;

    private long pageSize;

    public PageResult() {
    }

    public PageResult(List<T> records, long total, long pageNum, long pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    /**
     * 由 MyBatis-Plus 分页对象转换。
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
