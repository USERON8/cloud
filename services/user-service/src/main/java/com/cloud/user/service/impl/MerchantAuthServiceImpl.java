package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantAuthRequestDTO;
import com.cloud.user.converter.MerchantAuthConverter;
import com.cloud.user.mapper.MerchantAuthMapper;
import com.cloud.user.module.entity.MerchantAuth;
import com.cloud.user.service.MerchantAuthService;
import com.cloud.user.service.MerchantService;
import com.cloud.user.service.cache.TransactionalMerchantAuthCacheService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final TransactionalMerchantAuthCacheService merchantAuthCacheService;
  private final MerchantService merchantService;

  public MerchantAuthDTO getMerchantAuthById(Long id) {
    return getMerchantAuthByIdWithCache(id);
  }

  @Transactional(readOnly = true)
  public MerchantAuthDTO getMerchantAuthByIdWithCache(Long id) {
    TransactionalMerchantAuthCacheService.MerchantAuthCache cached =
        merchantAuthCacheService.getById(id);
    if (cached != null) {
      return toDTO(cached);
    }
    MerchantAuth merchantAuth = merchantAuthMapper.selectById(id);
    if (merchantAuth == null) {
      return null;
    }
    merchantAuthCacheService.putTransactional(merchantAuth);
    return merchantAuthConverter.toDTO(merchantAuth);
  }

  @Transactional(readOnly = true)
  public MerchantAuthDTO getMerchantAuthByMerchantIdWithCache(Long merchantId) {
    TransactionalMerchantAuthCacheService.MerchantAuthCache cached =
        merchantAuthCacheService.getByMerchantId(merchantId);
    if (cached != null) {
      return toDTO(cached);
    }
    MerchantAuth merchantAuth = lambdaQuery().eq(MerchantAuth::getMerchantId, merchantId).one();
    if (merchantAuth == null) {
      return null;
    }
    merchantAuthCacheService.putTransactional(merchantAuth);
    return merchantAuthConverter.toDTO(merchantAuth);
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
      merchantAuthCacheService.evictTransactional(merchantAuth.getId(), merchantId);
    }
    return removed;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean updateById(MerchantAuth merchantAuth) {
    merchantAuth.setUpdatedAt(LocalDateTime.now());
    boolean updated = super.updateById(merchantAuth);
    if (updated) {
      merchantAuthCacheService.putTransactional(merchantAuth);
    }
    return updated;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean save(MerchantAuth merchantAuth) {
    merchantAuth.setCreatedAt(LocalDateTime.now());
    merchantAuth.setUpdatedAt(LocalDateTime.now());
    boolean saved = super.save(merchantAuth);
    if (saved) {
      merchantAuthCacheService.putTransactional(merchantAuth);
    }
    return saved;
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
  public Page<MerchantAuthDTO> getMerchantAuthPage(Integer authStatus, Integer page, Integer size) {
    if (authStatus == null) {
      return new Page<>();
    }
    long safePage = page == null || page < 1 ? 1L : page.longValue();
    long safeSize = size == null || size < 1 ? 20L : size.longValue();
    Page<MerchantAuth> pageParam = new Page<>(safePage, safeSize);
    Page<MerchantAuth> merchantAuthPage =
        lambdaQuery()
            .eq(MerchantAuth::getAuthStatus, authStatus)
            .orderByDesc(MerchantAuth::getCreatedAt)
            .page(pageParam);
    Page<MerchantAuthDTO> dtoPage =
        new Page<>(
            merchantAuthPage.getCurrent(), merchantAuthPage.getSize(), merchantAuthPage.getTotal());
    List<MerchantAuth> records = merchantAuthPage.getRecords();
    if (records == null || records.isEmpty()) {
      dtoPage.setRecords(List.of());
      return dtoPage;
    }
    dtoPage.setRecords(records.stream().map(merchantAuthConverter::toDTO).toList());
    return dtoPage;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean updateBusinessLicenseUrlIfExists(Long merchantId, String objectName) {
    return updateAuthDocumentUrlIfExists(
        merchantId, objectName, merchantAuth -> merchantAuth.setBusinessLicenseUrl(objectName));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean updateIdCardFrontUrlIfExists(Long merchantId, String objectName) {
    return updateAuthDocumentUrlIfExists(
        merchantId, objectName, merchantAuth -> merchantAuth.setIdCardFrontUrl(objectName));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean updateIdCardBackUrlIfExists(Long merchantId, String objectName) {
    return updateAuthDocumentUrlIfExists(
        merchantId, objectName, merchantAuth -> merchantAuth.setIdCardBackUrl(objectName));
  }

  private boolean updateAuthDocumentUrlIfExists(
      Long merchantId,
      String objectName,
      java.util.function.Consumer<MerchantAuth> documentUpdater) {
    if (merchantId == null || objectName == null || objectName.isBlank()) {
      return false;
    }
    MerchantAuth merchantAuth = lambdaQuery().eq(MerchantAuth::getMerchantId, merchantId).one();
    if (merchantAuth == null) {
      return false;
    }
    documentUpdater.accept(merchantAuth);
    return updateById(merchantAuth);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int reviewAuthBatch(List<Long> merchantIds, Integer authStatus, String remark) {
    if (merchantIds == null || merchantIds.isEmpty() || authStatus == null) {
      return 0;
    }
    int successCount = 0;
    for (Long merchantId : merchantIds) {
      if (merchantId == null) {
        continue;
      }
      try {
        if (merchantService.getById(merchantId) == null) {
          continue;
        }
        if (updateAuthStatus(merchantId, authStatus, remark)
            && merchantService.updateMerchantAuditStatus(merchantId, authStatus)) {
          successCount++;
        }
      } catch (Exception e) {
        log.error("Failed to review merchant auth, merchantId={}", merchantId, e);
      }
    }
    return successCount;
  }

  private MerchantAuthDTO toDTO(TransactionalMerchantAuthCacheService.MerchantAuthCache cached) {
    return merchantAuthConverter.toDTO(cached);
  }
}
