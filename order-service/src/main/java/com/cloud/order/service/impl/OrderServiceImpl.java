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
            
            Page<OrderVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
            voPage.setRecords(resultPage.getRecords().stream()
                    .map(orderConverter::toVO)
                    .collect(Collectors.toList()));
            return voPage;
        } catch (Exception e) {
            log.error("鍒嗛〉鏌ヨ璁㈠崟澶辫触: ", e);
            throw new OrderServiceException("鍒嗛〉鏌ヨ璁㈠崟澶辫触: " + e.getMessage());
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
            
            Order order = this.baseMapper.selectById(id);
            if (order == null) {
                throw EntityNotFoundException.order(id);
            }

            OrderDTO orderDTO = orderConverter.toDTO(order);
            return orderDTO;
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("鏍规嵁ID鏌ヨ璁㈠崟澶辫触: ", e);
            throw new OrderServiceException("鏍规嵁ID鏌ヨ璁㈠崟澶辫触: " + e.getMessage());
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
                
            }

            return result;
        } catch (Exception e) {
            log.error("淇濆瓨璁㈠崟澶辫触: ", e);
            throw new OrderServiceException("淇濆瓨璁㈠崟澶辫触: " + e.getMessage());
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
            
            Order existingOrder = this.baseMapper.selectById(orderDTO.getId());
            if (existingOrder == null) {
                throw EntityNotFoundException.order(orderDTO.getId());
            }

            
            Order order = orderConverter.toEntity(orderDTO);
            boolean result = this.baseMapper.updateById(order) > 0;

            if (result) {
                
            }

            return result;
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("鏇存柊璁㈠崟澶辫触: ", e);
            throw e;
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'order:pay:' + #orderId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "璁㈠崟鏀粯鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Transactional(rollbackFor = Exception.class)
    public Boolean payOrder(Long orderId) {
        try {
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            
            if (order.getStatus() != 0) {
                throw InvalidStatusException.order(order.getStatus().toString(), "鏀粯");
            }
            order.setStatus(1); 

            return result;
        } catch (EntityNotFoundException | InvalidStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("鏀粯璁㈠崟澶辫触: ", e);
            throw e;
        }
    }

    @Override
    @PreAuthorize("@permissionChecker.checkPermission(authentication, 'order:manage') or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'order:ship:' + #orderId",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "璁㈠崟鍙戣揣鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Transactional(rollbackFor = Exception.class)
    public Boolean shipOrder(Long orderId) {
        try {
            Order order = this.baseMapper.selectById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            
            if (order.getStatus() != 1) {
                throw InvalidStatusException.order(order.getStatus().toString(), "鍙戣揣");
            }
            order.setStatus(2); 

            return result;
        } catch (EntityNotFoundException | InvalidStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("鍙戣揣璁㈠崟澶辫触: ", e);
            throw e;
        }
    }

    @Override
    @DistributedLock(
            key = "'order:complete:' + #orderId",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "璁㈠崟瀹屾垚鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Transactional(rollbackFor = Exception.class)
    public Boolean completeOrder(Long orderId) {

        Order order = this.baseMapper.selectById(orderId);
        if (order == null) {
            throw EntityNotFoundException.order(orderId);
        }

        
        if (order.getStatus() != 2) {
            throw InvalidStatusException.order(order.getStatus().toString(), "瀹屾垚");
        }
        order.setStatus(3); 

        
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
            throw InvalidStatusException.order(String.valueOf(order.getStatus()), "鍙栨秷");
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
                    throw new OrderServiceException("褰撳墠鐢ㄦ埛涓庤鍗曠敤鎴蜂笉涓€鑷?);
                }
            } catch (NumberFormatException e) {
                throw new OrderServiceException("褰撳墠鐢ㄦ埛ID鏍煎紡閿欒");
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
            throw new OrderServiceException("璁㈠崟閲戦蹇呴』澶т簬0");
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
            throw new BusinessException("鍒涘缓璁㈠崟澶辫触");
        }
        Order persisted = this.baseMapper.selectById(created.getId());
        if (persisted == null) {
            throw new EntityNotFoundException("璁㈠崟", created.getId());
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

            
            if (order.getStatus() != 0) {
                log.warn("璁㈠崟鐘舵€佷笉姝ｇ‘锛屾棤娉曟洿鏂版敮浠樼姸鎬? orderId={}, currentStatus={}", orderId, order.getStatus());
                return false;
            }

            order.setStatus(1); 

            if (result) {
                

            }

            return result;
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("鏀粯鎴愬姛鍚庢洿鏂拌鍗曠姸鎬佸け璐? orderId={}", orderId, e);
            throw new OrderServiceException("鏀粯鎴愬姛鍚庢洿鏂拌鍗曠姸鎬佸け璐? " + e.getMessage());
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

            
            if (order.getStatus() != 0) {
                log.warn("璁㈠崟鐘舵€佷笉姝ｇ‘锛屾棤娉曞彇娑? orderId={}, currentStatus={}", orderId, order.getStatus());
                return false;
            }

            order.setStatus(4); 

            if (result) {
                
            }

            return result;
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("搴撳瓨鍐荤粨澶辫触鍙栨秷璁㈠崟澶辫触: orderId={}", orderId, e);
            throw new OrderServiceException("搴撳瓨鍐荤粨澶辫触鍙栨秷璁㈠崟澶辫触: " + e.getMessage());
        }
    }

    

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
            log.warn("鎵归噺鏇存柊璁㈠崟鐘舵€佸け璐ワ紝璁㈠崟ID闆嗗悎涓虹┖");
            throw new OrderServiceException("璁㈠崟ID闆嗗悎涓嶈兘涓虹┖");
        }

        if (status == null) {
            log.warn("鎵归噺鏇存柊璁㈠崟鐘舵€佸け璐ワ紝鐘舵€佸€间负绌?);
            throw new OrderServiceException("鐘舵€佸€间笉鑳戒负绌?);
        }

        

        try {
            
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            wrapper.in(Order::getId, orderIds);

            Order updateEntity = new Order();
            updateEntity.setStatus(status);

            boolean result = this.update(updateEntity, wrapper);

            if (result) {
                
                return orderIds.size();
            } else {
                log.warn("鎵归噺鏇存柊璁㈠崟鐘舵€佸け璐?);
                return 0;
            }
        } catch (Exception e) {
            log.error("鎵归噺鏇存柊璁㈠崟鐘舵€佹椂鍙戠敓寮傚父锛岃鍗旾Ds: {}", orderIds, e);
            throw new OrderServiceException("鎵归噺鏇存柊璁㈠崟鐘舵€佸け璐? " + e.getMessage(), e);
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
            log.warn("鎵归噺鍒犻櫎璁㈠崟澶辫触锛岃鍗旾D闆嗗悎涓虹┖");
            throw new OrderServiceException("璁㈠崟ID闆嗗悎涓嶈兘涓虹┖");
        }

        

        try {
            

            if (result) {
                
                return orderIds.size();
            } else {
                log.warn("鎵归噺鍒犻櫎璁㈠崟澶辫触");
                return 0;
            }
        } catch (Exception e) {
            log.error("鎵归噺鍒犻櫎璁㈠崟鏃跺彂鐢熷紓甯革紝璁㈠崟IDs: {}", orderIds, e);
            throw new OrderServiceException("鎵归噺鍒犻櫎璁㈠崟澶辫触: " + e.getMessage(), e);
        }
    }

    private void validateOrderUser(Long orderId, String currentUserId) {
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new OrderServiceException("褰撳墠鐢ㄦ埛ID涓嶈兘涓虹┖");
        }
        Order order = this.baseMapper.selectById(orderId);
        if (order == null) {
            throw EntityNotFoundException.order(orderId);
        }
        try {
            Long currentUser = Long.parseLong(currentUserId);
            if (!Objects.equals(order.getUserId(), currentUser)) {
                throw new OrderServiceException("鏃犳潈鎿嶄綔璇ヨ鍗?);
            }
        } catch (NumberFormatException e) {
            throw new OrderServiceException("褰撳墠鐢ㄦ埛ID鏍煎紡閿欒");
        }
    }
}
