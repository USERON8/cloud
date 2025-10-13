package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.stock.StockDTO;
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
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.module.entity.StockIn;
import com.cloud.stock.module.entity.StockOut;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CachePut(
            cacheNames = "stockCache",
            key = "#result.id",
            unless = "#result == null"
    )
    public StockDTO createStock(StockDTO stockDTO) {
        if (stockDTO == null) {
            log.warn("库存信息不能为空");
            throw new IllegalArgumentException("库存信息不能为空");
        }

        try {
            log.info("创建库存记录，商品ID: {}", stockDTO.getProductId());
            Stock stock = stockConverter.toEntity(stockDTO);
            boolean saved = save(stock);

            if (saved) {
                log.info("库存记录创建成功，ID: {}", stock.getId());
                return stockConverter.toDTO(stock);
            } else {
                log.error("库存记录创建失败，商品ID: {}", stockDTO.getProductId());
                throw new BusinessException("创建库存记录失败");
            }
        } catch (Exception e) {
            log.error("创建库存记录异常，商品ID: {}", stockDTO.getProductId(), e);
            throw new BusinessException("创建库存记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CachePut(
            cacheNames = "stockCache",
            key = "#stockDTO.id",
            unless = "#result == false"
    )
    public boolean updateStock(StockDTO stockDTO) {
        if (stockDTO == null || stockDTO.getId() == null) {
            log.warn("库存信息或ID不能为空");
            throw new IllegalArgumentException("库存信息或ID不能为空");
        }

        try {
            log.info("更新库存记录，ID: {}", stockDTO.getId());
            Stock stock = stockConverter.toEntity(stockDTO);
            boolean updated = updateById(stock);

            if (updated) {
                log.info("库存记录更新成功，ID: {}", stock.getId());
                return true;
            } else {
                log.warn("库存记录更新失败，ID: {}", stockDTO.getId());
                return false;
            }
        } catch (Exception e) {
            log.error("更新库存记录异常，ID: {}", stockDTO.getId(), e);
            throw new BusinessException("更新库存记录失败: " + e.getMessage(), e);
        }
    }

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
    public boolean isStockFrozen(Long orderId) {
        return false;
    }

    @Override
    public boolean isStockReserved(Long orderId) {
        return false;
    }

    @Override
    public boolean isStockConfirmed(Long orderId) {
        return false;
    }

    @Override
    public boolean isStockRolledBack(Long orderId) {
        return false;
    }

    // ================= 批量操作方法实现 =================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchCreateStocks(List<StockDTO> stockDTOList) {
        if (stockDTOList == null || stockDTOList.isEmpty()) {
            log.warn("批量创建库存记录失败，库存信息列表为空");
            throw new BusinessException("库存信息列表不能为空");
        }

        if (stockDTOList.size() > 100) {
            throw new BusinessException("批量创建数量不能超过100");
        }

        log.info("开始批量创建库存记录，数量: {}", stockDTOList.size());

        int successCount = 0;
        for (StockDTO stockDTO : stockDTOList) {
            try {
                createStock(stockDTO);
                successCount++;
            } catch (Exception e) {
                log.error("创建库存记录失败，商品ID: {}", stockDTO.getProductId(), e);
            }
        }

        log.info("批量创建库存记录完成，成功: {}/{}", successCount, stockDTOList.size());
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchUpdateStocks(List<StockDTO> stockDTOList) {
        if (stockDTOList == null || stockDTOList.isEmpty()) {
            log.warn("批量更新库存信息失败，库存信息列表为空");
            throw new BusinessException("库存信息列表不能为空");
        }

        if (stockDTOList.size() > 100) {
            throw new BusinessException("批量更新数量不能超过100");
        }

        log.info("开始批量更新库存信息，数量: {}", stockDTOList.size());

        int successCount = 0;
        for (StockDTO stockDTO : stockDTOList) {
            try {
                if (updateStock(stockDTO)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("更新库存信息失败，库存ID: {}", stockDTO.getId(), e);
            }
        }

        log.info("批量更新库存信息完成，成功: {}/{}", successCount, stockDTOList.size());
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchStockIn(List<StockService.StockAdjustmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            log.warn("批量入库失败，入库请求列表为空");
            throw new BusinessException("入库请求列表不能为空");
        }

        if (requests.size() > 100) {
            throw new BusinessException("批量入库数量不能超过100");
        }

        log.info("开始批量入库，数量: {}", requests.size());

        int successCount = 0;
        for (StockService.StockAdjustmentRequest request : requests) {
            try {
                if (stockIn(request.getProductId(), request.getQuantity(), request.getRemark())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("入库失败，商品ID: {}", request.getProductId(), e);
            }
        }

        log.info("批量入库完成，成功: {}/{}", successCount, requests.size());
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchStockOut(List<StockService.StockAdjustmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            log.warn("批量出库失败，出库请求列表为空");
            throw new BusinessException("出库请求列表不能为空");
        }

        if (requests.size() > 100) {
            throw new BusinessException("批量出库数量不能超过100");
        }

        log.info("开始批量出库，数量: {}", requests.size());

        int successCount = 0;
        for (StockService.StockAdjustmentRequest request : requests) {
            try {
                if (stockOut(request.getProductId(), request.getQuantity(),
                        request.getOrderId(), request.getOrderNo(), request.getRemark())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("出库失败，商品ID: {}", request.getProductId(), e);
            }
        }

        log.info("批量出库完成，成功: {}/{}", successCount, requests.size());
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchReserveStock(List<StockService.StockAdjustmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            log.warn("批量预留库存失败，预留请求列表为空");
            throw new BusinessException("预留请求列表不能为空");
        }

        if (requests.size() > 100) {
            throw new BusinessException("批量预留数量不能超过100");
        }

        log.info("开始批量预留库存，数量: {}", requests.size());

        int successCount = 0;
        for (StockService.StockAdjustmentRequest request : requests) {
            try {
                if (reserveStock(request.getProductId(), request.getQuantity())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("预留库存失败，商品ID: {}", request.getProductId(), e);
            }
        }

        log.info("批量预留库存完成，成功: {}/{}", successCount, requests.size());
        return successCount;
    }

}

