package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.user.module.entity.MerchantAuth;






public interface MerchantAuthService extends IService<MerchantAuth> {

    





    MerchantAuthDTO getMerchantAuthByIdWithCache(Long id);

    





    MerchantAuthDTO getMerchantAuthByMerchantIdWithCache(Long merchantId);

}
