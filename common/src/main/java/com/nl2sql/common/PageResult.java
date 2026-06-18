package com.nl2sql.common;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果。页码从 1 开始。
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResult<T> implements Serializable {

    /** 当前页数据 */
    private List<T> records;
    /** 总记录数 */
    private long total;
    /** 当前页码（从 1 开始） */
    private int pageNum;
    /** 每页大小 */
    private int pageSize;
    /** 总页数 */
    private int pages;

    public PageResult() {
        this.records = Collections.emptyList();
    }

    public PageResult(List<T> records, long total, int pageNum, int pageSize) {
        this.records = records != null ? records : Collections.emptyList();
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pageSize > 0 ? (int) ((total + pageSize - 1) / pageSize) : 0;
    }

    /** 空分页 */
    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return new PageResult<>(Collections.emptyList(), 0, pageNum, pageSize);
    }

    /** 由 Spring Data 的 Page 构造（解耦：调用方传入字段，避免 common 依赖 spring-data） */
    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        return new PageResult<>(records, total, pageNum, pageSize);
    }
}
