package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.order.OrderCreateDTO;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.OrderVO;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.order.converter.OrderConverter;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.order.exception.OrderServiceException;
import com.cloud.order.exception.OrderStatusException;
import com.cloud.order.mapper.OrderMapper;
import com.cloud.order.module.entity.Order;
import com.cloud.order.module.entity.OrderItem;
import com.cloud.order.service.OrderItemService;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    @Transactional(rollbackFor = Exception.class)
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
            order.setStatus(1); // 设置为已支付状态
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                log.info("订单支付成功，订单ID: {}", orderId);
            }
            return result;
        } catch (EntityNotFoundException | OrderStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付订单失败: ", e);
            throw e;
        }
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
            order.setStatus(2); // 设置为已发货状态
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                log.info("订单发货成功，订单ID: {}", orderId);
            }
            return result;
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
            order.setStatus(3); // 设置为已完成状态
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
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
            order.setStatus(-1); // 设置为已取消状态
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                log.info("订单取消成功，订单ID: {}", orderId);
            }
            return result;
        } catch (EntityNotFoundException | OrderStatusException e) {
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
}
