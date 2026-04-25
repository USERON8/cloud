package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantAuthRequestDTO;
import com.cloud.user.module.entity.MerchantAuth;
import java.util.List;

public interface MerchantAuthService extends IService<MerchantAuth> {

  MerchantAuthDTO getMerchantAuthByIdWithCache(Long id);

  MerchantAuthDTO getMerchantAuthByMerchantIdWithCache(Long merchantId);

  boolean removeByMerchantId(Long merchantId);

  MerchantAuthDTO applyForAuth(
      Long merchantId,
      MerchantAuthRequestDTO requestDTO,
      Integer authStatus,
      String businessLicenseUrl);

  boolean updateAuthStatus(Long merchantId, Integer authStatus, String remark);

  Page<MerchantAuthDTO> getMerchantAuthPage(Integer authStatus, Integer page, Integer size);

  boolean updateBusinessLicenseUrlIfExists(Long merchantId, String objectName);

  boolean updateIdCardFrontUrlIfExists(Long merchantId, String objectName);

  boolean updateIdCardBackUrlIfExists(Long merchantId, String objectName);

  int reviewAuthBatch(List<Long> merchantIds, Integer authStatus, String remark);
}
