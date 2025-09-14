package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.common.domain.vo.OrderVO;
import com.cloud.order.converter.OrderConverter;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.order.exception.OrderServiceException;
import com.cloud.order.exception.OrderStatusException;
import com.cloud.order.mapper.OrderMapper;
import com.cloud.order.module.entity.Order;
import com.cloud.order.module.entity.OrderItem;
import com.cloud.order.service.CacheService;
import com.cloud.order.service.OrderItemService;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    // 缓存键前缀
    private static final String ORDER_CACHE_KEY_PREFIX = "order:";
    // 缓存过期时间（分钟）
    private static final long ORDER_CACHE_EXPIRE_MINUTES = 30;
    private final OrderConverter orderConverter;
    private final CacheService cacheService;
    private final OrderItemService orderItemService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Page<OrderVO> pageQuery(OrderPageQueryDTO queryDTO) {
        try {
            Page<Order> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
            Page<Order> resultPage = this.baseMapper.pageQuery(page, queryDTO);
            // 转换为VO对象
            Page<OrderVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
            voPage.setRecords(resultPage.getRecords().stream()
                    .map(orderConverter::toVO)
                    .toList());
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
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO getByOrderEntityId(Long id) {
        try {
            // 先从缓存中获取
            String cacheKey = ORDER_CACHE_KEY_PREFIX + "dto:" + id;
            OrderDTO cachedOrder = cacheService.get(cacheKey);
            if (cachedOrder != null) {
                log.debug("从缓存中获取订单DTO信息，订单ID: {}", id);
                return cachedOrder;
            }

            // 缓存中没有则从数据库查询
            Order order = this.baseMapper.selectById(id);
            if (order == null) {
                throw EntityNotFoundException.order(id);
            }

            OrderDTO orderDTO = orderConverter.toDTO(order);

            // 将查询结果放入缓存
            cacheService.set(cacheKey, orderDTO, ORDER_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.debug("将订单DTO信息放入缓存，订单ID: {}", id);

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
    public Boolean saveOrder(OrderDTO orderDTO) {
        try {
            Order order = orderConverter.toEntity(orderDTO);
            boolean result = this.baseMapper.insert(order) > 0;

            if (result) {
                // 清除相关缓存
                clearOrderCache(order.getId());

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
                // 清除相关缓存
                clearOrderCache(order.getId());
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
    @Transactional(rollbackFor = Exception.class)
    public Boolean payOrder(Long orderId) {
        try {
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            // 验证订单状态（必须是待支付状态）
            if (order.getStatus() != 0) {
                throw new OrderStatusException(orderId, order.getStatus(), "支付");
            }
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                // 清除相关缓存
                clearOrderCache(orderId);
                log.info("订单支付成功，订单ID: {}", orderId);
            }

        } catch (EntityNotFoundException | OrderStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付订单失败: ", e);
            throw e;
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean shipOrder(Long orderId) {
        try {
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            // 验证订单状态（必须是已支付状态）
            if (order.getStatus() != 1) {
                throw new OrderStatusException(orderId, order.getStatus(), "发货");
            }


            return true;
        } catch (EntityNotFoundException | OrderStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("发货订单失败: ", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean completeOrder(Long orderId) {
        try {
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            // 验证订单状态（必须是已发货状态）
            if (order.getStatus() != 2) {
                throw new OrderStatusException(orderId, order.getStatus(), "完成");
            }
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                // 清除相关缓存
                clearOrderCache(orderId);
                log.info("订单完成成功，订单ID: {}", orderId);
            }

            return result;
        } catch (EntityNotFoundException | OrderStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("完成订单失败: ", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelOrder(Long orderId) {
        try {
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            // 验证订单状态（必须是待支付或已支付状态）
            if (order.getStatus() != 0 && order.getStatus() != 1) {
                throw new OrderStatusException(orderId, order.getStatus(), "取消");
            }
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                // 清除相关缓存
                clearOrderCache(orderId);

                log.info("订单取消成功，订单ID: {}", orderId);
            }

        } catch (EntityNotFoundException | OrderStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("取消订单失败: ", e);
            throw e;
        }
        return true;
    }

    /**
     * 创建订单
     *
     * @param orderCreateDTO 订单创建信息
     * @param currentUserId  当前用户ID
     * @return 订单信息
     */
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
            // 清除订单相关缓存
            assert order != null;
            if (order.getId() != null) {
                clearOrderCache(order.getId());
            }
        }
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

    /**
     * 支付订单
     *
     * @param orderId 订单ID
     * @Override public Boolean deleteOrder(Long id) {
     * try {
     * // 先从数据库中删除订单
     * boolean removed = this.removeById(id);
     * <p>
     * if (removed) {
     * // 清除相关缓存
     * clearOrderCache(id);
     * log.info("订单删除成功，订单ID: {}", id);
     * } else {
     * log.warn("订单删除失败，订单ID: {}", id);
     * }
     * <p>
     * return removed;
     * } catch (Exception e) {
     * log.error("删除订单异常，订单ID: {}", id, e);
     * return false;
     * }
     * }
     * <p>
     * /**
     * 清除订单相关缓存
     */
    private void clearOrderCache(Long orderId) {
        try {
            // 清除订单VO缓存
            String voCacheKey = ORDER_CACHE_KEY_PREFIX + orderId;
            cacheService.delete(voCacheKey);

            // 清除订单DTO缓存
            String dtoCacheKey = ORDER_CACHE_KEY_PREFIX + "dto:" + orderId;
            cacheService.delete(dtoCacheKey);

            log.debug("清除订单缓存，订单ID: {}", orderId);
        } catch (Exception e) {
            log.warn("清除订单缓存失败，订单ID: {}", orderId, e);
        }
    }
}