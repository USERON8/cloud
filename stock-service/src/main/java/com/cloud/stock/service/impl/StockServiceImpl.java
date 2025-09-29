package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.event.order.OrderCompletedEvent;
import com.cloud.common.domain.event.order.OrderCreatedEvent;
import com.cloud.common.domain.event.stock.StockConfirmEvent;
import com.cloud.common.domain.event.stock.StockReserveEvent;
import com.cloud.common.domain.event.stock.StockRollbackEvent;
import com.cloud.common.domain.vo.stock.StockVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.common.utils.PageUtils;


import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.exception.StockFrozenException;
import com.cloud.stock.exception.StockInsufficientException;
import com.cloud.stock.exception.StockOperationException;
import com.cloud.stock.mapper.StockInMapper;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.mapper.StockOutMapper;
import com.cloud.common.messaging.BusinessLogProducer;
import com.cloud.common.messaging.AsyncLogProducer;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.module.entity.StockIn;
import com.cloud.stock.module.entity.StockOut;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import com.cloud.common.utils.UserContextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

/**
 * 库存服务实现类
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    private final StockMapper stockMapper;
    private final StockInMapper stockInMapper;
    private final StockOutMapper stockOutMapper;
    private final StockConverter stockConverter;
    private final BusinessLogProducer businessLogProducer;
    private final AsyncLogProducer asyncLogProducer;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "stockCache",
            key = "#id",
            unless = "#result == null"
    )
    public StockDTO getStockById(Long id) {
        if (id == null) {
            log.warn("库存ID不能为空");
            throw new IllegalArgumentException("库存ID不能为空");
        }

        try {
            log.info("根据ID查找库存: {}", id);
            Stock stock = getById(id);
            if (stock == null) {
                throw EntityNotFoundException.stock(id);
            }
            return stockConverter.toDTO(stock);
        } catch (Exception e) {
            log.error("根据ID查找库存失败，库存ID: {}", id, e);
            throw new BusinessException("获取库存信息失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "stockCache",
            key = "'product:' + #productId",
            unless = "#result == null"
    )
    public StockDTO getStockByProductId(Long productId) {
        if (productId == null) {
            log.warn("商品ID不能为空");
            throw new IllegalArgumentException("商品ID不能为空");
        }

        try {
            log.info("根据商品ID查找库存: {}", productId);
            Stock stock = getOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getProductId, productId));
            return stock != null ? stockConverter.toDTO(stock) : null;
        } catch (Exception e) {
            log.error("根据商品ID查找库存失败，商品ID: {}", productId, e);
            throw new BusinessException("获取库存信息失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "stockCache",
            key = "'batch:' + #productIds.toString()",
            condition = "#productIds != null && #productIds.size() <= 100",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<StockDTO> getStocksByProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<Stock> stocks = list(new LambdaQueryWrapper<Stock>()
                .in(Stock::getProductId, productIds));
        return stockConverter.toDTOList(stocks);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StockVO> pageQuery(StockPageDTO pageDTO) {
        try {
            log.info("分页查询库存，查询条件：{}", pageDTO);

            // 1. 构造分页对象
            Page<Stock> page = PageUtils.buildPage(pageDTO);

            // 2. 构造查询条件
            LambdaQueryWrapper<Stock> queryWrapper = new LambdaQueryWrapper<>();
            if (pageDTO.getProductId() != null) {
                queryWrapper.eq(Stock::getProductId, pageDTO.getProductId());
            }
            if (StringUtils.isNotBlank(pageDTO.getProductName())) {
                queryWrapper.like(Stock::getProductName, pageDTO.getProductName());
            }
            if (pageDTO.getStockStatus() != null) {
                queryWrapper.eq(Stock::getStockStatus, pageDTO.getStockStatus());
            }
            queryWrapper.orderByDesc(Stock::getCreatedAt);

            // 3. 执行分页查询
            Page<Stock> resultPage = this.page(page, queryWrapper);

            // 4. 转换实体列表为VO列表
            List<StockVO> stockVOList = stockConverter.toVOList(resultPage.getRecords());

            // 5. 封装分页结果
            PageResult<StockVO> pageResult = PageResult.of(
                    resultPage.getCurrent(),
                    resultPage.getSize(),
                    resultPage.getTotal(),
                    stockVOList
            );

            log.info("分页查询完成，总记录数：{}，当前页：{}，每页大小：{}",
                    pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());

            return pageResult;
        } catch (Exception e) {
            log.error("分页查询库存时发生异常，查询条件：{}", pageDTO, e);
            throw new BusinessException("分页查询库存失败");
        }
    }


    @Override
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "stockCache",
            key = "#id"
    )
    public boolean deleteStock(Long id) {
        log.info("删除库存信息，ID：{}", id);

        try {
            Stock stock = getById(id);
            if (stock == null) {
                throw EntityNotFoundException.stock(id);
            }

            boolean result = removeById(id);

            log.info("库存信息删除成功，ID：{}", id);
            return result;
        } catch (Exception e) {
            log.error("删除库存信息失败，ID：{}", id, e);
            throw new BusinessException("删除库存信息失败", e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:batch:delete:' + T(String).join(',', #ids)",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "批量删除库存操作获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "stockCache",
            allEntries = true,
            condition = "#ids != null && !#ids.isEmpty()"
    )
    public boolean deleteStocksByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        log.info("批量删除库存信息，数量：{}", ids.size());

        try {
            boolean result = removeByIds(ids);
            log.info("批量删除库存信息成功，数量：{}", ids.size());
            return result;
        } catch (Exception e) {
            log.error("批量删除库存信息失败，IDs：{}", ids, e);
            throw new BusinessException("批量删除库存信息失败", e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:in:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "库存入库操作获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "stockCache",
            key = "'product:' + #productId"
    )
    public boolean stockIn(Long productId, Integer quantity, String remark) {
        log.info("库存入库，商品ID：{}，数量：{}", productId, quantity);

        try {
            Stock stock = getOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getProductId, productId));
            if (stock == null) {
                throw new EntityNotFoundException("库存信息不存在，商品ID: " + productId + "，无法执行入库操作");
            }

            // 更新库存数量
            int affected = stockMapper.updateStockQuantity(stock.getId(), quantity);
            if (affected == 0) {
                throw new StockOperationException("入库", productId, "库存更新失败，可能是并发冲突或数据已变更");
            }

            // 创建入库记录
            createStockInRecord(stock, quantity, remark);

            // 发送库存变更日志 - 使用统一业务日志系统
            try {
                Integer originalStock = stock.getStockQuantity() - quantity; // 计算原始库存
                asyncLogProducer.sendBusinessLogAsync(
                        "stock-service",
                        "STOCK_MANAGEMENT",
                        "STOCK_IN",
                        "库存入库操作",
                        productId.toString(),
                        "PRODUCT",
                        String.format("{\"stock\":%d,\"quantity\":%d}", originalStock, quantity),
                        String.format("{\"stock\":%d,\"quantity\":%d}", stock.getStockQuantity(), quantity),
                        UserContextUtils.getCurrentUsername() != null ? UserContextUtils.getCurrentUsername() : "SYSTEM",
                        "商品: " + productId + " 入库 " + quantity + " 件"
                );
            } catch (Exception e) {
                log.warn("发送库存入库日志失败，商品ID：{}", productId, e);
            }

            log.info("库存入库成功，商品ID：{}，数量：{}", productId, quantity);
            return true;
        } catch (Exception e) {
            log.error("库存入库失败，商品ID：{}，数量：{}", productId, quantity, e);
            throw new BusinessException("库存入库失败", e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:out:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "库存出库操作获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "stockCache",
            key = "'product:' + #productId"
    )
    public boolean stockOut(Long productId, Integer quantity, Long orderId, String orderNo, String remark) {
        log.info("库存出库，商品ID：{}，数量：{}，订单ID：{}", productId, quantity, orderId);

        try {
            Stock stock = getOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getProductId, productId));
            if (stock == null) {
                throw new EntityNotFoundException("库存信息不存在，商品ID: " + productId + "，无法执行出库操作");
            }

            // 计算可用库存
            int availableQuantity = stock.getStockQuantity() - stock.getFrozenQuantity();
            // 检查库存是否充足
            if (availableQuantity < quantity) {
                throw new StockInsufficientException(productId, quantity, availableQuantity);
            }

            // 更新库存数量
            int affected = stockMapper.updateStockQuantity(stock.getId(), -quantity);
            if (affected == 0) {
                throw new StockOperationException("出库", productId, "库存更新失败，可能是库存不足或并发冲突");
            }

            // 创建出库记录
            createStockOutRecord(stock, quantity, orderId, orderNo, remark);

            // 发送库存扣减日志 - 使用统一业务日志系统
            try {
                Integer originalStock = stock.getStockQuantity() + quantity; // 计算原始库存
                asyncLogProducer.sendBusinessLogAsync(
                        "stock-service",
                        "STOCK_MANAGEMENT",
                        "STOCK_OUT",
                        "库存出库操作",
                        productId.toString(),
                        "PRODUCT",
                        String.format("{\"stock\":%d,\"quantity\":%d,\"orderId\":%s}",
                                originalStock, quantity, orderId != null ? orderId.toString() : "null"),
                        String.format("{\"stock\":%d,\"quantity\":%d,\"orderId\":%s}",
                                stock.getStockQuantity(), quantity, orderId != null ? orderId.toString() : "null"),
                        UserContextUtils.getCurrentUsername() != null ? UserContextUtils.getCurrentUsername() : "SYSTEM",
                        "商品: " + productId + " 出库 " + quantity + " 件" +
                                (orderNo != null ? " (订单号: " + orderNo + ")" : "")
                );
            } catch (Exception e) {
                log.warn("发送库存出库日志失败，商品ID：{}", productId, e);
            }

            log.info("库存出库成功，商品ID：{}，数量：{}", productId, quantity);
            return true;
        } catch (Exception e) {
            log.error("库存出库失败，商品ID：{}，数量：{}", productId, quantity, e);
            throw new BusinessException("库存出库失败", e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:reserve:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "库存预留操作获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "stockCache",
            key = "'product:' + #productId"
    )
    public boolean reserveStock(Long productId, Integer quantity) {
        log.info("预留库存，商品ID：{}，数量：{}", productId, quantity);

        try {
            Stock stock = getOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getProductId, productId));
            if (stock == null) {
                throw new EntityNotFoundException("库存信息不存在，商品ID: " + productId + "，无法执行预留库存操作");
            }

            int affected = stockMapper.freezeStock(stock.getId(), quantity);
            if (affected == 0) {
                throw new StockFrozenException("预留", productId, quantity);
            }

            // 发送库存冻结日志 - 使用统一业务日志系统
            try {
                asyncLogProducer.sendBusinessLogAsync(
                        "stock-service",
                        "STOCK_MANAGEMENT",
                        "RESERVE",
                        "预留库存操作",
                        productId.toString(),
                        "PRODUCT",
                        String.format("{\"frozen\":%d,\"quantity\":%d}", stock.getFrozenQuantity() - quantity, quantity),
                        String.format("{\"frozen\":%d,\"quantity\":%d}", stock.getFrozenQuantity(), quantity),
                        UserContextUtils.getCurrentUsername() != null ? UserContextUtils.getCurrentUsername() : "SYSTEM",
                        "商品: " + productId + " 预留 " + quantity + " 件"
                );
            } catch (Exception e) {
                log.warn("发送库存冻结日志失败，商品ID：{}", productId, e);
            }

            log.info("预留库存成功，商品ID：{}，数量：{}", productId, quantity);
            return true;
        } catch (Exception e) {
            log.error("预留库存失败，商品ID：{}，数量：{}", productId, quantity, e);
            throw new BusinessException("预留库存失败", e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:release:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "库存释放预留操作获取锁失败"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "stockCache",
            key = "'product:' + #productId"
    )
    public boolean releaseReservedStock(Long productId, Integer quantity) {
        log.info("释放预留库存，商品ID：{}，数量：{}", productId, quantity);

        try {
            Stock stock = getOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getProductId, productId));
            if (stock == null) {
                throw new EntityNotFoundException("库存信息不存在，商品ID: " + productId + "，无法执行释放预留库存操作");
            }

            int affected = stockMapper.unfreezeStock(stock.getId(), quantity);
            if (affected == 0) {
                throw new StockFrozenException("释放", productId, quantity);
            }

            // 发送库存解冻日志 - 使用统一业务日志系统
            try {
                asyncLogProducer.sendBusinessLogAsync(
                        "stock-service",
                        "STOCK_MANAGEMENT",
                        "RELEASE",
                        "释放预留库存操作",
                        productId.toString(),
                        "PRODUCT",
                        String.format("{\"frozen\":%d,\"quantity\":%d}", stock.getFrozenQuantity() + quantity, quantity),
                        String.format("{\"frozen\":%d,\"quantity\":%d}", stock.getFrozenQuantity(), quantity),
                        UserContextUtils.getCurrentUsername() != null ? UserContextUtils.getCurrentUsername() : "SYSTEM",
                        "商品: " + productId + " 释放预留 " + quantity + " 件"
                );
            } catch (Exception e) {
                log.warn("发送库存解冻日志失败，商品ID：{}", productId, e);
            }

            log.info("释放预留库存成功，商品ID：{}，数量：{}", productId, quantity);
            return true;
        } catch (Exception e) {
            log.error("释放预留库存失败，商品ID：{}，数量：{}", productId, quantity, e);
            throw new BusinessException("释放预留库存失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkStockSufficient(Long productId, Integer quantity) {
        try {
            Stock stock = getOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getProductId, productId));
            if (stock == null) {
                return false;
            }
            // 计算可用库存
            int availableQuantity = stock.getStockQuantity() - stock.getFrozenQuantity();
            return availableQuantity >= quantity;
        } catch (Exception e) {
            log.error("检查库存是否充足时发生异常，商品ID：{}", productId, e);
            return false;
        }
    }

    /**
     * 创建入库记录
     */
    private void createStockInRecord(Stock stock, Integer quantity, String remark) {
        StockIn stockIn = new StockIn();
        stockIn.setProductId(stock.getProductId());
        stockIn.setQuantity(quantity);

        stockInMapper.insert(stockIn);
    }

    /**
     * 创建出库记录
     */
    private void createStockOutRecord(Stock stock, Integer quantity, Long orderId, String orderNo, String remark) {
        StockOut stockOut = new StockOut();
        stockOut.setProductId(stock.getProductId());
        stockOut.setOrderId(orderId);
        stockOut.setQuantity(quantity);

        stockOutMapper.insert(stockOut);
    }

    @Override
    public boolean isStockDeducted(Long orderId) {
        log.info("检查库存是否已扣减，订单ID：{}", orderId);
        try {
            // 查询出库记录表，检查是否已有该订单的出库记录
            long count = stockOutMapper.selectCount(new LambdaQueryWrapper<StockOut>()
                    .eq(StockOut::getOrderId, orderId));
            boolean deducted = count > 0;
            log.info("库存扣减检查结果，订单ID：{}，是否已扣减：{}", orderId, deducted);
            return deducted;
        } catch (Exception e) {
            log.error("检查库存是否已扣减失败，订单ID：{}", orderId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfreezeAndDeductStock(OrderCompletedEvent event) {
        log.info("解冻并扣减库存，订单ID：{}，商品数量：{}", event.getOrderId(),
                event.getOrderItems() != null ? event.getOrderItems().size() : 0);
        try {
            // 遍历库存扣减信息，解冻并扣减库存
            if (event.getOrderItems() != null) {
                for (OrderCompletedEvent.OrderItem item : event.getOrderItems()) {
                    Long productId = item.getProductId();
                    Integer quantity = item.getQuantity();

                    // 解冻库存
                    releaseReservedStock(productId, quantity);

                    // 扣减库存
                    stockOut(productId, quantity, event.getOrderId(), event.getOrderNo(), "订单完成扣减");
                }
            }
            log.info("解冻并扣减库存成功，订单ID：{}", event.getOrderId());
            return true;
        } catch (Exception e) {
            log.error("解冻并扣减库存失败，订单ID：{}", event.getOrderId(), e);
            return false;
        }
    }

    @Override
    public boolean isStockReserved(Long orderId) {
        log.info("检查库存是否已预留，订单ID：{}", orderId);
        try {
            // 这里可以通过查询预留记录表或者其他方式来判断
            // 简化实现：假设如果没有出库记录但有冻结库存，则认为库存已预留
            boolean reserved = isStockFrozen(orderId);
            log.info("库存预留检查结果，订单ID：{}，是否已预留：{}", orderId, reserved);
            return reserved;
        } catch (Exception e) {
            log.error("检查库存是否已预留失败，订单ID：{}", orderId, e);
            return false;
        }
    }

    @Override
    public boolean isStockConfirmed(Long orderId) {
        log.info("检查库存是否已确认，订单ID：{}", orderId);
        try {
            // 这里可以通过查询确认记录表或者其他方式来判断
            // 简化实现：假设如果有出库记录，则认为库存已确认
            boolean confirmed = isStockDeducted(orderId);
            log.info("库存确认检查结果，订单ID：{}，是否已确认：{}", orderId, confirmed);
            return confirmed;
        } catch (Exception e) {
            log.error("检查库存是否已确认失败，订单ID：{}", orderId, e);
            return false;
        }
    }

    @Override
    public boolean isStockRolledBack(Long orderId) {
        log.info("检查库存是否已回滚，订单ID：{}", orderId);
        try {
            // 这里可以通过查询回滚记录表或者其他方式来判断
            // 简化实现：假设如果没有出库记录，则认为库存已回滚
            boolean rolledBack = !isStockDeducted(orderId);
            log.info("库存回滚检查结果，订单ID：{}，是否已回滚：{}", orderId, rolledBack);
            return rolledBack;
        } catch (Exception e) {
            log.error("检查库存是否已回滚失败，订单ID：{}", orderId, e);
            return false;
        }
    }

    @Override
    public boolean isStockFrozen(Long orderId) {
        log.info("检查库存是否已冻结，订单ID：{}", orderId);
        try {
            // 这里可以通过查询冻结记录表或者其他方式来判断
            // 简化实现：假设如果没有出库记录，则认为库存已冻结
            return !isStockDeducted(orderId);
        } catch (Exception e) {
            log.error("检查库存是否已冻结失败，订单ID：{}", orderId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean freezeStock(OrderCreatedEvent event) {
        log.info("冻结库存，订单ID：{}，商品数量：{}", event.getOrderId(),
                event.getOrderItems() != null ? event.getOrderItems().size() : 0);
        try {
            // 遍历订单项，冻结库存
            if (event.getOrderItems() != null) {
                for (OrderCreatedEvent.OrderItem item : event.getOrderItems()) {
                    Long productId = item.getProductId();
                    Integer quantity = item.getQuantity();

                    // 预留库存（冻结）
                    reserveStock(productId, quantity);
                }
            }
            log.info("冻结库存成功，订单ID：{}", event.getOrderId());
            return true;
        } catch (Exception e) {
            log.error("冻结库存失败，订单ID：{}", event.getOrderId(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveStock(StockReserveEvent event) {
        log.info("预留库存，订单ID：{}，商品数量：{}", event.getOrderId(),
                event.getReserveItems() != null ? event.getReserveItems().size() : 0);
        try {
            // 遍历预留项，预留库存
            if (event.getReserveItems() != null) {
                for (StockReserveEvent.StockReserveItem item : event.getReserveItems()) {
                    Long productId = item.getProductId();
                    Integer quantity = item.getQuantity();

                    // 预留库存
                    reserveStock(productId, quantity);
                    
                    // 发送库存预留日志
                    try {
                        asyncLogProducer.sendBusinessLogAsync(
                                "stock-service",
                                "STOCK_MANAGEMENT",
                                "RESERVE",
                                "预留库存操作",
                                productId.toString(),
                                "PRODUCT",
                                String.format("{\"quantity\":%d}", quantity),
                                String.format("{\"reserved\":true}"),
                                event.getOperator() != null ? event.getOperator() : "SYSTEM",
                                "商品: " + productId + " 预留 " + quantity + " 件"
                        );
                    } catch (Exception e) {
                        log.warn("发送库存预留日志失败，商品ID：{}", productId, e);
                    }
                }
            }
            log.info("预留库存成功，订单ID：{}", event.getOrderId());
            return true;
        } catch (Exception e) {
            log.error("预留库存失败，订单ID：{}", event.getOrderId(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmStock(StockConfirmEvent event) {
        log.info("确认库存，订单ID：{}，商品数量：{}", event.getOrderId(),
                event.getConfirmItems() != null ? event.getConfirmItems().size() : 0);
        try {
            // 遍历确认项，确认库存扣减
            if (event.getConfirmItems() != null) {
                for (StockConfirmEvent.StockConfirmItem item : event.getConfirmItems()) {
                    Long productId = item.getProductId();
                    Integer quantity = item.getQuantity();

                    // 确认库存扣减（这里可以添加具体的确认逻辑）
                    log.info("确认库存扣减，商品ID：{}，数量：{}", productId, quantity);
                    
                    // 发送库存确认日志
                    try {
                        asyncLogProducer.sendBusinessLogAsync(
                                "stock-service",
                                "STOCK_MANAGEMENT",
                                "CONFIRM",
                                "确认库存扣减操作",
                                productId.toString(),
                                "PRODUCT",
                                String.format("{\"quantity\":%d}", quantity),
                                String.format("{\"confirmed\":true}"),
                                event.getOperator() != null ? event.getOperator() : "SYSTEM",
                                "商品: " + productId + " 确认扣减 " + quantity + " 件"
                        );
                    } catch (Exception e) {
                        log.warn("发送库存确认日志失败，商品ID：{}", productId, e);
                    }
                }
            }
            log.info("确认库存成功，订单ID：{}", event.getOrderId());
            return true;
        } catch (Exception e) {
            log.error("确认库存失败，订单ID：{}", event.getOrderId(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackStock(StockRollbackEvent event) {
        log.info("回滚库存，订单ID：{}，商品数量：{}", event.getOrderId(),
                event.getRollbackItems() != null ? event.getRollbackItems().size() : 0);
        try {
            // 遍历订单项，回滚库存（释放预留）
            if (event.getRollbackItems() != null) {
                for (StockRollbackEvent.StockRollbackItem item : event.getRollbackItems()) {
                    Long productId = item.getProductId();
                    Integer quantity = item.getQuantity();

                    // 释放预留库存
                    releaseReservedStock(productId, quantity);
                }
            }
            log.info("回滚库存成功，订单ID：{}", event.getOrderId());
            return true;
        } catch (Exception e) {
            log.error("回滚库存失败，订单ID：{}", event.getOrderId(), e);
            return false;
        }
    }


}

