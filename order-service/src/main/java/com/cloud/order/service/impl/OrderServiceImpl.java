package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.order.OrderVO;
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

import java.util.List;
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
    public Boolean cancelOrder(Long orderId) {
        return null;
    }

    @Override
    public Boolean createOrder(OrderCreateDTO orderCreateDTO, String currentUserId) {
        return null;
    }

    @Override
    public Boolean payOrder(Long orderId, String currentUserId) {
        return null;
    }

    @Override
    public Boolean shipOrder(Long orderId, String currentUserId) {
        return null;
    }

    @Override
    public Boolean completeOrder(Long orderId, String currentUserId) {
        return null;
    }

    @Override
    public Boolean cancelOrder(Long orderId, String currentUserId) {
        return null;
    }

    @Override
    public Boolean deleteOrder(Long id) {
        return null;
    }

    @Override
    public OrderDTO createOrder(OrderDTO orderDTO) {
        return null;
    }

    @Override
    public OrderDTO createOrder(OrderCreateDTO orderCreateDTO) {
        return null;
    }

    @Override
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        return List.of();
    }

    @Override
    public OrderDTO getOrderByOrderNo(String orderNo) {
        return null;
    }

    @Override
    public Boolean isOrderPaid(Long orderId) {
        return null;
    }

    @Override
    public OrderVO getOrderByOrderIdForFeign(Long orderId) {
        return null;
    }

    @Override
    public OrderVO createOrderForFeign(OrderDTO orderDTO) {
        return null;
    }

    @Override
    public Boolean updateOrderStatusForFeign(Long orderId, Integer status) {
        return null;
    }

    @Override
    public Boolean completeOrderForFeign(Long orderId) {
        return null;
    }
}
