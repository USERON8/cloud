package com.cloud.common.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询基础类
 */
@Data
public class PageQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private Long current = 1L;

    /**
     * 每页大小
     */
    private Long size = 10L;

    /**
     * 排序字段
     */
    private String orderBy;

    /**
     * 排序方向：asc/desc
     */
    private String orderDirection;
}