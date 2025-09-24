package com.cloud.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.order.module.entity.Order;
import org.apache.ibatis.annotations.Param;

/**
 * 订单Mapper接口
 * 提供订单相关的数据库操作，包括状态机的条件更新SQL
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
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

    // ==================== 订单状态机条件更新SQL（并发安全） ====================

    /**
     * 条件更新订单状态 - 从待支付到已支付
     * 确保只有待支付状态的订单才能更新为已支付
     *
     * @param orderId 订单ID
     * @return 影响行数，0表示状态不匹配或并发冲突
     */
    int updateStatusToPaid(@Param("orderId") Long orderId);

    /**
     * 条件更新订单状态 - 从已支付到已发货
     * 确保只有已支付状态的订单才能更新为已发货
     *
     * @param orderId 订单ID
     * @return 影响行数，0表示状态不匹配或并发冲突
     */
    int updateStatusToShipped(@Param("orderId") Long orderId);

    /**
     * 条件更新订单状态 - 从已发货到已完成
     * 确保只有已发货状态的订单才能更新为已完成
     *
     * @param orderId 订单ID
     * @return 影响行数，0表示状态不匹配或并发冲突
     */
    int updateStatusToCompleted(@Param("orderId") Long orderId);

    /**
     * 条件取消订单 - 从待支付到已取消
     * 确保只有待支付状态的订单才能取消
     *
     * @param orderId 订单ID
     * @return 影响行数，0表示状态不匹配或并发冲突
     */
    int cancelOrderFromPending(@Param("orderId") Long orderId);

    /**
     * 条件取消订单 - 从已支付到已取消（退款场景）
     * 确保只有已支付状态的订单才能取消
     *
     * @param orderId 订单ID
     * @return 影响行数，0表示状态不匹配或并发冲突
     */
    int cancelOrderFromPaid(@Param("orderId") Long orderId);

    /**
     * 根据订单ID查询订单（加行锁）
     * 用于需要加锁查询的场景
     *
     * @param orderId 订单ID
     * @return 订单信息
     */
    Order selectByIdForUpdate(@Param("orderId") Long orderId);

    /**
     * 批量条件更新订单状态
     * 支持批量状态转换，保证原子性
     *
     * @param orderIds   订单ID列表
     * @param fromStatus 原状态
     * @param toStatus   目标状态
     * @return 影响行数
     */
    int batchUpdateStatus(@Param("orderIds") java.util.List<Long> orderIds,
                          @Param("fromStatus") Integer fromStatus,
                          @Param("toStatus") Integer toStatus);
}