package com.cloud.common.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.PageQuery;

/**
 * 分页工具类
 */
public class PageUtils {

    /**
     * 根据分页查询参数构建MyBatis Plus的Page对象
     *
     * @param pageQuery 分页查询参数
     * @param <T>       分页数据类型
     * @return MyBatis Plus的Page对象
     */
    public static <T> Page<T> buildPage(PageQuery pageQuery) {
        // 创建Page对象，current为当前页码，size为每页大小
        Page<T> page = new Page<>(pageQuery.getCurrent(), pageQuery.getSize());

        // 设置排序字段和方向
        if (pageQuery.getOrderBy() != null && !pageQuery.getOrderBy().isEmpty()) {
            if ("desc".equalsIgnoreCase(pageQuery.getOrderDirection())) {
                page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc(pageQuery.getOrderBy()));
            } else {
                page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.asc(pageQuery.getOrderBy()));
            }
        }

        return page;
    }
}