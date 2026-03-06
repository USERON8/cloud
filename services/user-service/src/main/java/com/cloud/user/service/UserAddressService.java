package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.UserAddressDTO;
import com.cloud.user.module.entity.UserAddress;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;






public interface UserAddressService extends IService<UserAddress> {

    





    UserAddressDTO getUserAddressByIdWithCache(Long id);

    





    List<UserAddressDTO> getUserAddressListByUserIdWithCache(Long userId);

    @Transactional(rollbackFor = Exception.class)
    boolean removeById(Long id);
}
