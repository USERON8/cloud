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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    private static final String ORDER_RESERVED_KEY_PREFIX = "stock:order:reserved:";
    private static final String ORDER_CONFIRMED_KEY_PREFIX = "stock:order:confirmed:";
    private static final String ORDER_ROLLED_BACK_KEY_PREFIX = "stock:order:rolledback:";
    private static final Duration ORDER_STATE_KEY_TTL = Duration.ofDays(7);

    private final StockMapper stockMapper;
    private final StockInMapper stockInMapper;
    private final StockOutMapper stockOutMapper;
    private final StockConverter stockConverter;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @DistributedLock(
            key = "'stock:create:' + #stockDTO.productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "Acquire stock create lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @CachePut(cacheNames = "stockCache", key = "#result.id", unless = "#result == null")
    public StockDTO createStock(StockDTO stockDTO) {
        if (stockDTO == null || stockDTO.getProductId() == null) {
            throw new IllegalArgumentException("stockDTO and productId are required");
        }

        Stock existing = getOne(new LambdaQueryWrapper<Stock>().eq(Stock::getProductId, stockDTO.getProductId()));
        if (existing != null) {
            throw new BusinessException("Stock already exists for productId=" + stockDTO.getProductId());
        }

        Stock stock = stockConverter.toEntity(stockDTO);
        initStockOnCreate(stock);
        boolean saved = save(stock);
        if (!saved) {
            throw new BusinessException("Create stock failed");
        }
        return stockConverter.toDTO(stock);
    }

    @Override
    @DistributedLock(
            key = "'stock:update:' + #stockDTO.id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "Acquire stock update lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "stockCache", key = "#stockDTO.id"),
            @CacheEvict(cacheNames = "stockCache", key = "'product:' + #stockDTO.productId")
    })
    public boolean updateStock(StockDTO stockDTO) {
        if (stockDTO == null || stockDTO.getId() == null) {
            throw new IllegalArgumentException("stockDTO and id are required");
        }

        Stock existing = getById(stockDTO.getId());
        if (existing == null) {
            throw EntityNotFoundException.stock(stockDTO.getId());
        }

        Stock stock = stockConverter.toEntity(stockDTO);
        mergeStockForUpdate(stock, existing);
        return updateById(stock);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "stockCache", key = "#id", unless = "#result == null")
    public StockDTO getStockById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("stock id is required");
        }
        Stock stock = getById(id);
        if (stock == null) {
            throw EntityNotFoundException.stock(id);
        }
        return stockConverter.toDTO(stock);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "stockCache", key = "'product:' + #productId", unless = "#result == null")
    public StockDTO getStockByProductId(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
        Stock stock = getOne(new LambdaQueryWrapper<Stock>().eq(Stock::getProductId, productId));
        return stock == null ? null : stockConverter.toDTO(stock);
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
        List<Stock> stocks = list(new LambdaQueryWrapper<Stock>().in(Stock::getProductId, productIds));
        return stockConverter.toDTOList(stocks);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StockVO> pageQuery(StockPageDTO pageDTO) {
        Page<Stock> page = PageUtils.buildPage(pageDTO);
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        if (pageDTO.getProductId() != null) {
            wrapper.eq(Stock::getProductId, pageDTO.getProductId());
        }
        if (pageDTO.getProductName() != null && !pageDTO.getProductName().isBlank()) {
            wrapper.like(Stock::getProductName, pageDTO.getProductName());
        }
        if (pageDTO.getStockStatus() != null) {
            wrapper.eq(Stock::getStockStatus, pageDTO.getStockStatus());
        }
        wrapper.orderByDesc(Stock::getCreatedAt);

        Page<Stock> resultPage = page(page, wrapper);
        List<StockVO> voList = stockConverter.toVOList(resultPage.getRecords());
        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), voList);
    }

    @Override
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:delete:' + #id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "Acquire stock delete lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "stockCache", key = "#id")
    public boolean deleteStock(Long id) {
        Stock stock = getById(id);
        if (stock == null) {
            throw EntityNotFoundException.stock(id);
        }
        return removeById(id);
    }

    @Override
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:batch:delete:' + #ids.toString()",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "Acquire stock batch delete lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "stockCache", allEntries = true, condition = "#ids != null && !#ids.isEmpty()")
    public boolean deleteStocksByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return removeByIds(ids);
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:in:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "Acquire stock in lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "stockCache", key = "'product:' + #productId")
    public boolean stockIn(Long productId, Integer quantity, String remark) {
        validateQuantity(productId, quantity);
        Stock stock = findStockByProductId(productId);

        int affected = stockMapper.stockInWithCondition(productId, quantity);
        if (affected <= 0) {
            affected = stockMapper.updateStockQuantity(stock.getId(), quantity);
        }
        if (affected <= 0) {
            return false;
        }

        createStockInRecord(stock, quantity, remark);
        return true;
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:out:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "Acquire stock out lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "stockCache", key = "'product:' + #productId")
    public boolean stockOut(Long productId, Integer quantity, Long orderId, String orderNo, String remark) {
        validateQuantity(productId, quantity);
        Stock stock = findStockByProductId(productId);

        Integer available = stock.getAvailableQuantity();
        if (available == null || available < quantity) {
            throw new StockInsufficientException(productId, quantity, available == null ? 0 : available);
        }

        int affected = stockMapper.stockOutWithCondition(productId, quantity);
        if (affected <= 0) {
            return false;
        }

        createStockOutRecord(stock, quantity, orderId, orderNo, remark);
        markOrderState(ORDER_CONFIRMED_KEY_PREFIX, orderId);
        return true;
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:reserve:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "Acquire stock reserve lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "stockCache", key = "'product:' + #productId")
    public boolean reserveStock(Long productId, Integer quantity) {
        validateQuantity(productId, quantity);
        findStockByProductId(productId);

        int affected = stockMapper.reserveStockWithCondition(productId, quantity);
        if (affected <= 0) {
            throw new StockFrozenException("reserve stock", productId, quantity);
        }
        return true;
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:release:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "Acquire stock release lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "stockCache", key = "'product:' + #productId")
    public boolean releaseReservedStock(Long productId, Integer quantity) {
        validateQuantity(productId, quantity);
        findStockByProductId(productId);

        int affected = stockMapper.releaseReservedStockWithCondition(productId, quantity);
        if (affected <= 0) {
            throw new StockFrozenException("release reserved stock", productId, quantity);
        }
        return true;
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:confirm:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "Acquire confirm stock out lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "stockCache", key = "'product:' + #productId")
    public boolean confirmReservedStockOut(Long productId, Integer quantity, Long orderId, String orderNo, String remark) {
        validateQuantity(productId, quantity);
        Stock stock = findStockByProductId(productId);

        int affected = stockMapper.confirmStockOutWithCondition(productId, quantity);
        if (affected <= 0) {
            return false;
        }

        createStockOutRecord(stock, quantity, orderId, orderNo, remark);
        markOrderState(ORDER_CONFIRMED_KEY_PREFIX, orderId);
        clearOrderState(ORDER_RESERVED_KEY_PREFIX, orderId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkStockSufficient(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            return false;
        }

        try {
            Stock stock = getOne(new LambdaQueryWrapper<Stock>().eq(Stock::getProductId, productId));
            if (stock == null) {
                return false;
            }
            Integer available = stock.getAvailableQuantity();
            return available != null && available >= quantity;
        } catch (Exception e) {
            log.error("Check stock sufficient failed: productId={}", productId, e);
            return false;
        }
    }

    @Override
    public boolean isStockDeducted(Long orderId) {
        if (orderId == null) {
            return false;
        }
        long count = stockOutMapper.selectCount(new LambdaQueryWrapper<StockOut>().eq(StockOut::getOrderId, orderId));
        return count > 0;
    }

    @Override
    public boolean isStockFrozen(Long orderId) {
        return isStockReserved(orderId) && !isStockConfirmed(orderId) && !isStockRolledBack(orderId);
    }

    @Override
    public boolean isStockReserved(Long orderId) {
        return hasOrderState(ORDER_RESERVED_KEY_PREFIX, orderId);
    }

    @Override
    public boolean isStockConfirmed(Long orderId) {
        return hasOrderState(ORDER_CONFIRMED_KEY_PREFIX, orderId) || isStockDeducted(orderId);
    }

    @Override
    public boolean isStockRolledBack(Long orderId) {
        return hasOrderState(ORDER_ROLLED_BACK_KEY_PREFIX, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(
            key = "'stock:batch:create'",
            waitTime = 10,
            leaseTime = 60,
            failMessage = "Acquire stock batch create lock failed"
    )
    public Integer batchCreateStocks(List<StockDTO> stockDTOList) {
        if (stockDTOList == null || stockDTOList.isEmpty()) {
            throw new BusinessException("stockDTOList cannot be empty");
        }
        if (stockDTOList.size() > 100) {
            throw new BusinessException("batch create size cannot exceed 100");
        }

        int successCount = 0;
        for (StockDTO dto : stockDTOList) {
            try {
                createStock(dto);
                successCount++;
            } catch (Exception e) {
                log.error("Batch create stock failed: productId={}", dto.getProductId(), e);
            }
        }
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(
            key = "'stock:batch:update'",
            waitTime = 10,
            leaseTime = 60,
            failMessage = "Acquire stock batch update lock failed"
    )
    public Integer batchUpdateStocks(List<StockDTO> stockDTOList) {
        if (stockDTOList == null || stockDTOList.isEmpty()) {
            throw new BusinessException("stockDTOList cannot be empty");
        }
        if (stockDTOList.size() > 100) {
            throw new BusinessException("batch update size cannot exceed 100");
        }

        int successCount = 0;
        for (StockDTO dto : stockDTOList) {
            try {
                if (updateStock(dto)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Batch update stock failed: stockId={}", dto.getId(), e);
            }
        }
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(
            key = "'stock:batch:in'",
            waitTime = 10,
            leaseTime = 60,
            failMessage = "Acquire stock batch in lock failed"
    )
    public Integer batchStockIn(List<StockAdjustmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException("stock in requests cannot be empty");
        }
        if (requests.size() > 100) {
            throw new BusinessException("batch stock in size cannot exceed 100");
        }

        int successCount = 0;
        for (StockAdjustmentRequest request : requests) {
            try {
                if (stockIn(request.getProductId(), request.getQuantity(), request.getRemark())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Batch stock in failed: productId={}", request.getProductId(), e);
            }
        }
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(
            key = "'stock:batch:out'",
            waitTime = 10,
            leaseTime = 60,
            failMessage = "Acquire stock batch out lock failed"
    )
    public Integer batchStockOut(List<StockAdjustmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException("stock out requests cannot be empty");
        }
        if (requests.size() > 100) {
            throw new BusinessException("batch stock out size cannot exceed 100");
        }

        int successCount = 0;
        for (StockAdjustmentRequest request : requests) {
            try {
                if (stockOut(
                        request.getProductId(),
                        request.getQuantity(),
                        request.getOrderId(),
                        request.getOrderNo(),
                        request.getRemark())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Batch stock out failed: productId={}", request.getProductId(), e);
            }
        }
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(
            key = "'stock:batch:reserve'",
            waitTime = 10,
            leaseTime = 60,
            failMessage = "Acquire stock batch reserve lock failed"
    )
    public Integer batchReserveStock(List<StockAdjustmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException("reserve requests cannot be empty");
        }
        if (requests.size() > 100) {
            throw new BusinessException("batch reserve size cannot exceed 100");
        }

        int successCount = 0;
        for (StockAdjustmentRequest request : requests) {
            try {
                if (reserveStock(request.getProductId(), request.getQuantity())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Batch reserve stock failed: productId={}", request.getProductId(), e);
            }
        }
        return successCount;
    }

    private void initStockOnCreate(Stock stock) {
        if (stock.getStockQuantity() == null) {
            stock.setStockQuantity(0);
        }
        if (stock.getFrozenQuantity() == null) {
            stock.setFrozenQuantity(0);
        }
        if (stock.getStockStatus() == null) {
            stock.setStockStatus(1);
        }
        if (stock.getLowStockThreshold() == null) {
            stock.setLowStockThreshold(0);
        }
    }

    private void mergeStockForUpdate(Stock target, Stock source) {
        if (target.getProductId() == null) {
            target.setProductId(source.getProductId());
        }
        if (target.getProductName() == null) {
            target.setProductName(source.getProductName());
        }
        if (target.getStockQuantity() == null) {
            target.setStockQuantity(source.getStockQuantity());
        }
        if (target.getFrozenQuantity() == null) {
            target.setFrozenQuantity(source.getFrozenQuantity());
        }
        if (target.getStockStatus() == null) {
            target.setStockStatus(source.getStockStatus());
        }
        if (target.getLowStockThreshold() == null) {
            target.setLowStockThreshold(source.getLowStockThreshold());
        }
    }

    private Stock findStockByProductId(Long productId) {
        Stock stock = getOne(new LambdaQueryWrapper<Stock>().eq(Stock::getProductId, productId));
        if (stock == null) {
            throw new EntityNotFoundException("Stock", "productId: " + productId);
        }
        return stock;
    }

    private void validateQuantity(Long productId, Integer quantity) {
        if (productId == null) {
            throw new BusinessException("productId is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("quantity must be greater than 0");
        }
    }

    private void createStockInRecord(Stock stock, Integer quantity, String remark) {
        StockIn stockIn = new StockIn();
        stockIn.setProductId(stock.getProductId());
        stockIn.setQuantity(quantity);
        stockInMapper.insert(stockIn);
    }

    private void createStockOutRecord(Stock stock, Integer quantity, Long orderId, String orderNo, String remark) {
        StockOut stockOut = new StockOut();
        stockOut.setProductId(stock.getProductId());
        stockOut.setOrderId(orderId);
        stockOut.setQuantity(quantity);
        stockOutMapper.insert(stockOut);
    }

    private boolean hasOrderState(String keyPrefix, Long orderId) {
        if (orderId == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(keyPrefix + orderId));
        } catch (Exception e) {
            log.warn("Check order state failed: keyPrefix={}, orderId={}", keyPrefix, orderId, e);
            return false;
        }
    }

    private void markOrderState(String keyPrefix, Long orderId) {
        if (orderId == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(keyPrefix + orderId, "1", ORDER_STATE_KEY_TTL);
        } catch (Exception e) {
            log.warn("Mark order state failed: keyPrefix={}, orderId={}", keyPrefix, orderId, e);
        }
    }

    private void clearOrderState(String keyPrefix, Long orderId) {
        if (orderId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(keyPrefix + orderId);
        } catch (Exception e) {
            log.warn("Clear order state failed: keyPrefix={}, orderId={}", keyPrefix, orderId, e);
        }
    }
}
