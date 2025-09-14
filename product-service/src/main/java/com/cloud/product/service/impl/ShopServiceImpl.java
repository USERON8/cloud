package com.cloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.cache.annotation.MultiLevelCacheEvict;
import com.cloud.common.cache.annotation.MultiLevelCachePut;
import com.cloud.common.cache.annotation.MultiLevelCacheable;
import com.cloud.common.cache.annotation.MultiLevelCaching;
import com.cloud.common.result.PageResult;
import com.cloud.product.module.dto.ShopPageDTO;
import com.cloud.product.module.dto.ShopRequestDTO;
import com.cloud.product.mapper.ShopMapper;
import com.cloud.product.module.entity.Shop;
import com.cloud.product.module.vo.ShopVO;
import com.cloud.product.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author what's up
 * @description 针对表【merchant_shop(商家店铺表)】的数据库操作Service实现
 * @createDate 2025-09-09 15:57:06
 */
@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop>
        implements ShopService {

    // 由于缺少ShopConverter和完整的DTO/VO，以下实现将使用最小化的方式
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createShop(ShopRequestDTO requestDTO) {
        // 简化实现：直接创建Shop实体
        Shop shop = new Shop();
        shop.setShopName(requestDTO.getShopName());
        shop.setDescription(requestDTO.getDescription());
        // 设置其他必要字段...
        
        boolean saved = save(shop);
        if (!saved) {
            throw new RuntimeException("创建店铺失败");
        }
        
        return shop.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateShop(Long id, ShopRequestDTO requestDTO) {
        Shop shop = getById(id);
        if (shop == null) {
            throw new RuntimeException("店铺不存在: " + id);
        }
        
        shop.setShopName(requestDTO.getShopName());
        shop.setDescription(requestDTO.getDescription());
        // 设置其他字段...
        
        return updateById(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteShop(Long id) {
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDeleteShops(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }
        return removeBatchByIds(ids);
    }

    @Override
    @Transactional(readOnly = true)
    @MultiLevelCacheable(value = "shopCache", key = "#id")
    public ShopVO getShopById(Long id) {
        Shop shop = getById(id);
        if (shop == null) {
            return null;
        }
        
        // 简单转换
        ShopVO shopVO = new ShopVO();
        shopVO.setId(shop.getId());
        shopVO.setShopName(shop.getShopName());
        shopVO.setDescription(shop.getDescription());
        // 设置其他字段...
        
        return shopVO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopVO> getShopsByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        
        List<Shop> shops = listByIds(ids);
        List<ShopVO> shopVOs = new ArrayList<>();
        
        for (Shop shop : shops) {
            ShopVO shopVO = new ShopVO();
            shopVO.setId(shop.getId());
            shopVO.setShopName(shop.getShopName());
            shopVO.setDescription(shop.getDescription());
            shopVOs.add(shopVO);
        }
        
        return shopVOs;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ShopVO> getShopsPage(ShopPageDTO pageDTO) {
        Page<Shop> page = new Page<>(pageDTO.getCurrent(), pageDTO.getSize());
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(pageDTO.getShopNameKeyword())) {
            queryWrapper.like(Shop::getShopName, pageDTO.getShopNameKeyword());
        }
        
        if (pageDTO.getStatus() != null) {
            queryWrapper.eq(Shop::getStatus, pageDTO.getStatus());
        }
        
        Page<Shop> shopPage = page(page, queryWrapper);
        List<ShopVO> shopVOs = new ArrayList<>();
        
        for (Shop shop : shopPage.getRecords()) {
            ShopVO shopVO = new ShopVO();
            shopVO.setId(shop.getId());
            shopVO.setShopName(shop.getShopName());
            shopVO.setDescription(shop.getDescription());
            shopVOs.add(shopVO);
        }
        
        return PageResult.of(shopVOs, shopPage.getTotal(), pageDTO.getCurrent(), pageDTO.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopVO> getShopsByMerchantId(Long merchantId, Integer status) {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getMerchantId, merchantId);
        if (status != null) {
            queryWrapper.eq(Shop::getStatus, status);
        }
        
        List<Shop> shops = list(queryWrapper);
        List<ShopVO> shopVOs = new ArrayList<>();
        
        for (Shop shop : shops) {
            ShopVO shopVO = new ShopVO();
            shopVO.setId(shop.getId());
            shopVO.setShopName(shop.getShopName());
            shopVO.setDescription(shop.getDescription());
            shopVOs.add(shopVO);
        }
        
        return shopVOs;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ShopVO> searchShopsByName(String shopName, Integer status) {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(shopName)) {
            queryWrapper.like(Shop::getShopName, shopName);
        }
        if (status != null) {
            queryWrapper.eq(Shop::getStatus, status);
        }
        
        List<Shop> shops = list(queryWrapper);
        List<ShopVO> shopVOs = new ArrayList<>();
        
        for (Shop shop : shops) {
            ShopVO shopVO = new ShopVO();
            shopVO.setId(shop.getId());
            shopVO.setShopName(shop.getShopName());
            shopVO.setDescription(shop.getDescription());
            shopVOs.add(shopVO);
        }
        
        return shopVOs;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean enableShop(Long id) {
        return updateShopStatus(id, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disableShop(Long id) {
        return updateShopStatus(id, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchEnableShops(List<Long> ids) {
        return batchUpdateShopStatus(ids, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDisableShops(List<Long> ids) {
        return batchUpdateShopStatus(ids, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalShopCount() {
        return count();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getEnabledShopCount() {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getStatus, 1);
        return count(queryWrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getDisabledShopCount() {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getStatus, 0);
        return count(queryWrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getShopCountByMerchantId(Long merchantId) {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getMerchantId, merchantId);
        return count(queryWrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean hasPermission(Long merchantId, Long shopId) {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getId, shopId)
                .eq(Shop::getMerchantId, merchantId);
        return count(queryWrapper) > 0;
    }

    @Override
    @MultiLevelCacheEvict(value = "shopCache", key = "#id")
    public void evictShopCache(Long id) {
        log.info("清除店铺缓存: {}", id);
    }

    @Override
    @MultiLevelCacheEvict(value = {"shopCache"}, allEntries = true)
    public void evictAllShopCache() {
        log.info("清除所有店铺缓存");
    }

    @Override
    public void warmupShopCache(List<Long> ids) {
        log.info("预热店铺缓存: {}", ids);
        
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        
        // 预热店铺缓存
        ids.forEach(this::getShopById);
        
        log.info("店铺缓存预热完成, 数量: {}", ids.size());
    }
    
    // 私有辅助方法
    private Boolean updateShopStatus(Long id, Integer status) {
        LambdaUpdateWrapper<Shop> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Shop::getId, id)
                .set(Shop::getStatus, status);
        return update(updateWrapper);
    }
    
    private Boolean batchUpdateShopStatus(List<Long> ids, Integer status) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }
        
        LambdaUpdateWrapper<Shop> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Shop::getId, ids)
                .set(Shop::getStatus, status);
        return update(updateWrapper);
    }
}
