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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collection;
import java.util.List;






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
    @CachePut(
            cacheNames = "stockCache",
            key = "#result.id",
            unless = "#result == null"
    )
    public StockDTO createStock(StockDTO stockDTO) {
        if (stockDTO == null) {
            log.warn("搴撳瓨淇℃伅涓嶈兘涓虹┖");
            throw new IllegalArgumentException("搴撳瓨淇℃伅涓嶈兘涓虹┖");
        }

        try {
            
            Stock stock = stockConverter.toEntity(stockDTO);
            boolean saved = save(stock);

            if (saved) {
                
                return stockConverter.toDTO(stock);
            } else {
                log.error("搴撳瓨璁板綍鍒涘缓澶辫触锛屽晢鍝両D: {}", stockDTO.getProductId());
                throw new BusinessException("鍒涘缓搴撳瓨璁板綍澶辫触");
            }
        } catch (Exception e) {
            log.error("鍒涘缓搴撳瓨璁板綍寮傚父锛屽晢鍝両D: {}", stockDTO.getProductId(), e);
            throw new BusinessException("鍒涘缓搴撳瓨璁板綍澶辫触: " + e.getMessage(), e);
        }
    }

    @Override
    @DistributedLock(
            key = "'stock:update:' + #stockDTO.id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "Acquire stock update lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @CachePut(
            cacheNames = "stockCache",
            key = "#stockDTO.id",
            unless = "#result == false"
    )
    public boolean updateStock(StockDTO stockDTO) {
        if (stockDTO == null || stockDTO.getId() == null) {
            log.warn("搴撳瓨淇℃伅鎴朓D涓嶈兘涓虹┖");
            throw new IllegalArgumentException("搴撳瓨淇℃伅鎴朓D涓嶈兘涓虹┖");
        }

        try {
            
            Stock stock = stockConverter.toEntity(stockDTO);
            boolean updated = updateById(stock);

            if (updated) {
                
                return true;
            } else {
                log.warn("搴撳瓨璁板綍鏇存柊澶辫触锛孖D: {}", stockDTO.getId());
                return false;
            }
        } catch (Exception e) {
            log.error("鏇存柊搴撳瓨璁板綍寮傚父锛孖D: {}", stockDTO.getId(), e);
            throw new BusinessException("鏇存柊搴撳瓨璁板綍澶辫触: " + e.getMessage(), e);
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
            log.warn("搴撳瓨ID涓嶈兘涓虹┖");
            throw new IllegalArgumentException("搴撳瓨ID涓嶈兘涓虹┖");
        }

        try {
            
            Stock stock = getById(id);
            if (stock == null) {
                throw EntityNotFoundException.stock(id);
            }
            return stockConverter.toDTO(stock);
        } catch (Exception e) {
            log.error("鏍规嵁ID鏌ユ壘搴撳瓨澶辫触锛屽簱瀛業D: {}", id, e);
            throw new BusinessException("鑾峰彇搴撳瓨淇℃伅澶辫触", e);
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
            log.warn("鍟嗗搧ID涓嶈兘涓虹┖");
            throw new IllegalArgumentException("鍟嗗搧ID涓嶈兘涓虹┖");
        }

        try {
            
            Stock stock = getOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getProductId, productId));
            return stock != null ? stockConverter.toDTO(stock) : null;
        } catch (Exception e) {
            log.error("鏍规嵁鍟嗗搧ID鏌ユ壘搴撳瓨澶辫触锛屽晢鍝両D: {}", productId, e);
            throw new BusinessException("鑾峰彇搴撳瓨淇℃伅澶辫触", e);
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
            

            
            Page<Stock> page = PageUtils.buildPage(pageDTO);

            
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

            
            Page<Stock> resultPage = this.page(page, queryWrapper);

            
            List<StockVO> stockVOList = stockConverter.toVOList(resultPage.getRecords());

            
            PageResult<StockVO> pageResult = PageResult.of(
                    resultPage.getCurrent(),
                    resultPage.getSize(),
                    resultPage.getTotal(),
                    stockVOList
            );

            


            return pageResult;
        } catch (Exception e) {
            log.error("鍒嗛〉鏌ヨ搴撳瓨鏃跺彂鐢熷紓甯革紝鏌ヨ鏉′欢锛歿}", pageDTO, e);
            throw new BusinessException("鍒嗛〉鏌ヨ搴撳瓨澶辫触");
        }
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
    @CacheEvict(
            cacheNames = "stockCache",
            key = "#id"
    )
    public boolean deleteStock(Long id) {
        

        try {
            Stock stock = getById(id);
            if (stock == null) {
                throw EntityNotFoundException.stock(id);
            }

            boolean result = removeById(id);

            
            return result;
        } catch (Exception e) {
            log.error("鍒犻櫎搴撳瓨淇℃伅澶辫触锛孖D锛歿}", id, e);
            throw new BusinessException("鍒犻櫎搴撳瓨淇℃伅澶辫触", e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:batch:delete:' + #ids.toString()",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "鎵归噺鍒犻櫎搴撳瓨鎿嶄綔鑾峰彇閿佸け璐?
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

        

        try {
            boolean result = removeByIds(ids);
            
            return result;
        } catch (Exception e) {
            log.error("鎵归噺鍒犻櫎搴撳瓨淇℃伅澶辫触锛孖Ds锛歿}", ids, e);
            throw new BusinessException("鎵归噺鍒犻櫎搴撳瓨淇℃伅澶辫触", e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:in:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "搴撳瓨鍏ュ簱鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "stockCache",
            key = "'product:' + #productId"
    )
    public boolean stockIn(Long productId, Integer quantity, String remark) {
        

        try {
            Stock stock = getOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getProductId, productId));
            if (stock == null) {
                throw new EntityNotFoundException("搴撳瓨淇℃伅涓嶅瓨鍦紝鍟嗗搧ID: " + productId + "锛屾棤娉曟墽琛屽叆搴撴搷浣?);
            }

            
            int affected = stockMapper.updateStockQuantity(stock.getId(), quantity);
            if (affected == 0) {
                throw new StockOperationException("鍏ュ簱", productId, "搴撳瓨鏇存柊澶辫触锛屽彲鑳芥槸骞跺彂鍐茬獊鎴栨暟鎹凡鍙樻洿");
            }

            
            createStockInRecord(stock, quantity, remark);

            
            try {
                Integer originalStock = stock.getStockQuantity() - quantity; 

            } catch (Exception e) {
                log.warn("鍙戦€佸簱瀛樺叆搴撴棩蹇楀け璐ワ紝鍟嗗搧ID锛歿}", productId, e);
            }

            
            return true;
        } catch (Exception e) {
            log.error("搴撳瓨鍏ュ簱澶辫触锛屽晢鍝両D锛歿}锛屾暟閲忥細{}", productId, quantity, e);
            throw new BusinessException("搴撳瓨鍏ュ簱澶辫触", e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:out:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "搴撳瓨鍑哄簱鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "stockCache",
            key = "'product:' + #productId"
    )
    public boolean stockOut(Long productId, Integer quantity, Long orderId, String orderNo, String remark) {
        

        try {
            Stock stock = getOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getProductId, productId));
            if (stock == null) {
                throw new EntityNotFoundException("搴撳瓨淇℃伅涓嶅瓨鍦紝鍟嗗搧ID: " + productId + "锛屾棤娉曟墽琛屽嚭搴撴搷浣?);
            }

            
            int availableQuantity = stock.getStockQuantity() - stock.getFrozenQuantity();
            
            if (availableQuantity < quantity) {
                throw new StockInsufficientException(productId, quantity, availableQuantity);
            }

            
            int affected = stockMapper.updateStockQuantity(stock.getId(), -quantity);
            if (affected == 0) {
                throw new StockOperationException("鍑哄簱", productId, "搴撳瓨鏇存柊澶辫触锛屽彲鑳芥槸搴撳瓨涓嶈冻鎴栧苟鍙戝啿绐?);
            }

            
            createStockOutRecord(stock, quantity, orderId, orderNo, remark);

            
            try {
                Integer originalStock = stock.getStockQuantity() + quantity; 

            } catch (Exception e) {
                log.warn("鍙戦€佸簱瀛樺嚭搴撴棩蹇楀け璐ワ紝鍟嗗搧ID锛歿}", productId, e);
            }

            
            return true;
        } catch (Exception e) {
            log.error("搴撳瓨鍑哄簱澶辫触锛屽晢鍝両D锛歿}锛屾暟閲忥細{}", productId, quantity, e);
            throw new BusinessException("搴撳瓨鍑哄簱澶辫触", e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:reserve:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "搴撳瓨棰勭暀鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "stockCache",
            key = "'product:' + #productId"
    )
    public boolean reserveStock(Long productId, Integer quantity) {
        

        try {
            Stock stock = getOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getProductId, productId));
            if (stock == null) {
                throw new EntityNotFoundException("搴撳瓨淇℃伅涓嶅瓨鍦紝鍟嗗搧ID: " + productId + "锛屾棤娉曟墽琛岄鐣欏簱瀛樻搷浣?);
            }

            int affected = stockMapper.freezeStock(stock.getId(), quantity);
            if (affected == 0) {
                throw new StockFrozenException("棰勭暀", productId, quantity);
            }

            
            try {

            } catch (Exception e) {
                log.warn("鍙戦€佸簱瀛樺喕缁撴棩蹇楀け璐ワ紝鍟嗗搧ID锛歿}", productId, e);
            }

            
            return true;
        } catch (Exception e) {
            log.error("棰勭暀搴撳瓨澶辫触锛屽晢鍝両D锛歿}锛屾暟閲忥細{}", productId, quantity, e);
            throw new BusinessException("棰勭暀搴撳瓨澶辫触", e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:release:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "搴撳瓨閲婃斁棰勭暀鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "stockCache",
            key = "'product:' + #productId"
    )
    public boolean releaseReservedStock(Long productId, Integer quantity) {
        

        try {
            Stock stock = getOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getProductId, productId));
            if (stock == null) {
                throw new EntityNotFoundException("搴撳瓨淇℃伅涓嶅瓨鍦紝鍟嗗搧ID: " + productId + "锛屾棤娉曟墽琛岄噴鏀鹃鐣欏簱瀛樻搷浣?);
            }

            int affected = stockMapper.unfreezeStock(stock.getId(), quantity);
            if (affected == 0) {
                throw new StockFrozenException("閲婃斁", productId, quantity);
            }

            
            try {

            } catch (Exception e) {
                log.warn("鍙戦€佸簱瀛樿В鍐绘棩蹇楀け璐ワ紝鍟嗗搧ID锛歿}", productId, e);
            }

            
            return true;
        } catch (Exception e) {
            log.error("閲婃斁棰勭暀搴撳瓨澶辫触锛屽晢鍝両D锛歿}锛屾暟閲忥細{}", productId, quantity, e);
            throw new BusinessException("閲婃斁棰勭暀搴撳瓨澶辫触", e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'stock:confirm:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "纭棰勭暀搴撳瓨鎵ｅ噺鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "stockCache",
            key = "'product:' + #productId"
    )
    public boolean confirmReservedStockOut(Long productId, Integer quantity, Long orderId, String orderNo, String remark) {
        

        try {
            Stock stock = getOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getProductId, productId));
            if (stock == null) {
                throw new EntityNotFoundException("搴撳瓨淇℃伅涓嶅瓨鍦紝鍟嗗搧ID: " + productId + "锛屾棤娉曟墽琛岀‘璁ゆ墸鍑忔搷浣?);
            }

            int affected = stockMapper.confirmStockOutWithCondition(productId, quantity);
            if (affected == 0) {
                throw new StockOperationException("纭鎵ｅ噺", productId, "鍐荤粨搴撳瓨涓嶈冻鎴栧苟鍙戝啿绐?);
            }

            createStockOutRecord(stock, quantity, orderId, orderNo, remark);
            markOrderState(ORDER_CONFIRMED_KEY_PREFIX, orderId);
            clearOrderState(ORDER_RESERVED_KEY_PREFIX, orderId);
            
            return true;
        } catch (Exception e) {
            log.error("纭棰勭暀搴撳瓨鎵ｅ噺澶辫触锛屽晢鍝両D锛歿}锛屾暟閲忥細{}锛岃鍗旾D锛歿}", productId, quantity, orderId, e);
            throw new BusinessException("纭棰勭暀搴撳瓨鎵ｅ噺澶辫触", e);
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
            
            int availableQuantity = stock.getStockQuantity() - stock.getFrozenQuantity();
            return availableQuantity >= quantity;
        } catch (Exception e) {
            log.error("妫€鏌ュ簱瀛樻槸鍚﹀厖瓒虫椂鍙戠敓寮傚父锛屽晢鍝両D锛歿}", productId, e);
            return false;
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

    @Override
    public boolean isStockDeducted(Long orderId) {
        
        try {
            
            long count = stockOutMapper.selectCount(new LambdaQueryWrapper<StockOut>()
                    .eq(StockOut::getOrderId, orderId));
            boolean deducted = count > 0;
            
            return deducted;
        } catch (Exception e) {
            log.error("妫€鏌ュ簱瀛樻槸鍚﹀凡鎵ｅ噺澶辫触锛岃鍗旾D锛歿}", orderId, e);
            return false;
        }
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
    private boolean hasOrderState(String keyPrefix, Long orderId) {
        if (orderId == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(keyPrefix + orderId));
        } catch (Exception e) {
            log.warn("Check order stock state failed, keyPrefix={}, orderId={}", keyPrefix, orderId, e);
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
            log.warn("Mark order stock state failed, keyPrefix={}, orderId={}", keyPrefix, orderId, e);
        }
    }

    private void clearOrderState(String keyPrefix, Long orderId) {
        if (orderId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(keyPrefix + orderId);
        } catch (Exception e) {
            log.warn("Clear order stock state failed, keyPrefix={}, orderId={}", keyPrefix, orderId, e);
        }
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
            log.warn("鎵归噺鍒涘缓搴撳瓨璁板綍澶辫触锛屽簱瀛樹俊鎭垪琛ㄤ负绌?);
            throw new BusinessException("搴撳瓨淇℃伅鍒楄〃涓嶈兘涓虹┖");
        }

        if (stockDTOList.size() > 100) {
            throw new BusinessException("鎵归噺鍒涘缓鏁伴噺涓嶈兘瓒呰繃100");
        }

        

        int successCount = 0;
        for (StockDTO stockDTO : stockDTOList) {
            try {
                createStock(stockDTO);
                successCount++;
            } catch (Exception e) {
                log.error("鍒涘缓搴撳瓨璁板綍澶辫触锛屽晢鍝両D: {}", stockDTO.getProductId(), e);
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
            log.warn("鎵归噺鏇存柊搴撳瓨淇℃伅澶辫触锛屽簱瀛樹俊鎭垪琛ㄤ负绌?);
            throw new BusinessException("搴撳瓨淇℃伅鍒楄〃涓嶈兘涓虹┖");
        }

        if (stockDTOList.size() > 100) {
            throw new BusinessException("鎵归噺鏇存柊鏁伴噺涓嶈兘瓒呰繃100");
        }

        

        int successCount = 0;
        for (StockDTO stockDTO : stockDTOList) {
            try {
                if (updateStock(stockDTO)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("鏇存柊搴撳瓨淇℃伅澶辫触锛屽簱瀛業D: {}", stockDTO.getId(), e);
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
    public Integer batchStockIn(List<StockService.StockAdjustmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            log.warn("鎵归噺鍏ュ簱澶辫触锛屽叆搴撹姹傚垪琛ㄤ负绌?);
            throw new BusinessException("鍏ュ簱璇锋眰鍒楄〃涓嶈兘涓虹┖");
        }

        if (requests.size() > 100) {
            throw new BusinessException("鎵归噺鍏ュ簱鏁伴噺涓嶈兘瓒呰繃100");
        }

        

        int successCount = 0;
        for (StockService.StockAdjustmentRequest request : requests) {
            try {
                if (stockIn(request.getProductId(), request.getQuantity(), request.getRemark())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("鍏ュ簱澶辫触锛屽晢鍝両D: {}", request.getProductId(), e);
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
    public Integer batchStockOut(List<StockService.StockAdjustmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            log.warn("鎵归噺鍑哄簱澶辫触锛屽嚭搴撹姹傚垪琛ㄤ负绌?);
            throw new BusinessException("鍑哄簱璇锋眰鍒楄〃涓嶈兘涓虹┖");
        }

        if (requests.size() > 100) {
            throw new BusinessException("鎵归噺鍑哄簱鏁伴噺涓嶈兘瓒呰繃100");
        }

        

        int successCount = 0;
        for (StockService.StockAdjustmentRequest request : requests) {
            try {
                if (stockOut(request.getProductId(), request.getQuantity(),
                        request.getOrderId(), request.getOrderNo(), request.getRemark())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("鍑哄簱澶辫触锛屽晢鍝両D: {}", request.getProductId(), e);
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
    public Integer batchReserveStock(List<StockService.StockAdjustmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            log.warn("鎵归噺棰勭暀搴撳瓨澶辫触锛岄鐣欒姹傚垪琛ㄤ负绌?);
            throw new BusinessException("棰勭暀璇锋眰鍒楄〃涓嶈兘涓虹┖");
        }

        if (requests.size() > 100) {
            throw new BusinessException("鎵归噺棰勭暀鏁伴噺涓嶈兘瓒呰繃100");
        }

        

        int successCount = 0;
        for (StockService.StockAdjustmentRequest request : requests) {
            try {
                if (reserveStock(request.getProductId(), request.getQuantity())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("棰勭暀搴撳瓨澶辫触锛屽晢鍝両D: {}", request.getProductId(), e);
            }
        }

        
        return successCount;
    }

}



