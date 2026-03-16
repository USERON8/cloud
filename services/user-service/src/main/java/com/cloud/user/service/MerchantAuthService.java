package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantAuthRequestDTO;
import com.cloud.user.module.entity.MerchantAuth;

import java.util.List;






public interface MerchantAuthService extends IService<MerchantAuth> {







    MerchantAuthDTO getMerchantAuthByIdWithCache(Long id);







    MerchantAuthDTO getMerchantAuthByMerchantIdWithCache(Long merchantId);

    boolean removeByMerchantId(Long merchantId);

    MerchantAuthDTO applyForAuth(Long merchantId, MerchantAuthRequestDTO requestDTO, Integer authStatus, String businessLicenseUrl);

    boolean updateAuthStatus(Long merchantId, Integer authStatus, String remark);

    List<MerchantAuthDTO> listByAuthStatus(Integer authStatus, int limit);

    boolean updateBusinessLicenseUrlIfExists(Long merchantId, String objectName);

}
