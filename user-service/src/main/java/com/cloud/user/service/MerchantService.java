package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.user.module.entity.Merchant;

/**
 * @author what's up
 * @description 针对表【merchant(商家表)】的数据库操作Service
 * @createDate 2025-09-06 19:31:12
 */
public interface MerchantService extends IService<Merchant> {

    /**
     * 根据ID获取商家信息(带缓存)
     *
     * @param id 商家ID
     * @return 商家信息DTO
     */
    MerchantDTO getMerchantByIdWithCache(Long id);

    /**
     * 根据商家名称获取商家信息(带缓存)
     *
     * @param merchantName 商家名称
     * @return 商家信息DTO
     */
    MerchantDTO getMerchantByNameWithCache(String merchantName);

}