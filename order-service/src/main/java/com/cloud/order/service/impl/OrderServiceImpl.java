package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.event.order.OrderCompletedEvent;
import com.cloud.common.domain.event.payment.PaymentSuccessEvent;
import com.cloud.common.domain.vo.order.OrderVO;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.order.converter.OrderConverter;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.order.exception.OrderServiceException;
import com.cloud.common.exception.InvalidStatusException;
import com.cloud.order.mapper.OrderMapper;
import com.cloud.common.messaging.BusinessLogProducer;
import com.cloud.common.messaging.AsyncLogProducer;
import com.cloud.order.messaging.producer.OrderEventProducer;
import com.cloud.order.module.entity.Order;
import com.cloud.order.module.entity.OrderItem;
import com.cloud.order.service.OrderItemService;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import com.cloud.common.utils.UserContextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 * 针对表【order(订单主表)】的数据库操作Service实现
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>
        implements OrderService {

    private final OrderConverter orderConverter;
    private final OrderItemService orderItemService;
    private final OrderEventProducer orderEventProducer;
    private final BusinessLogProducer businessLogProducer;
    private final AsyncLogProducer asyncLogProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    public Page<OrderVO> pageQuery(OrderPageQueryDTO queryDTO) {
        try {
            Page<Order> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
            Page<Order> resultPage = this.baseMapper.pageQuery(page, queryDTO);
            // 转换为VO对象
            Page<OrderVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
            voPage.setRecords(resultPage.getRecords().stream()
                    .map(orderConverter::toVO)
                    .collect(Collectors.toList()));
            return voPage;
        } catch (Exception e) {
            log.error("分页查询订单失败: ", e);
            throw new OrderServiceException("分页查询订单失败: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Page<OrderVO> queryOrdersByPage(OrderPageQueryDTO queryDTO) {
        return pageQuery(queryDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Cacheable(cacheNames = "orderCache", key = "#id", unless = "#result == null")
    public OrderDTO getByOrderEntityId(Long id) {
        try {
            // 直接从数据库查询
            Order order = this.baseMapper.selectById(id);
            if (order == null) {
                throw EntityNotFoundException.order(id);
            }

            OrderDTO orderDTO = orderConverter.toDTO(order);
            return orderDTO;
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("根据ID查询订单失败: ", e);
            throw new OrderServiceException("根据ID查询订单失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("@permissionManager.hasUserAccess(authentication)")
    public Boolean saveOrder(OrderDTO orderDTO) {
        try {
            Order order = orderConverter.toEntity(orderDTO);
            boolean result = this.baseMapper.insert(order) > 0;

            if (result) {
                log.info("订单创建成功，订单ID: {}", order.getId());
            }

            return result;
        } catch (Exception e) {
            log.error("保存订单失败: ", e);
            throw new OrderServiceException("保存订单失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "orderCache", key = "#orderDTO.id"),
                    @CacheEvict(cacheNames = "orderListCache", key = "'user:' + #orderDTO.userId"),
                    @CacheEvict(cacheNames = "orderPageCache", allEntries = true)
            }
    )
    public Boolean updateOrder(OrderDTO orderDTO) {
        try {
            // 根据ID获取现有订单记录
            Order existingOrder = this.baseMapper.selectById(orderDTO.getId());
            if (existingOrder == null) {
                throw EntityNotFoundException.order(orderDTO.getId());
            }

            // 更新订单信息
            Order order = orderConverter.toEntity(orderDTO);
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                log.info("订单更新成功，订单ID: {}", order.getId());
            }

            return result;
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新订单失败: ", e);
            throw e;
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'order:pay:' + #orderId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "订单支付操作获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    public Boolean payOrder(Long orderId) {
        try {
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            // 验证订单状态（必须是待支付状态）
            if (order.getStatus() != 0) {
                throw InvalidStatusException.order(order.getStatus().toString(), "支付");
            }
            order.setStatus(1); // 设置为已支付状态
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                // 发送订单支付日志 - 使用统一业务日志系统
                try {
                    asyncLogProducer.sendBusinessLogAsync(
                            "order-service",
                            "ORDER_MANAGEMENT",
                            "PAY",
                            "订单支付操作",
                            orderId.toString(),
                            "ORDER",
                            String.format("{\"status\":%d,\"amount\":%s}", 0, order.getPayAmount()),
                            String.format("{\"status\":%d,\"amount\":%s}", 1, order.getPayAmount()),
                            UserContextUtils.getCurrentUsername() != null ? UserContextUtils.getCurrentUsername() : "SYSTEM",
                            "订单: " + order.getOrderNo()
                    );
                } catch (Exception e) {
                    log.warn("发送订单支付日志失败，订单ID：{}", orderId, e);
                }

                log.info("订单支付成功，订单ID: {}", orderId);
            }
            return result;
        } catch (EntityNotFoundException | InvalidStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付订单失败: ", e);
            throw e;
        }
    }

    @Override
    @PreAuthorize("@permissionChecker.checkPermission(authentication, 'order:manage') or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'order:ship:' + #orderId",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "订单发货操作获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    public Boolean shipOrder(Long orderId) {
        try {
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            // 验证订单状态（必须是已支付状态）
            if (order.getStatus() != 1) {
                throw InvalidStatusException.order(order.getStatus().toString(), "发货");
            }
            order.setStatus(2); // 设置为已发货状态
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                // 发送订单发货日志 - 使用统一业务日志系统
                try {
                    asyncLogProducer.sendBusinessLogAsync(
                            "order-service",
                            "ORDER_MANAGEMENT",
                            "SHIP",
                            "订单发货操作",
                            orderId.toString(),
                            "ORDER",
                            String.format("{\"status\":%d,\"amount\":%s}", 1, order.getTotalAmount()),
                            String.format("{\"status\":%d,\"amount\":%s}", 2, order.getTotalAmount()),
                            UserContextUtils.getCurrentUsername() != null ? UserContextUtils.getCurrentUsername() : "SYSTEM",
                            "订单: " + order.getOrderNo()
                    );
                } catch (Exception e) {
                    log.warn("发送订单发货日志失败，订单ID：{}", orderId, e);
                }

                log.info("订单发货成功，订单ID: {}", orderId);
            }
            return result;
        } catch (EntityNotFoundException | InvalidStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("发货订单失败: ", e);
            throw e;
        }
    }

    @Override
    @DistributedLock(
            key = "'order:complete:' + #orderId",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "订单完成操作获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    public Boolean completeOrder(Long orderId) {
        try {
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            // 验证订单状态（必须是已发货状态）
            if (order.getStatus() != 2) {
                throw InvalidStatusException.order(order.getStatus().toString(), "完成");
            }
            order.setStatus(3); // 设置为已完成状态
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                // 发送订单完成日志 - 使用统一业务日志系统
                try {
                    asyncLogProducer.sendBusinessLogAsync(
                            "order-service",
                            "ORDER_MANAGEMENT",
                            "COMPLETE",
                            "订单完成操作",
                            orderId.toString(),
                            "ORDER",
                            String.format("{\"status\":%d,\"amount\":%s}", 2, order.getTotalAmount()),
                            String.format("{\"status\":%d,\"amount\":%s}", 3, order.getTotalAmount()),
                            UserContextUtils.getCurrentUsername() != null ? UserContextUtils.getCurrentUsername() : "SYSTEM",
                            "订单: " + order.getOrderNo()
                    );
                } catch (Exception e) {
                    log.warn("发送订单完成日志失败，订单ID：{}", orderId, e);
                }

                log.info("订单完成成功，订单ID: {}", orderId);

                // 发布订单完成事件
                publishOrderCompletedEvent(order);
            }

            return result;
        } catch (EntityNotFoundException | InvalidStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("完成订单失败: ", e);
            throw e;
        }
    }

    @Override
    @DistributedLock(
            key = "'order:cancel:' + #orderId",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "订单取消操作获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelOrder(Long orderId) {
        try {
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            // 验证订单状态（必须是待支付或已支付状态）
            if (order.getStatus() != 0 && order.getStatus() != 1) {
                throw InvalidStatusException.order(order.getStatus().toString(), "取消");
            }
            order.setStatus(-1); // 设置为已取消状态
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                // 发送订单取消日志 - 使用统一业务日志系统
                try {
                    asyncLogProducer.sendBusinessLogAsync(
                            "order-service",
                            "ORDER_MANAGEMENT",
                            "CANCEL",
                            "订单取消操作",
                            orderId.toString(),
                            "ORDER",
                            String.format("{\"status\":%d,\"amount\":%s}", order.getStatus(), order.getTotalAmount()),
                            String.format("{\"status\":%d,\"amount\":%s}", -1, order.getTotalAmount()),
                            UserContextUtils.getCurrentUsername() != null ? UserContextUtils.getCurrentUsername() : "SYSTEM",
                            "订单: " + order.getOrderNo()
                    );
                } catch (Exception e) {
                    log.warn("发送订单取消日志失败，订单ID：{}", orderId, e);
                }

                log.info("订单取消成功，订单ID: {}", orderId);
            }
            return result;
        } catch (EntityNotFoundException | InvalidStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("取消订单失败: ", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean createOrder(OrderCreateDTO orderCreateDTO, String currentUserId) {
        Order order = null;
        try {
            log.info("开始创建订单，用户ID: {}，操作人: {}", orderCreateDTO.getUserId(), currentUserId);

            // 1. 创建订单主表
            order = new Order();
            order.setUserId(orderCreateDTO.getUserId());
            order.setTotalAmount(orderCreateDTO.getTotalAmount());
            order.setPayAmount(orderCreateDTO.getPayAmount() != null ? orderCreateDTO.getPayAmount() : orderCreateDTO.getTotalAmount());
            order.setStatus(0); // 待支付状态
            order.setAddressId(orderCreateDTO.getAddressId());

            boolean saved = this.save(order);
            if (!saved) {
                log.error("创建订单主表失败，用户ID: {}，操作人: {}", orderCreateDTO.getUserId(), currentUserId);
                return false;
            }

            // 2. 创建订单明细
            List<OrderItem> orderItems = new ArrayList<>();
            for (OrderCreateDTO.OrderItemDTO itemDTO : orderCreateDTO.getOrderItems()) {
                OrderItem item = new OrderItem();
                item.setOrderId(order.getId());
                item.setProductId(itemDTO.getProductId());
                item.setProductSnapshot(itemDTO.getProductSnapshot().toString());
                item.setQuantity(itemDTO.getQuantity());
                item.setPrice(itemDTO.getPrice());
                item.setCreateBy(Long.valueOf(currentUserId));
                orderItems.add(item);
            }

            boolean itemsSaved = orderItemService.saveBatch(orderItems);
            if (!itemsSaved) {
                log.error("创建订单明细失败，订单ID: {}，操作人: {}", order.getId(), currentUserId);
                throw new RuntimeException("创建订单明细失败");
            }

            // 发送订单创建日志 - 使用统一业务日志系统
            try {
                asyncLogProducer.sendBusinessLogAsync(
                        "order-service",
                        "ORDER_MANAGEMENT",
                        "CREATE",
                        "订单创建操作",
                        order.getId().toString(),
                        "ORDER",
                        null,
                        String.format("{\"status\":%d,\"amount\":%s,\"userId\":%d}",
                                0, order.getTotalAmount(), order.getUserId()),
                        currentUserId != null ? currentUserId : (UserContextUtils.getCurrentUsername() != null ? UserContextUtils.getCurrentUsername() : "SYSTEM"),
                        "订单: ORDER_" + order.getId()
                );
            } catch (Exception e) {
                log.warn("发送订单创建日志失败，订单ID：{}", order.getId(), e);
            }

            log.info("创建订单成功，订单ID: {}，操作人: {}", order.getId(), currentUserId);
            return true;
        } catch (OrderServiceException e) {
            // 已知的订单服务异常直接抛出
            throw e;
        } catch (Exception e) {
            log.error("创建订单异常，用户ID: {}，操作人: {}", orderCreateDTO.getUserId(), currentUserId, e);
            // 其他异常包装成订单服务异常抛出
            throw e;
        } finally {
            // 订单处理完成
            log.debug("订单创建处理完成");
        }
    }

    @Override
    public OrderDTO createOrder(OrderDTO orderDTO) {
        // 简化实现：将DTO转换为实体并保存
        Order order = orderConverter.toEntity(orderDTO);
        order.setStatus(0); // 设置为待支付状态
        this.save(order);
        return orderConverter.toDTO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO createOrder(OrderCreateDTO orderCreateDTO) {
        try {
            // 创建订单实体
            Order order = new Order();
            order.setUserId(orderCreateDTO.getUserId());
            order.setTotalAmount(orderCreateDTO.getTotalAmount());
            order.setPayAmount(orderCreateDTO.getPayAmount() != null ?
                    orderCreateDTO.getPayAmount() : orderCreateDTO.getTotalAmount());
            order.setStatus(0); // 待支付状态
            order.setAddressId(orderCreateDTO.getAddressId());

            // 保存订单
            this.save(order);

            // 转换为DTO返回
            return orderConverter.toDTO(order);
        } catch (Exception e) {
            log.error("创建订单失败: ", e);
            throw new OrderServiceException("创建订单失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Boolean payOrder(Long orderId, String currentUserId) {
        return payOrder(orderId);
    }

    @Override
    public Boolean shipOrder(Long orderId, String currentUserId) {
        return shipOrder(orderId);
    }

    @Override
    public Boolean completeOrder(Long orderId, String currentUserId) {
        return completeOrder(orderId);
    }

    @Override
    public Boolean cancelOrder(Long orderId, String currentUserId) {
        return cancelOrder(orderId);
    }

    @Override
    public Boolean isOrderPaid(Long orderId) {
        Order order = this.baseMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        // 状态1表示已支付
        return order.getStatus() == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateOrderToPaid(PaymentSuccessEvent event) {
        try {
            Order order = this.baseMapper.selectById(event.getOrderId());
            if (order == null) {
                log.warn("订单不存在，无法更新支付状态，订单ID: {}", event.getOrderId());
                return false;
            }

            // 检查订单状态，只有待支付状态才能更新为已支付
            if (order.getStatus() != 0) {
                log.warn("订单状态不是待支付，无法更新支付状态，订单ID: {}, 当前状态: {}",
                        event.getOrderId(), order.getStatus());
                return false;
            }

            // 更新订单状态为已支付
            order.setStatus(1);
            order.setPayAmount(event.getPaymentAmount());

            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                log.info("订单支付状态更新成功，订单ID: {}, 支付金额: {}",
                        event.getOrderId(), event.getPaymentAmount());
            }

            return result;
        } catch (Exception e) {
            log.error("更新订单支付状态失败，订单ID: {}", event.getOrderId(), e);
            throw e;
        }
    }

    @Override
    public Boolean deleteOrder(Long id) {
        try {
            // 先从数据库中删除订单
            boolean removed = this.removeById(id);

            if (removed) {
                log.info("订单删除成功，订单ID: {}", id);
            } else {
                log.warn("订单删除失败，订单ID: {}", id);
            }

            return removed;
        } catch (Exception e) {
            log.error("删除订单异常，订单ID: {}", id, e);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "orderListCache", key = "'user:' + #userId", unless = "#result == null or #result.isEmpty()")
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        try {
            List<Order> orders = this.lambdaQuery()
                    .eq(Order::getUserId, userId)
                    .list();
            return orders.stream()
                    .map(orderConverter::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("根据用户ID查询订单失败，用户ID: {}", userId, e);
            throw new OrderServiceException("根据用户ID查询订单失败: " + e.getMessage());
        }
    }

    @Override
    public OrderDTO getOrderByOrderNo(String orderNo) {
        try {
            // 由于Order实体中没有orderNo字段，这里简化处理
            // 实际项目中应该首先在Order实体中添加orderNo字段
            log.warn("暂不支持根据订单号查询，订单号: {}", orderNo);
            return null;
        } catch (Exception e) {
            log.error("根据订单号查询订单失败，订单号: {}", orderNo, e);
            throw new OrderServiceException("根据订单号查询订单失败: " + e.getMessage());
        }
    }

    /**
     * 发布订单完成事件
     * 通知库存服务进行库存扣减
     */
    private void publishOrderCompletedEvent(Order order) {
        try {
            String traceId = UUID.randomUUID().toString().replace("-", "");

            // 构建订单完成事件
            OrderCompletedEvent event = OrderCompletedEvent.builder()
                    .orderId(order.getId())
                    .orderNo("ORDER_" + order.getId()) // 简化订单号生成
                    .userId(order.getUserId())
                    .userName("User_" + order.getUserId()) // 简化用户名
                    .totalAmount(order.getTotalAmount())
                    .payAmount(order.getPayAmount())
                    .orderStatus(3) // 已完成状态
                    .beforeStatus(2) // 之前是已发货状态
                    .afterStatus(3)  // 现在是已完成状态
                    .completedTime(LocalDateTime.now())
                    .operator("SYSTEM")
                    .traceId(traceId)
                    .build();

            // 发布事件
            orderEventProducer.sendOrderCompletedEvent(event);

            log.info("📨 订单完成事件发布成功 - 订单ID: {}, 追踪ID: {}", order.getId(), traceId);

        } catch (Exception e) {
            log.error("❌ 发布订单完成事件失败 - 订单ID: {}, 错误: {}", order.getId(), e.getMessage(), e);
            // 事件发布失败不应该影响订单状态更新的主流程
        }
    }

    // ================= Feign客户端接口方法实现 =================

    /**
     * 根据订单ID查询订单信息（Feign客户端接口）
     *
     * @param orderId 订单ID
     * @return 订单信息
     */
    @Override
    @Transactional(readOnly = true)
    public OrderVO getOrderByOrderIdForFeign(Long orderId) {
        try {
            log.debug("[订单服务] 开始处理Feign调用：根据订单ID查询订单信息，订单ID: {}", orderId);
            
            // 直接从数据库查询
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                log.warn("[订单服务] 订单不存在，订单ID: {}", orderId);
                return null;
            }

            OrderVO orderVO = orderConverter.toVO(order);
            log.debug("[订单服务] 根据订单ID查询订单信息成功，订单ID: {}", orderId);
            return orderVO;
        } catch (Exception e) {
            log.error("[订单服务] 根据订单ID查询订单信息异常，订单ID: {}", orderId, e);
            return null;
        }
    }

    /**
     * 创建订单（Feign客户端接口）
     *
     * @param orderDTO 订单信息
     * @return 订单信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrderForFeign(OrderDTO orderDTO) {
        try {
            log.info("[订单服务] 开始处理Feign调用：创建订单，用户ID: {}", orderDTO.getUserId());
            
            // 创建订单实体
            Order order = orderConverter.toEntity(orderDTO);
            order.setStatus(0); // 设置为待支付状态
            
            boolean saved = this.save(order);
            if (!saved) {
                log.error("[订单服务] 创建订单失败");
                return null;
            }
            
            OrderVO orderVO = orderConverter.toVO(order);
            log.info("[订单服务] 创建订单成功，订单ID: {}", order.getId());
            return orderVO;
        } catch (Exception e) {
            log.error("[订单服务] 创建订单异常", e);
            return null;
        }
    }

    /**
     * 更新订单状态（Feign客户端接口）
     *
     * @param orderId 订单ID
     * @param status  订单状态
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateOrderStatusForFeign(Long orderId, Integer status) {
        try {
            log.info("[订单服务] 开始处理Feign调用：更新订单状态，订单ID: {}，状态: {}", orderId, status);
            
            // 查询订单
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                log.warn("[订单服务] 订单不存在，订单ID: {}", orderId);
                return false;
            }
            
            // 更新状态
            order.setStatus(status);
            boolean updated = this.updateById(order);
            
            if (updated) {
                log.info("[订单服务] 更新订单状态成功，订单ID: {}，新状态: {}", orderId, status);
            } else {
                log.warn("[订单服务] 更新订单状态失败，订单ID: {}，状态: {}", orderId, status);
            }
            
            return updated;
        } catch (Exception e) {
            log.error("[订单服务] 更新订单状态异常，订单ID: {}，状态: {}", orderId, status, e);
            return false;
        }
    }

    /**
     * 完成订单（Feign客户端接口）
     *
     * @param orderId 订单ID
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean completeOrderForFeign(Long orderId) {
        try {
            log.info("[订单服务] 开始处理Feign调用：完成订单，订单ID: {}", orderId);
            
            // 查询订单
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                log.warn("[订单服务] 订单不存在，订单ID: {}", orderId);
                return false;
            }
            
            // 验证订单状态（必须是已发货状态）
            if (order.getStatus() != 2) {
                log.warn("[订单服务] 订单状态不正确，无法完成订单，订单ID: {}，当前状态: {}", orderId, order.getStatus());
                return false;
            }
            
            // 更新状态为已完成
            order.setStatus(3);
            boolean updated = this.updateById(order);
            
            if (updated) {
                log.info("[订单服务] 完成订单成功，订单ID: {}", orderId);
                // 发布订单完成事件
                publishOrderCompletedEvent(order);
            } else {
                log.warn("[订单服务] 完成订单失败，订单ID: {}", orderId);
            }
            
            return updated;
        } catch (Exception e) {
            log.error("[订单服务] 完成订单异常，订单ID: {}", orderId, e);
            return false;
        }
    }
}
