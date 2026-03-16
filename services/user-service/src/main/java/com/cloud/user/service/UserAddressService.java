package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.UserAddressDTO;
import com.cloud.common.domain.dto.user.UserAddressPageDTO;
import com.cloud.common.domain.dto.user.UserAddressRequestDTO;
import com.cloud.common.domain.vo.UserAddressVO;
import com.cloud.common.result.PageResult;
import com.cloud.user.module.entity.UserAddress;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

public interface UserAddressService extends IService<UserAddress> {

  UserAddressDTO getUserAddressByIdWithCache(Long id);

  UserAddressDTO getAddressById(Long id);

  List<UserAddressDTO> getUserAddressListByUserIdWithCache(Long userId);

  UserAddressDTO createAddress(Long userId, UserAddressRequestDTO requestDTO);

  UserAddressDTO updateAddress(Long addressId, UserAddressRequestDTO requestDTO);

  List<UserAddressVO> listAddressesByUserId(Long userId);

  UserAddressVO getDefaultAddress(Long userId);

  PageResult<UserAddressVO> pageAddresses(UserAddressPageDTO pageDTO);

  int deleteAddressBatch(List<Long> addressIds, Authentication authentication);

  int updateAddressBatch(List<UserAddressRequestDTO> addressList, Authentication authentication);

  @Transactional(rollbackFor = Exception.class)
  boolean removeById(Long id);

  @Transactional(rollbackFor = Exception.class)
  boolean resetDefaultAddress(Long userId);
}
