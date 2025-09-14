package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.UserAddressDTO;
import com.cloud.user.module.entity.UserAddress;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author what's up
 * @description 针对表【user_address(用户地址表)】的数据库操作Service
 * @createDate 2025-09-06 19:31:12
 */
public interface UserAddressService extends IService<UserAddress> {

    /**
     * 根据ID获取用户地址详情(带缓存)
     *
     * @param id 地址ID
     * @return 用户地址DTO
     */
    UserAddressDTO getUserAddressByIdWithCache(Long id);

    /**
     * 根据用户ID获取用户地址列表(带缓存)
     *
     * @param userId 用户ID
     * @return 用户地址DTO列表
     */
    List<UserAddressDTO> getUserAddressListByUserIdWithCache(Long userId);

    @Transactional(rollbackFor = Exception.class)
    boolean removeById(Long id);
}