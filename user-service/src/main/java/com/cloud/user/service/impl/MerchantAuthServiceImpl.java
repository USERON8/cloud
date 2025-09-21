package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.user.annotation.MultiLevelCacheEvict;
import com.cloud.user.annotation.MultiLevelCachePut;
import com.cloud.user.annotation.MultiLevelCacheable;
import com.cloud.user.annotation.MultiLevelCaching;
import com.cloud.user.converter.MerchantAuthConverter;
import com.cloud.user.mapper.MerchantAuthMapper;
import com.cloud.user.module.entity.MerchantAuth;
import com.cloud.user.service.MerchantAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @author what's up
 * @description 针对表【merchant_auth(商家认证表)】的数据库操作Service实现
 * @createDate 2025-09-06 19:31:12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantAuthServiceImpl extends ServiceImpl<MerchantAuthMapper, MerchantAuth>
        implements MerchantAuthService {

    // 商家认证缓存名称
    private static final String MERCHANT_AUTH_CACHE_NAME = "merchantAuthCache";
    private final MerchantAuthMapper merchantAuthMapper;
    private final MerchantAuthConverter merchantAuthConverter;

    public MerchantAuthDTO getMerchantAuthById(Long id) {
        return getMerchantAuthByIdWithCache(id);
    }

    /**
     * 根据ID获取商家认证信息(带缓存)
     *
     * @param id 认证ID
     * @return 商家认证信息DTO
     */
    @Transactional(readOnly = true)
    @MultiLevelCacheable(
            cacheName = MERCHANT_AUTH_CACHE_NAME,
            key = "'id:' + #id",
            expire = 1800,
            unless = "#result == null"
    )
    public MerchantAuthDTO getMerchantAuthByIdWithCache(Long id) {
        MerchantAuth merchantAuth = merchantAuthMapper.selectById(id);
        return merchantAuth != null ? merchantAuthConverter.toDTO(merchantAuth) : null;
    }

    /**
     * 根据商家ID获取商家认证信息(带缓存)
     *
     * @param merchantId 商家ID
     * @return 商家认证信息DTO
     */
    @Transactional(readOnly = true)
    @MultiLevelCacheable(
            cacheName = MERCHANT_AUTH_CACHE_NAME,
            key = "'merchantId:' + #merchantId",
            expire = 1800,
            unless = "#result == null"
    )
    public MerchantAuthDTO getMerchantAuthByMerchantIdWithCache(Long merchantId) {
        MerchantAuth merchantAuth = lambdaQuery().eq(MerchantAuth::getMerchantId, merchantId).one();
        return merchantAuth != null ? merchantAuthConverter.toDTO(merchantAuth) : null;
    }

    /**
     * 更新商家认证信息
     *
     * @param merchantAuth 商家认证实体
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCaching(
            evict = {
                    @MultiLevelCacheEvict(cacheName = MERCHANT_AUTH_CACHE_NAME, key = "'id:' + #merchantAuth.id"),
                    @MultiLevelCacheEvict(cacheName = MERCHANT_AUTH_CACHE_NAME, key = "'merchantId:' + #merchantAuth.merchantId")
            }
    )
    public boolean updateById(MerchantAuth merchantAuth) {
        merchantAuth.setUpdatedAt(LocalDateTime.now());
        return super.updateById(merchantAuth);
    }

    /**
     * 保存商家认证信息
     *
     * @param merchantAuth 商家认证实体
     * @return 保存结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCaching(
            put = {
                    @MultiLevelCachePut(cacheName = MERCHANT_AUTH_CACHE_NAME, key = "'id:' + #merchantAuth.id", expire = 1800),
                    @MultiLevelCachePut(cacheName = MERCHANT_AUTH_CACHE_NAME, key = "'merchantId:' + #merchantAuth.merchantId", expire = 1800)
            }
    )
    public boolean save(MerchantAuth merchantAuth) {
        merchantAuth.setCreatedAt(LocalDateTime.now());
        merchantAuth.setUpdatedAt(LocalDateTime.now());
        return super.save(merchantAuth);
    }

}