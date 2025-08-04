package com.cloud.common.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用分页查询基础类
 */
@Data
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码，默认第1页
     */
    private Integer current = 1;

    /**
     * 每页大小，默认10条，最大100条
     */
    private Integer size = 10;

    /**
     * 排序字段
     */
    private String orderBy;

    /**
     * 排序方式：asc升序，desc降序，默认降序
     */
    private String orderType = "desc";

    /**
     * 设置每页大小，限制最大值
     */
    public void setSize(Integer size) {
        if (size != null && size > 0) {
            this.size = Math.min(size, 100); // 限制最大100条
        }
    }

    /**
     * 设置当前页码，确保不小于1
     */
    public void setCurrent(Integer current) {
        if (current != null && current > 0) {
            this.current = current;
        }
    }

    /**
     * 获取偏移量
     */
    public long getOffset() {
        return (long) (current - 1) * size;
    }
}