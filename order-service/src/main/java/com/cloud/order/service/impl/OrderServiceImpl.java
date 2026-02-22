package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.order.OrderVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.exception.InvalidStatusException;
import com.cloud.order.converter.OrderConverter;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.order.exception.OrderServiceException;
import com.cloud.order.mapper.OrderMapper;
import com.cloud.order.module.entity.Order;
import com.cloud.order.service.OrderItemService;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
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

        log.info("订单完成成功，订单ID: {}", orderId);
        return result;

    }

    @Override
    @DistributedLock(
            key = "'order:cancel:' + #orderId",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire order cancel lock failed"
    )
    public Boolean cancelOrder(Long orderId) {
        Order order = this.baseMapper.selectById(orderId);
        if (order == null) {
            throw EntityNotFoundException.order(orderId);
        }
        if (order.getStatus() == null || (order.getStatus() != 0 && order.getStatus() != 1)) {
            throw InvalidStatusException.order(String.valueOf(order.getStatus()), "取消");
        }
        order.setStatus(4);
        order.setCancelTime(LocalDateTime.now());
        return this.baseMapper.updateById(order) > 0;
    }

    @Override
    public Boolean createOrder(OrderCreateDTO orderCreateDTO, String currentUserId) {
        if (currentUserId != null && !currentUserId.isBlank()) {
            try {
                Long currentUser = Long.parseLong(currentUserId);
                if (!Objects.equals(currentUser, orderCreateDTO.getUserId())) {
                    throw new OrderServiceException("当前用户与订单用户不一致");
                }
            } catch (NumberFormatException e) {
                throw new OrderServiceException("当前用户ID格式错误");
            }
        }
        OrderDTO created = createOrder(orderCreateDTO);
        return created != null && created.getId() != null;
    }

    @Override
    public Boolean payOrder(Long orderId, String currentUserId) {
        validateOrderUser(orderId, currentUserId);
        return payOrder(orderId);
    }

    @Override
    public Boolean shipOrder(Long orderId, String currentUserId) {
        return shipOrder(orderId);
    }

    @Override
    public Boolean completeOrder(Long orderId, String currentUserId) {
        validateOrderUser(orderId, currentUserId);
        return completeOrder(orderId);
    }

    @Override
    public Boolean cancelOrder(Long orderId, String currentUserId) {
        validateOrderUser(orderId, currentUserId);
        return cancelOrder(orderId);
    }

    @Override
    @DistributedLock(
            key = "'order:delete:' + #id",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "Acquire order delete lock failed"
    )
    public Boolean deleteOrder(Long id) {
        return this.baseMapper.deleteById(id) > 0;
    }

    @Override
    public OrderDTO createOrder(OrderDTO orderDTO) {
        Order order = orderConverter.toEntity(orderDTO);
        if (order.getOrderNo() == null || order.getOrderNo().isBlank()) {
            order.setOrderNo(Order.generateOrderNo());
        }
        if (order.getStatus() == null) {
            order.setStatus(0);
        }
        this.baseMapper.insert(order);
        return orderConverter.toDTO(order);
    }

    @Override
    public OrderDTO createOrder(OrderCreateDTO orderCreateDTO) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderNo(Order.generateOrderNo());
        orderDTO.setUserId(orderCreateDTO.getUserId());
        orderDTO.setAddressId(orderCreateDTO.getAddressId());
        BigDecimal totalAmount = orderCreateDTO.getTotalAmount() == null ? BigDecimal.ZERO : orderCreateDTO.getTotalAmount();
        BigDecimal payAmount = orderCreateDTO.getPayAmount() == null ? totalAmount : orderCreateDTO.getPayAmount();
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0 || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderServiceException("订单金额必须大于0");
        }
        orderDTO.setTotalAmount(totalAmount);
        orderDTO.setPayAmount(payAmount);
        orderDTO.setStatus(0);
        orderDTO.setRemark(orderCreateDTO.getRemark());
        return createOrder(orderDTO);
    }

    @Override
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        List<Order> orders = this.baseMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreatedAt)
        );
        return orderConverter.toDTOList(orders);
    }

    @Override
    public OrderDTO getOrderByOrderNo(String orderNo) {
        Order order = this.baseMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );
        return order == null ? null : orderConverter.toDTO(order);
    }

    @Override
    public Boolean isOrderPaid(Long orderId) {
        Order order = this.baseMapper.selectById(orderId);
        if (order == null || order.getStatus() == null) {
            return false;
        }
        return order.getStatus() == 1 || order.getStatus() == 2 || order.getStatus() == 3;
    }

    @Override
    public OrderVO getOrderByOrderIdForFeign(Long orderId) {
        Order order = this.baseMapper.selectById(orderId);
        return order == null ? null : orderConverter.toVO(order);
    }

    @Override
    public OrderVO createOrderForFeign(OrderDTO orderDTO) {
        OrderDTO created = createOrder(orderDTO);
        if (created == null || created.getId() == null) {
            throw new BusinessException("创建订单失败");
        }
        Order persisted = this.baseMapper.selectById(created.getId());
        if (persisted == null) {
            throw new EntityNotFoundException("订单", created.getId());
        }
        return orderConverter.toVO(persisted);
    }

    @Override
    public Boolean updateOrderStatusForFeign(Long orderId, Integer status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case 1 -> payOrder(orderId);
            case 2 -> shipOrder(orderId);
            case 3 -> completeOrder(orderId);
            case 4 -> cancelOrder(orderId);
            default -> false;
        };
    }

    @Override
    public Boolean completeOrderForFeign(Long orderId) {
        return completeOrder(orderId);
    }

    @Override
    @DistributedLock(
            key = "'order:payment:callback:' + #orderId",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire order payment callback lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "orderCache", key = "#orderId")
    public Boolean updateOrderStatusAfterPayment(Long orderId, Long paymentId, String transactionNo) {
        try {
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            // 验证订单状态（必须是待支付状态）
            if (order.getStatus() != 0) {
                log.warn("订单状态不正确，无法更新支付状态: orderId={}, currentStatus={}", orderId, order.getStatus());
                return false;
            }

            order.setStatus(1); // 设置为已支付状态
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                log.info("支付成功后更新订单状态成功: orderId={}, paymentId={}, transactionNo={}",
                        orderId, paymentId, transactionNo);
            }

            return result;
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付成功后更新订单状态失败: orderId={}", orderId, e);
            throw new OrderServiceException("支付成功后更新订单状态失败: " + e.getMessage());
        }
    }

    @Override
    @DistributedLock(
            key = "'order:stock:freeze:failed:' + #orderId",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire order stock freeze failed lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "orderCache", key = "#orderId")
    public Boolean cancelOrderDueToStockFreezeFailed(Long orderId, String reason) {
        try {
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            // 验证订单状态（只有待支付状态可以取消）
            if (order.getStatus() != 0) {
                log.warn("订单状态不正确，无法取消: orderId={}, currentStatus={}", orderId, order.getStatus());
                return false;
            }

            order.setStatus(4); // 设置为已取消状态
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                log.info("库存冻结失败取消订单成功: orderId={}, reason={}", orderId, reason);
            }

            return result;
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("库存冻结失败取消订单失败: orderId={}", orderId, e);
            throw new OrderServiceException("库存冻结失败取消订单失败: " + e.getMessage());
        }
    }

    // ================= 批量操作方法实现 =================

    @Override
    @DistributedLock(
            key = "'order:batch:update:' + #orderIds.toString()",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "Acquire order batch update lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "orderCache", allEntries = true)
    public Integer batchUpdateOrderStatus(List<Long> orderIds, Integer status) {
        if (orderIds == null || orderIds.isEmpty()) {
            log.warn("批量更新订单状态失败，订单ID集合为空");
            throw new OrderServiceException("订单ID集合不能为空");
        }

        if (status == null) {
            log.warn("批量更新订单状态失败，状态值为空");
            throw new OrderServiceException("状态值不能为空");
        }

        log.info("开始批量更新订单状态，订单数量: {}, 状态值: {}", orderIds.size(), status);

        try {
            // 使用 MyBatis Plus 的 lambdaUpdate 批量更新
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            wrapper.in(Order::getId, orderIds);

            Order updateEntity = new Order();
            updateEntity.setStatus(status);

            boolean result = this.update(updateEntity, wrapper);

            if (result) {
                log.info("批量更新订单状态成功，订单数量: {}", orderIds.size());
                return orderIds.size();
            } else {
                log.warn("批量更新订单状态失败");
                return 0;
            }
        } catch (Exception e) {
            log.error("批量更新订单状态时发生异常，订单IDs: {}", orderIds, e);
            throw new OrderServiceException("批量更新订单状态失败: " + e.getMessage(), e);
        }
    }

    @Override
    @DistributedLock(
            key = "'order:batch:delete:' + #orderIds.toString()",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "Acquire order batch delete lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "orderCache", allEntries = true)
    public Integer batchDeleteOrders(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            log.warn("批量删除订单失败，订单ID集合为空");
            throw new OrderServiceException("订单ID集合不能为空");
        }

        log.info("开始批量删除订单，订单数量: {}", orderIds.size());

        try {
            // 使用 MyBatis Plus 的批量删除（逻辑删除）
            boolean result = this.removeByIds(orderIds);

            if (result) {
                log.info("批量删除订单成功，订单数量: {}", orderIds.size());
                return orderIds.size();
            } else {
                log.warn("批量删除订单失败");
                return 0;
            }
        } catch (Exception e) {
            log.error("批量删除订单时发生异常，订单IDs: {}", orderIds, e);
            throw new OrderServiceException("批量删除订单失败: " + e.getMessage(), e);
        }
    }

    private void validateOrderUser(Long orderId, String currentUserId) {
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new OrderServiceException("当前用户ID不能为空");
        }
        Order order = this.baseMapper.selectById(orderId);
        if (order == null) {
            throw EntityNotFoundException.order(orderId);
        }
        try {
            Long currentUser = Long.parseLong(currentUserId);
            if (!Objects.equals(order.getUserId(), currentUser)) {
                throw new OrderServiceException("无权操作该订单");
            }
        } catch (NumberFormatException e) {
            throw new OrderServiceException("当前用户ID格式错误");
        }
    }
}
