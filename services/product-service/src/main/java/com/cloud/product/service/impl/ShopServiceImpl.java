package com.cloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.exception.BusinessException;
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
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    private final ShopConverter shopConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"shopCache", "shopListCache"}, allEntries = true)
    public Long createShop(ShopRequestDTO requestDTO) {
        validateShopRequest(requestDTO, true);

        Shop shop = shopConverter.requestDTOToEntity(requestDTO);
        if (shop.getStatus() == null) {
            shop.setStatus(1);
        }

        boolean saved = save(shop);
        if (!saved) {
            throw new BusinessException("Create shop failed");
        }
        return shop.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "shopCache", key = "#id"),
            @CacheEvict(cacheNames = "shopListCache", allEntries = true)
    })
    public Boolean updateShop(Long id, ShopRequestDTO requestDTO) {
        validateId(id);
        validateShopRequest(requestDTO, false);

        Shop existingShop = requireExistingShop(id);
        applyRequest(existingShop, requestDTO);

        boolean updated = updateById(existingShop);
        if (!updated) {
            throw new BusinessException("Update shop failed");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "shopCache", key = "#id"),
            @CacheEvict(cacheNames = "shopListCache", allEntries = true)
    })
    public Boolean deleteShop(Long id) {
        validateId(id);
        requireExistingShop(id);

        boolean deleted = removeById(id);
        if (!deleted) {
            throw new BusinessException("Delete shop failed");
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
            throw new BusinessException("Batch delete shops failed");
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopCache", key = "#id", condition = "#id != null")
    public ShopVO getShopById(Long id) {
        validateId(id);
        log.debug("Query shop by id: {}", id);
        return shopConverter.toVO(requireExistingShop(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopVO> getShopsByIds(List<Long> ids) {
        log.debug("Batch query shops: ids={}", ids);
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        return shopConverter.toVOList(listByIds(ids));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "shopListCache",
            key = "'page:' + #pageDTO.current + ':' + #pageDTO.size + ':' + (#pageDTO.shopNameKeyword ?: 'null') + ':' + (#pageDTO.status ?: 'null')"
    )
    public PageResult<ShopVO> getShopsPage(ShopPageDTO pageDTO) {
        ShopPageDTO request = pageDTO == null ? new ShopPageDTO() : pageDTO;
        long current = request.getCurrent() == null || request.getCurrent() <= 0 ? 1L : request.getCurrent();
        long size = request.getSize() == null || request.getSize() <= 0 ? 20L : request.getSize();
        log.debug("Query shops by page: current={}, size={}, keyword={}, status={}",
                current, size, request.getShopNameKeyword(), request.getStatus());

        Page<Shop> page = new Page<>(current, size);
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getShopNameKeyword())) {
            queryWrapper.like(Shop::getShopName, request.getShopNameKeyword());
        }
        if (request.getStatus() != null) {
            queryWrapper.eq(Shop::getStatus, request.getStatus());
        }
        queryWrapper.orderByDesc(Shop::getCreatedAt);

        Page<Shop> shopPage = page(page, queryWrapper);
        List<ShopVO> shopVOs = shopConverter.toVOList(shopPage.getRecords());
        return PageResult.of(shopVOs, shopPage.getTotal(), shopPage.getCurrent(), shopPage.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopListCache", key = "'merchant:' + #merchantId + ':' + (#status ?: 'null')")
    public List<ShopVO> getShopsByMerchantId(Long merchantId, Integer status) {
        log.debug("Query shops by merchant: merchantId={}, status={}", merchantId, status);
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getMerchantId, merchantId);
        if (status != null) {
            queryWrapper.eq(Shop::getStatus, status);
        }
        queryWrapper.orderByDesc(Shop::getCreatedAt);
        return shopConverter.toVOList(list(queryWrapper));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopListCache", key = "'search:' + #shopName + ':' + (#status ?: 'null')")
    public List<ShopVO> searchShopsByName(String shopName, Integer status) {
        log.debug("Search shops by name: shopName={}, status={}", shopName, status);
        if (!StringUtils.hasText(shopName)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Shop::getShopName, shopName);
        if (status != null) {
            queryWrapper.eq(Shop::getStatus, status);
        }
        queryWrapper.orderByDesc(Shop::getCreatedAt);
        return shopConverter.toVOList(list(queryWrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "shopCache", key = "#id"),
            @CacheEvict(cacheNames = "shopListCache", allEntries = true)
    })
    public Boolean enableShop(Long id) {
        return updateShopStatus(id, 1, "Enable");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "shopCache", key = "#id"),
            @CacheEvict(cacheNames = "shopListCache", allEntries = true)
    })
    public Boolean disableShop(Long id) {
        return updateShopStatus(id, 0, "Disable");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"shopCache", "shopListCache"}, allEntries = true)
    public Boolean batchEnableShops(List<Long> ids) {
        return batchUpdateShopStatus(ids, 1, "Batch enable");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"shopCache", "shopListCache"}, allEntries = true)
    public Boolean batchDisableShops(List<Long> ids) {
        return batchUpdateShopStatus(ids, 0, "Batch disable");
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
        return count(new LambdaQueryWrapper<Shop>().eq(Shop::getStatus, 1));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopCache", key = "'stats:disabled'")
    public Long getDisabledShopCount() {
        return count(new LambdaQueryWrapper<Shop>().eq(Shop::getStatus, 0));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopCache", key = "'stats:merchant:' + #merchantId")
    public Long getShopCountByMerchantId(Long merchantId) {
        return count(new LambdaQueryWrapper<Shop>().eq(Shop::getMerchantId, merchantId));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopCache", key = "'permission:' + #merchantId + ':' + #shopId")
    public Boolean hasPermission(Long merchantId, Long shopId) {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getId, shopId).eq(Shop::getMerchantId, merchantId);
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
        for (Long id : ids) {
            try {
                getShopById(id);
            } catch (Exception e) {
                log.warn("Warmup shop cache failed: id={}", id, e);
            }
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("Invalid shop id");
        }
    }

    private Shop requireExistingShop(Long id) {
        Shop shop = getById(id);
        if (shop == null) {
            throw new BusinessException("Shop not found: " + id);
        }
        return shop;
    }

    private void validateShopRequest(ShopRequestDTO requestDTO, boolean create) {
        if (requestDTO == null) {
            throw new BusinessException("Shop payload cannot be null");
        }
        if (create && requestDTO.getMerchantId() == null) {
            throw new BusinessException("Merchant id is required");
        }
        if (create && !StringUtils.hasText(requestDTO.getShopName())) {
            throw new BusinessException("Shop name cannot be blank");
        }
        if (create && !StringUtils.hasText(requestDTO.getContactPhone())) {
            throw new BusinessException("Contact phone cannot be blank");
        }
        if (create && !StringUtils.hasText(requestDTO.getAddress())) {
            throw new BusinessException("Address cannot be blank");
        }
        if (requestDTO.getStatus() != null && requestDTO.getStatus() != 0 && requestDTO.getStatus() != 1) {
            throw new BusinessException("Shop status must be 0 or 1");
        }
    }

    private void applyRequest(Shop shop, ShopRequestDTO requestDTO) {
        if (StringUtils.hasText(requestDTO.getShopName())) {
            shop.setShopName(requestDTO.getShopName());
        }
        if (StringUtils.hasText(requestDTO.getAvatarUrl())) {
            shop.setAvatarUrl(requestDTO.getAvatarUrl());
        }
        if (StringUtils.hasText(requestDTO.getDescription())) {
            shop.setDescription(requestDTO.getDescription());
        }
        if (StringUtils.hasText(requestDTO.getContactPhone())) {
            shop.setContactPhone(requestDTO.getContactPhone());
        }
        if (StringUtils.hasText(requestDTO.getAddress())) {
            shop.setAddress(requestDTO.getAddress());
        }
        if (requestDTO.getStatus() != null) {
            shop.setStatus(requestDTO.getStatus());
        }
    }

    private Boolean updateShopStatus(Long id, Integer status, String operation) {
        requireExistingShop(id);
        LambdaUpdateWrapper<Shop> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Shop::getId, id).set(Shop::getStatus, status);
        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new BusinessException(operation + " shop failed");
        }
        return true;
    }

    private Boolean batchUpdateShopStatus(List<Long> ids, Integer status, String operation) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }
        LambdaUpdateWrapper<Shop> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Shop::getId, ids).set(Shop::getStatus, status);
        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new BusinessException(operation + " shops failed");
        }
        return true;
    }
}
