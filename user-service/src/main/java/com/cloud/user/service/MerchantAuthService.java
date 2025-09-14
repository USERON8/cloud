package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.user.module.entity.MerchantAuth;

/**
 * @author what's up
 * @description 针对表【merchant_auth(商家认证表)】的数据库操作Service
 * @createDate 2025-09-06 19:31:12
 */
public interface MerchantAuthService extends IService<MerchantAuth> {

    /**
     * 根据ID获取商家认证信息(带缓存)
     *
     * @param id 认证ID
     * @return 商家认证信息DTO
     */
    MerchantAuthDTO getMerchantAuthByIdWithCache(Long id);

    /**
     * 根据商家ID获取商家认证信息(带缓存)
     *
     * @param merchantId 商家ID
     * @return 商家认证信息DTO
     */
    MerchantAuthDTO getMerchantAuthByMerchantIdWithCache(Long merchantId);

}