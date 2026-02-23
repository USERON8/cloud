package com.cloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.result.PageResult;
import com.cloud.product.converter.ShopConverter;
import com.cloud.product.mapper.ShopMapper;
import com.cloud.product.module.dto.ShopPageDTO;
import com.cloud.product.module.dto.ShopRequestDTO;
import com.cloud.product.module.entity.Shop;
import com.cloud.product.module.vo.ShopVO;
import com.cloud.product.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;









@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop>
        implements ShopService {

    private final ShopConverter shopConverter;

    

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"shopCache", "shopListCache"}, allEntries = true)
    public Long createShop(ShopRequestDTO requestDTO) {
        

        
        Shop shop = shopConverter.requestDTOToEntity(requestDTO);

        boolean saved = save(shop);
        if (!saved) {
            throw new RuntimeException("鍒涘缓搴楅摵澶辫触");
        }

        
        return shop.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "shopCache", key = "#id"),
                    @CacheEvict(cacheNames = "shopListCache", allEntries = true)
            }
    )
    public Boolean updateShop(Long id, ShopRequestDTO requestDTO) {
        

        Shop existingShop = getById(id);
        if (existingShop == null) {
            throw new RuntimeException("搴楅摵涓嶅瓨鍦? " + id);
        }

        
        Shop updateShop = shopConverter.requestDTOToEntity(requestDTO);
        updateShop.setId(id);

        boolean updated = updateById(updateShop);
        if (!updated) {
            throw new RuntimeException("鏇存柊搴楅摵澶辫触");
        }

        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "shopCache", key = "#id"),
                    @CacheEvict(cacheNames = "shopListCache", allEntries = true)
            }
    )
    public Boolean deleteShop(Long id) {
        

        Shop shop = getById(id);
        if (shop == null) {
            throw new RuntimeException("搴楅摵涓嶅瓨鍦? " + id);
        }

        boolean deleted = removeById(id);
        if (!deleted) {
            throw new RuntimeException("鍒犻櫎搴楅摵澶辫触");
        }

        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"shopCache", "shopListCache"}, allEntries = true)
    public Boolean batchDeleteShops(List<Long> ids) {
        

        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }

        boolean deleted = removeBatchByIds(ids);
        if (!deleted) {
            throw new RuntimeException("鎵归噺鍒犻櫎搴楅摵澶辫触");
        }

        
        return true;
    }

    

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopCache", key = "#id",
            condition = "#id != null")
    public ShopVO getShopById(Long id) {
        log.debug("鑾峰彇搴楅摵璇︽儏: {}", id);

        Shop shop = getById(id);
        if (shop == null) {
            throw new RuntimeException("搴楅摵涓嶅瓨鍦? " + id);
        }

        return shopConverter.toVO(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopVO> getShopsByIds(List<Long> ids) {
        log.debug("鎵归噺鑾峰彇搴楅摵: {}", ids);
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }

        List<Shop> shops = listByIds(ids);
        return shopConverter.toVOList(shops);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopListCache",
            key = "'page:' + #pageDTO.current + ':' + #pageDTO.size + ':' + (#pageDTO.shopNameKeyword ?: 'null') + ':' + (#pageDTO.status ?: 'null')")
    public PageResult<ShopVO> getShopsPage(ShopPageDTO pageDTO) {
        log.debug("鍒嗛〉鏌ヨ搴楅摵: {}", pageDTO);

        Page<Shop> page = new Page<>(pageDTO.getCurrent(), pageDTO.getSize());
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(pageDTO.getShopNameKeyword())) {
            queryWrapper.like(Shop::getShopName, pageDTO.getShopNameKeyword());
        }

        if (pageDTO.getStatus() != null) {
            queryWrapper.eq(Shop::getStatus, pageDTO.getStatus());
        }

        queryWrapper.orderByDesc(Shop::getCreatedAt);

        Page<Shop> shopPage = page(page, queryWrapper);
        List<ShopVO> shopVOs = shopConverter.toVOList(shopPage.getRecords());

        return PageResult.of(shopVOs, shopPage.getTotal(), pageDTO.getCurrent(), pageDTO.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopListCache",
            key = "'merchant:' + #merchantId + ':' + (#status ?: 'null')")
    public List<ShopVO> getShopsByMerchantId(Long merchantId, Integer status) {
        log.debug("鏍规嵁鍟嗘埛ID鏌ヨ搴楅摵: merchantId={}, status={}", merchantId, status);

        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getMerchantId, merchantId);
        if (status != null) {
            queryWrapper.eq(Shop::getStatus, status);
        }
        queryWrapper.orderByDesc(Shop::getCreatedAt);

        List<Shop> shops = list(queryWrapper);
        return shopConverter.toVOList(shops);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopListCache",
            key = "'search:' + #shopName + ':' + (#status ?: 'null')")
    public List<ShopVO> searchShopsByName(String shopName, Integer status) {
        log.debug("鎼滅储搴楅摵: shopName={}, status={}", shopName, status);

        if (!StringUtils.hasText(shopName)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Shop::getShopName, shopName);
        if (status != null) {
            queryWrapper.eq(Shop::getStatus, status);
        }
        queryWrapper.orderByDesc(Shop::getCreatedAt);

        List<Shop> shops = list(queryWrapper);
        return shopConverter.toVOList(shops);
    }

    

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "shopCache", key = "#id"),
                    @CacheEvict(cacheNames = "shopListCache", allEntries = true)
            }
    )
    public Boolean enableShop(Long id) {
        
        return updateShopStatus(id, 1, "鍚敤");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "shopCache", key = "#id"),
                    @CacheEvict(cacheNames = "shopListCache", allEntries = true)
            }
    )
    public Boolean disableShop(Long id) {
        
        return updateShopStatus(id, 0, "绂佺敤");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"shopCache", "shopListCache"}, allEntries = true)
    public Boolean batchEnableShops(List<Long> ids) {
        
        return batchUpdateShopStatus(ids, 1, "鎵归噺鍚敤");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"shopCache", "shopListCache"}, allEntries = true)
    public Boolean batchDisableShops(List<Long> ids) {
        
        return batchUpdateShopStatus(ids, 0, "鎵归噺绂佺敤");
    }

    

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopCache", key = "'stats:total'")
    public Long getTotalShopCount() {
        return count();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopCache", key = "'stats:enabled'")
    public Long getEnabledShopCount() {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getStatus, 1);
        return count(queryWrapper);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopCache", key = "'stats:disabled'")
    public Long getDisabledShopCount() {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getStatus, 0);
        return count(queryWrapper);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopCache", key = "'stats:merchant:' + #merchantId")
    public Long getShopCountByMerchantId(Long merchantId) {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getMerchantId, merchantId);
        return count(queryWrapper);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopCache", key = "'permission:' + #merchantId + ':' + #shopId")
    public Boolean hasPermission(Long merchantId, Long shopId) {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getId, shopId)
                .eq(Shop::getMerchantId, merchantId);
        return count(queryWrapper) > 0;
    }

    

    @Override
    @CacheEvict(cacheNames = "shopCache", key = "#id")
    public void evictShopCache(Long id) {
        
    }

    @Override
    @CacheEvict(cacheNames = {"shopCache", "shopListCache"}, allEntries = true)
    public void evictAllShopCache() {
    }

    @Override
    public void warmupShopCache(List<Long> ids) {
        

        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        
        ids.forEach(this::getShopById);

        
    }

    

    


    private Boolean updateShopStatus(Long id, Integer status, String operation) {
        Shop shop = getById(id);
        if (shop == null) {
            throw new RuntimeException("搴楅摵涓嶅瓨鍦? " + id);
        }

        LambdaUpdateWrapper<Shop> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Shop::getId, id)
                .set(Shop::getStatus, status);

        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new RuntimeException(operation + "搴楅摵澶辫触");
        }

        
        return true;
    }

    


    private Boolean batchUpdateShopStatus(List<Long> ids, Integer status, String operation) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }

        LambdaUpdateWrapper<Shop> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Shop::getId, ids)
                .set(Shop::getStatus, status);

        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new RuntimeException(operation + "搴楅摵澶辫触");
        }

        
        return true;
    }
}
