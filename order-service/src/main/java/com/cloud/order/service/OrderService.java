package com.cloud.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.OrderVO;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.order.module.entity.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 订单服务接口
 * 提供订单相关的业务操作接口定义
 *
 * @author cloud
 * @since 1.0.0
 */
public interface OrderService extends IService<Order> {

    Page<OrderVO> pageQuery(OrderPageQueryDTO queryDTO);

    OrderDTO getByOrderEntityId(Long id);

    Boolean updateOrder(@Valid OrderDTO orderDTO);

    Boolean saveOrder(@Valid OrderDTO orderDTO);

    /**
     * 支付订单
     *
     * @param orderId 订单ID
     * @return 是否支付成功
     */
    Boolean payOrder(Long orderId);

    /**
     * 发货订单
     *
     * @param orderId 订单ID
     * @return 是否发货成功
     */
    Boolean shipOrder(Long orderId);

    /**
     * 完成订单
     *
     * @param orderId 订单ID
     * @return 是否完成成功
     */
    Boolean completeOrder(Long orderId);

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @return 是否取消成功
     */
    Boolean cancelOrder(Long orderId);

    /**
     * 创建订单
     *
     * @param orderCreateDTO 订单创建信息
     * @param currentUserId  当前用户ID
     * @return 订单信息
     */
    Boolean createOrder(OrderCreateDTO orderCreateDTO, String currentUserId);

    /**
     * 支付订单
     *
     * @param orderId       订单ID
     * @param currentUserId 当前用户ID
     * @return 是否支付成功
     */
    Boolean payOrder(Long orderId, String currentUserId);

    /**
     * 发货订单
     *
     * @param orderId       订单ID
     * @param currentUserId 当前用户ID
     * @return 是否发货成功
     */
    Boolean shipOrder(Long orderId, String currentUserId);

    /**
     * 完成订单
     *
     * @param orderId       订单ID
     * @param currentUserId 当前用户ID
     * @return 是否完成成功
     */
    Boolean completeOrder(Long orderId, String currentUserId);

    /**
     * 取消订单
     *
     * @param orderId       订单ID
     * @param currentUserId 当前用户ID
     * @return 是否取消成功
     */
    Boolean cancelOrder(Long orderId, String currentUserId);

    Boolean deleteOrder(@NotNull Long id);

    /**
     * 创建订单（简化版本）
     *
     * @param orderDTO 订单信息
     * @return 创建的订单信息
     */
    OrderDTO createOrder(OrderDTO orderDTO);

    /**
     * 根据用户ID查询订单列表
     *
     * @param userId 用户ID
     * @return 订单列表
     */
    List<OrderDTO> getOrdersByUserId(Long userId);

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    OrderDTO getOrderByOrderNo(String orderNo);
}