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
import com.cloud.product.converter.ShopConverter;
import com.cloud.product.mapper.ShopMapper;
import com.cloud.product.module.dto.ShopPageDTO;
import com.cloud.product.module.dto.ShopRequestDTO;
import com.cloud.product.module.entity.Shop;
import com.cloud.product.module.vo.ShopVO;
import com.cloud.product.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 店铺服务实现类
 * 针对表【merchant_shop(商家店铺表)】的数据库操作Service实现
 * 使用多级缓存提升性能，遵循事务管理规范
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop>
        implements ShopService {

    private final ShopConverter shopConverter;

    // ================= 基础CRUD操作 =================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCachePut(value = "shopCache", key = "#result",
            condition = "#result != null",
            expire = 60, timeUnit = TimeUnit.MINUTES)
    public Long createShop(ShopRequestDTO requestDTO) {
        log.info("创建店铺: {}", requestDTO.getName());

        // 使用ShopConverter转换DTO为实体
        Shop shop = shopConverter.requestDTOToEntity(requestDTO);

        boolean saved = save(shop);
        if (!saved) {
            throw new RuntimeException("创建店铺失败");
        }

        log.info("店铺创建成功, ID: {}", shop.getId());
        return shop.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCaching(
            put = @MultiLevelCachePut(value = "shopCache", key = "#id",
                    condition = "#result == true",
                    expire = 60, timeUnit = TimeUnit.MINUTES),
            evict = {
                    @MultiLevelCacheEvict(value = "shopListCache", allEntries = true)
            }
    )
    public Boolean updateShop(Long id, ShopRequestDTO requestDTO) {
        log.info("更新店铺: ID={}, Name={}", id, requestDTO.getName());

        Shop existingShop = getById(id);
        if (existingShop == null) {
            throw new RuntimeException("店铺不存在: " + id);
        }

        // 使用转换器更新实体字段
        Shop updateShop = shopConverter.requestDTOToEntity(requestDTO);
        updateShop.setId(id);

        boolean updated = updateById(updateShop);
        if (!updated) {
            throw new RuntimeException("更新店铺失败");
        }

        log.info("店铺更新成功: {}", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCaching(
            evict = {
                    @MultiLevelCacheEvict(value = "shopCache", key = "#id"),
                    @MultiLevelCacheEvict(value = "shopListCache", allEntries = true)
            }
    )
    public Boolean deleteShop(Long id) {
        log.info("删除店铺: {}", id);

        Shop shop = getById(id);
        if (shop == null) {
            throw new RuntimeException("店铺不存在: " + id);
        }

        boolean deleted = removeById(id);
        if (!deleted) {
            throw new RuntimeException("删除店铺失败");
        }

        log.info("店铺删除成功: {}", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCacheEvict(value = {"shopCache", "shopListCache"}, allEntries = true)
    public Boolean batchDeleteShops(List<Long> ids) {
        log.info("批量删除店铺: {}", ids);

        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }

        boolean deleted = removeBatchByIds(ids);
        if (!deleted) {
            throw new RuntimeException("批量删除店铺失败");
        }

        log.info("批量删除店铺成功, 数量: {}", ids.size());
        return true;
    }

    // ================= 查询操作 =================

    @Override
    @Transactional(readOnly = true)
    @MultiLevelCacheable(value = "shopCache", key = "#id",
            condition = "#id != null",
            expire = 60, timeUnit = TimeUnit.MINUTES)
    public ShopVO getShopById(Long id) {
        log.debug("获取店铺详情: {}", id);

        Shop shop = getById(id);
        if (shop == null) {
            throw new RuntimeException("店铺不存在: " + id);
        }

        return shopConverter.toVO(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopVO> getShopsByIds(List<Long> ids) {
        log.debug("批量获取店铺: {}", ids);
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }

        List<Shop> shops = listByIds(ids);
        return shopConverter.toVOList(shops);
    }

    @Override
    @Transactional(readOnly = true)
    @MultiLevelCacheable(value = "shopListCache",
            key = "'page:' + #pageDTO.current + ':' + #pageDTO.size + ':' + (#pageDTO.shopNameKeyword ?: 'null') + ':' + (#pageDTO.status ?: 'null')",
            expire = 30, timeUnit = TimeUnit.MINUTES)
    public PageResult<ShopVO> getShopsPage(ShopPageDTO pageDTO) {
        log.debug("分页查询店铺: {}", pageDTO);

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
    @MultiLevelCacheable(value = "shopListCache",
            key = "'merchant:' + #merchantId + ':' + (#status ?: 'null')",
            expire = 45, timeUnit = TimeUnit.MINUTES)
    public List<ShopVO> getShopsByMerchantId(Long merchantId, Integer status) {
        log.debug("根据商户ID查询店铺: merchantId={}, status={}", merchantId, status);

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
    @MultiLevelCacheable(value = "shopListCache",
            key = "'search:' + #shopName + ':' + (#status ?: 'null')",
            expire = 30, timeUnit = TimeUnit.MINUTES)
    public List<ShopVO> searchShopsByName(String shopName, Integer status) {
        log.debug("搜索店铺: shopName={}, status={}", shopName, status);

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

    // ================= 状态管理 =================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCaching(
            put = @MultiLevelCachePut(value = "shopCache", key = "#id"),
            evict = @MultiLevelCacheEvict(value = "shopListCache", allEntries = true)
    )
    public Boolean enableShop(Long id) {
        log.info("启用店铺: {}", id);
        return updateShopStatus(id, 1, "启用");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCaching(
            put = @MultiLevelCachePut(value = "shopCache", key = "#id"),
            evict = @MultiLevelCacheEvict(value = "shopListCache", allEntries = true)
    )
    public Boolean disableShop(Long id) {
        log.info("禁用店铺: {}", id);
        return updateShopStatus(id, 0, "禁用");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCacheEvict(value = {"shopCache", "shopListCache"}, allEntries = true)
    public Boolean batchEnableShops(List<Long> ids) {
        log.info("批量启用店铺: {}", ids);
        return batchUpdateShopStatus(ids, 1, "批量启用");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCacheEvict(value = {"shopCache", "shopListCache"}, allEntries = true)
    public Boolean batchDisableShops(List<Long> ids) {
        log.info("批量禁用店铺: {}", ids);
        return batchUpdateShopStatus(ids, 0, "批量禁用");
    }

    // ================= 统计分析 =================

    @Override
    @Transactional(readOnly = true)
    @MultiLevelCacheable(value = "shopCache", key = "'stats:total'",
            expire = 120, timeUnit = TimeUnit.MINUTES)
    public Long getTotalShopCount() {
        return count();
    }

    @Override
    @Transactional(readOnly = true)
    @MultiLevelCacheable(value = "shopCache", key = "'stats:enabled'",
            expire = 120, timeUnit = TimeUnit.MINUTES)
    public Long getEnabledShopCount() {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getStatus, 1);
        return count(queryWrapper);
    }

    @Override
    @Transactional(readOnly = true)
    @MultiLevelCacheable(value = "shopCache", key = "'stats:disabled'",
            expire = 120, timeUnit = TimeUnit.MINUTES)
    public Long getDisabledShopCount() {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getStatus, 0);
        return count(queryWrapper);
    }

    @Override
    @Transactional(readOnly = true)
    @MultiLevelCacheable(value = "shopCache", key = "'stats:merchant:' + #merchantId",
            expire = 60, timeUnit = TimeUnit.MINUTES)
    public Long getShopCountByMerchantId(Long merchantId) {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getMerchantId, merchantId);
        return count(queryWrapper);
    }

    @Override
    @Transactional(readOnly = true)
    @MultiLevelCacheable(value = "shopCache", key = "'permission:' + #merchantId + ':' + #shopId",
            expire = 30, timeUnit = TimeUnit.MINUTES)
    public Boolean hasPermission(Long merchantId, Long shopId) {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getId, shopId)
                .eq(Shop::getMerchantId, merchantId);
        return count(queryWrapper) > 0;
    }

    // ================= 缓存管理 =================

    @Override
    @MultiLevelCacheEvict(value = "shopCache", key = "#id")
    public void evictShopCache(Long id) {
        log.info("清除店铺缓存: {}", id);
    }

    @Override
    @MultiLevelCacheEvict(value = {"shopCache", "shopListCache"}, allEntries = true)
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

    // ================= 私有辅助方法 =================

    /**
     * 更新店铺状态
     */
    private Boolean updateShopStatus(Long id, Integer status, String operation) {
        Shop shop = getById(id);
        if (shop == null) {
            throw new RuntimeException("店铺不存在: " + id);
        }

        LambdaUpdateWrapper<Shop> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Shop::getId, id)
                .set(Shop::getStatus, status);

        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new RuntimeException(operation + "店铺失败");
        }

        log.info("{}店铺成功: {}", operation, id);
        return true;
    }

    /**
     * 批量更新店铺状态
     */
    private Boolean batchUpdateShopStatus(List<Long> ids, Integer status, String operation) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }

        LambdaUpdateWrapper<Shop> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Shop::getId, ids)
                .set(Shop::getStatus, status);

        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new RuntimeException(operation + "店铺失败");
        }

        log.info("{}店铺成功, 数量: {}", operation, ids.size());
        return true;
    }
}
