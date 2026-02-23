package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.user.converter.MerchantAuthConverter;
import com.cloud.user.mapper.MerchantAuthMapper;
import com.cloud.user.module.entity.MerchantAuth;
import com.cloud.user.service.MerchantAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;






@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantAuthServiceImpl extends ServiceImpl<MerchantAuthMapper, MerchantAuth>
        implements MerchantAuthService {

    
    private static final String MERCHANT_AUTH_CACHE_NAME = "merchantAuthCache";
    private final MerchantAuthMapper merchantAuthMapper;
    private final MerchantAuthConverter merchantAuthConverter;

    public MerchantAuthDTO getMerchantAuthById(Long id) {
        return getMerchantAuthByIdWithCache(id);
    }

    





    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = MERCHANT_AUTH_CACHE_NAME,
            key = "'id:' + #id",
            unless = "#result == null"
    )
    public MerchantAuthDTO getMerchantAuthByIdWithCache(Long id) {
        MerchantAuth merchantAuth = merchantAuthMapper.selectById(id);
        return merchantAuth != null ? merchantAuthConverter.toDTO(merchantAuth) : null;
    }

    





    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = MERCHANT_AUTH_CACHE_NAME,
            key = "'merchantId:' + #merchantId",
            unless = "#result == null"
    )
    public MerchantAuthDTO getMerchantAuthByMerchantIdWithCache(Long merchantId) {
        MerchantAuth merchantAuth = lambdaQuery().eq(MerchantAuth::getMerchantId, merchantId).one();
        return merchantAuth != null ? merchantAuthConverter.toDTO(merchantAuth) : null;
    }

    





    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = MERCHANT_AUTH_CACHE_NAME, key = "'id:' + #merchantAuth.id"),
                    @CacheEvict(cacheNames = MERCHANT_AUTH_CACHE_NAME, key = "'merchantId:' + #merchantAuth.merchantId")
            }
    )
    public boolean updateById(MerchantAuth merchantAuth) {
        merchantAuth.setUpdatedAt(LocalDateTime.now());
        return super.updateById(merchantAuth);
    }

    





    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = MERCHANT_AUTH_CACHE_NAME, key = "'id:' + #merchantAuth.id"),
                    @CacheEvict(cacheNames = MERCHANT_AUTH_CACHE_NAME, key = "'merchantId:' + #merchantAuth.merchantId")
            }
    )
    public boolean save(MerchantAuth merchantAuth) {
        merchantAuth.setCreatedAt(LocalDateTime.now());
        merchantAuth.setUpdatedAt(LocalDateTime.now());
        return super.save(merchantAuth);
    }

}
