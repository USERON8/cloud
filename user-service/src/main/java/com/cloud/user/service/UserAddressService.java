package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.user.module.entity.UserAddress;

import java.util.List;

/**
 * @author what's up
 * @description 针对表【user_address(用户地址表)】的数据库操作Service
 * @createDate 2025-08-20 12:35:31
 */
public interface UserAddressService extends IService<UserAddress> {
    /**
     * 根据用户ID获取用户地址列表
     *
     * @param userId 用户ID
     * @return 用户地址列表
     */
    List<UserAddress> getAddressByUserId(Long userId);

    /**
     * 根据地址ID获取用户地址详情
     *
     * @param addressId 地址ID
     * @return 用户地址详情
     */
    UserAddress getAddressById(Long addressId);


    /**
     * 逻辑删除用户地址
     *
     * @param id 地址ID
     * @return 是否删除成功
     */
    boolean removeById(Long id);


}