package com.cloud.common.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 通用分页结果类
 *
 * @param <T> 数据类型
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    public PageResult() {
    }

    public PageResult(Long current, Long size, Long total, List<T> records) {
        this.current = current;
        this.size = size;
        this.total = total;
        this.records = records;
        this.pages = (total + size - 1) / size; // 计算总页数
        this.hasPrevious = current > 1;
        this.hasNext = current < pages;
    }

    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(Long current, Long size, Long total, List<T> records) {
        return new PageResult<>(current, size, total, records);
    }

    /**
     * 创建空分页结果
     */
    public static <T> PageResult<T> empty(Long current, Long size) {
        return new PageResult<>(current, size, 0L, List.of());
    }
}