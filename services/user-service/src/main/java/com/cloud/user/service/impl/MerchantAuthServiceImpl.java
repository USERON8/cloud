package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantAuthRequestDTO;
import com.cloud.user.converter.MerchantAuthConverter;
import com.cloud.user.mapper.MerchantAuthMapper;
import com.cloud.user.module.entity.MerchantAuth;
import com.cloud.user.service.MerchantAuthService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantAuthServiceImpl extends ServiceImpl<MerchantAuthMapper, MerchantAuth>
    implements MerchantAuthService {

  private static final String MERCHANT_AUTH_CACHE_NAME = "merchantAuthCache";
  private final MerchantAuthMapper merchantAuthMapper;
  private final MerchantAuthConverter merchantAuthConverter;
  private final CacheManager cacheManager;

  public MerchantAuthDTO getMerchantAuthById(Long id) {
    return getMerchantAuthByIdWithCache(id);
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = MERCHANT_AUTH_CACHE_NAME, key = "'id:' + #id", unless = "#result == null")
  public MerchantAuthDTO getMerchantAuthByIdWithCache(Long id) {
    MerchantAuth merchantAuth = merchantAuthMapper.selectById(id);
    return merchantAuth != null ? merchantAuthConverter.toDTO(merchantAuth) : null;
  }

  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = MERCHANT_AUTH_CACHE_NAME,
      key = "'merchantId:' + #merchantId",
      unless = "#result == null")
  public MerchantAuthDTO getMerchantAuthByMerchantIdWithCache(Long merchantId) {
    MerchantAuth merchantAuth = lambdaQuery().eq(MerchantAuth::getMerchantId, merchantId).one();
    return merchantAuth != null ? merchantAuthConverter.toDTO(merchantAuth) : null;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean removeByMerchantId(Long merchantId) {
    if (merchantId == null) {
      return false;
    }
    MerchantAuth merchantAuth = lambdaQuery().eq(MerchantAuth::getMerchantId, merchantId).one();
    if (merchantAuth == null) {
      return false;
    }
    boolean removed = super.removeById(merchantAuth.getId());
    if (removed) {
      Cache cache = cacheManager.getCache(MERCHANT_AUTH_CACHE_NAME);
      if (cache != null) {
        cache.evict("id:" + merchantAuth.getId());
        cache.evict("merchantId:" + merchantId);
      }
    }
    return removed;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  @Caching(
      evict = {
        @CacheEvict(cacheNames = MERCHANT_AUTH_CACHE_NAME, key = "'id:' + #merchantAuth.id"),
        @CacheEvict(
            cacheNames = MERCHANT_AUTH_CACHE_NAME,
            key = "'merchantId:' + #merchantAuth.merchantId")
      })
  public boolean updateById(MerchantAuth merchantAuth) {
    merchantAuth.setUpdatedAt(LocalDateTime.now());
    return super.updateById(merchantAuth);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  @Caching(
      evict = {
        @CacheEvict(cacheNames = MERCHANT_AUTH_CACHE_NAME, key = "'id:' + #merchantAuth.id"),
        @CacheEvict(
            cacheNames = MERCHANT_AUTH_CACHE_NAME,
            key = "'merchantId:' + #merchantAuth.merchantId")
      })
  public boolean save(MerchantAuth merchantAuth) {
    merchantAuth.setCreatedAt(LocalDateTime.now());
    merchantAuth.setUpdatedAt(LocalDateTime.now());
    return super.save(merchantAuth);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public MerchantAuthDTO applyForAuth(
      Long merchantId,
      MerchantAuthRequestDTO requestDTO,
      Integer authStatus,
      String businessLicenseUrl) {
    if (merchantId == null || requestDTO == null) {
      log.warn("Failed to apply merchant auth due to missing merchantId or request");
      return null;
    }
    MerchantAuth merchantAuth = merchantAuthConverter.toEntity(requestDTO);
    if (merchantAuth == null) {
      log.warn(
          "Failed to apply merchant auth because conversion failed, merchantId={}", merchantId);
      return null;
    }
    merchantAuth.setMerchantId(merchantId);
    merchantAuth.setAuthStatus(authStatus);
    merchantAuth.setBusinessLicenseUrl(businessLicenseUrl);

    MerchantAuth existingAuth = lambdaQuery().eq(MerchantAuth::getMerchantId, merchantId).one();
    if (existingAuth != null) {
      merchantAuth.setId(existingAuth.getId());
      merchantAuth.setCreatedAt(existingAuth.getCreatedAt());
      if (!updateById(merchantAuth)) {
        log.error("Failed to update merchant auth application, merchantId={}", merchantId);
        return null;
      }
      return merchantAuthConverter.toDTO(merchantAuth);
    }

    if (!save(merchantAuth)) {
      log.error("Failed to create merchant auth application, merchantId={}", merchantId);
      return null;
    }
    return merchantAuthConverter.toDTO(merchantAuth);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean updateAuthStatus(Long merchantId, Integer authStatus, String remark) {
    if (merchantId == null || authStatus == null) {
      return false;
    }
    MerchantAuth merchantAuth = lambdaQuery().eq(MerchantAuth::getMerchantId, merchantId).one();
    if (merchantAuth == null) {
      return false;
    }
    merchantAuth.setAuthStatus(authStatus);
    merchantAuth.setAuthRemark(remark);
    return updateById(merchantAuth);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MerchantAuthDTO> listByAuthStatus(Integer authStatus, int limit) {
    if (authStatus == null) {
      return Collections.emptyList();
    }
    int effectiveLimit = limit <= 0 ? 200 : limit;
    List<MerchantAuth> list =
        lambdaQuery()
            .eq(MerchantAuth::getAuthStatus, authStatus)
            .last("LIMIT " + effectiveLimit)
            .list();
    if (list == null || list.isEmpty()) {
      return List.of();
    }
    return list.stream().map(merchantAuthConverter::toDTO).toList();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean updateBusinessLicenseUrlIfExists(Long merchantId, String objectName) {
    if (merchantId == null || objectName == null || objectName.isBlank()) {
      return false;
    }
    MerchantAuth merchantAuth = lambdaQuery().eq(MerchantAuth::getMerchantId, merchantId).one();
    if (merchantAuth == null) {
      return false;
    }
    merchantAuth.setBusinessLicenseUrl(objectName);
    return updateById(merchantAuth);
  }
}
