package com.cloud.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.order.module.entity.Order;
import org.apache.ibatis.annotations.Param;

/**
 * @author what's up
 * @description 针对表【order(订单主表)】的数据库操作Mapper
 * @createDate 2025-08-16 19:50:33
 * @Entity com.cloud.order.module.entity.Order
 */
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 分页查询订单
     *
     * @param page     分页对象
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Page<Order> pageQuery(@Param("page") Page<Order> page, @Param("query") OrderPageQueryDTO queryDTO);
}