package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.user.converter.MerchantConverter;
import com.cloud.user.mapper.MerchantMapper;
import com.cloud.user.module.entity.Merchant;
import com.cloud.user.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @author what's up
 * @description 针对表【merchant(商家表)】的数据库操作Service实现
 * @createDate 2025-09-06 19:31:12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant>
        implements MerchantService {

    // 商家缓存名称
    private static final String MERCHANT_CACHE_NAME = "merchantCache";
    private final MerchantMapper merchantMapper;
    private final MerchantConverter merchantConverter;

    public MerchantDTO getMerchantById(Long id) {
        return getMerchantByIdWithCache(id);
    }

    /**
     * 根据ID获取商家信息(带缓存)
     *
     * @param id 商家ID
     * @return 商家信息DTO
     */
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = MERCHANT_CACHE_NAME,
            key = "'id:' + #id",
            unless = "#result == null"
    )
    public MerchantDTO getMerchantByIdWithCache(Long id) {
        Merchant merchant = merchantMapper.selectById(id);
        return merchant != null ? merchantConverter.toDTO(merchant) : null;
    }

    /**
     * 根据商家名称获取商家信息(带缓存)
     *
     * @param merchantName 商家名称
     * @return 商家信息DTO
     */
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = MERCHANT_CACHE_NAME,
            key = "'name:' + #merchantName",
            unless = "#result == null"
    )
    public MerchantDTO getMerchantByNameWithCache(String merchantName) {
        Merchant merchant = lambdaQuery().eq(Merchant::getMerchantName, merchantName).one();
        return merchant != null ? merchantConverter.toDTO(merchant) : null;
    }

    /**
     * 更新商家信息
     *
     * @param merchant 商家实体
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = MERCHANT_CACHE_NAME, key = "'id:' + #merchant.id"),
                    @CacheEvict(cacheNames = MERCHANT_CACHE_NAME, key = "'name:' + #merchant.merchantName", condition = "#merchant.merchantName != null")
            }
    )
    public boolean updateById(Merchant merchant) {
        // 更新数据库
        return super.updateById(merchant);
    }

    /**
     * 保存商家信息
     *
     * @param merchant 商家实体
     * @return 保存结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CachePut(
            cacheNames = MERCHANT_CACHE_NAME,
            key = "'id:' + #merchant.id"
    )
    public boolean save(Merchant merchant) {
        merchant.setCreatedAt(LocalDateTime.now());
        merchant.setUpdatedAt(LocalDateTime.now());
        // 保存到数据库
        return super.save(merchant);
    }

    /**
     * 删除商家信息
     *
     * @param id 商家ID
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = MERCHANT_CACHE_NAME,
            key = "'id:' + #id"
    )
    public boolean removeById(java.io.Serializable id) {
        return super.removeById(id);
    }

}
