package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.user.UserAddressDTO;
import com.cloud.common.domain.dto.user.UserAddressPageDTO;
import com.cloud.common.domain.dto.user.UserAddressRequestDTO;
import com.cloud.common.domain.vo.UserAddressVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.common.utils.PageUtils;
import com.cloud.user.converter.UserAddressConverter;
import com.cloud.user.mapper.UserAddressMapper;
import com.cloud.user.module.entity.UserAddress;
import com.cloud.user.service.UserAddressService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress>
    implements UserAddressService {

  private static final String USER_ADDRESS_CACHE_NAME = "userAddressCache";
  private final UserAddressMapper userAddressMapper;
  private final UserAddressConverter userAddressConverter;
  private final CacheManager cacheManager;

  @Override
  @Transactional(rollbackFor = Exception.class)
  @Caching(
      evict = {
        @CacheEvict(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'detail:' + #entity.id"),
        @CacheEvict(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'list:' + #entity.userId")
      })
  public boolean save(UserAddress entity) {
    if (entity == null) {
      throw new BusinessException("user address is required");
    }
    entity.setCreatedAt(LocalDateTime.now());
    entity.setUpdatedAt(LocalDateTime.now());

    if (Integer.valueOf(1).equals(entity.getIsDefault())) {
      resetDefaultAddress(entity.getUserId());
    }

    boolean saved = super.save(entity);
    if (!saved) {
      log.error("Failed to save user address");
      throw new BusinessException(500, "Failed to save user address");
    }
    return true;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  @Caching(
      evict = {
        @CacheEvict(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'detail:' + #entity.id"),
        @CacheEvict(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'list:' + #entity.userId")
      })
  public boolean updateById(UserAddress entity) {
    if (entity == null || entity.getId() == null) {
      throw new BusinessException("address id is required");
    }
    entity.setUpdatedAt(LocalDateTime.now());

    UserAddress existingAddress = this.getById(entity.getId());
    if (existingAddress == null) {
      log.warn(
          "Failed to update user address because it does not exist, addressId={}", entity.getId());
      throw new ResourceNotFoundException("address", String.valueOf(entity.getId()));
    }

    Long userId = entity.getUserId();
    if (userId == null) {
      userId = existingAddress.getUserId();
      entity.setUserId(userId);
    }

    if (!existingAddress.getUserId().equals(userId)) {
      log.warn(
          "Failed to update user address due to permission mismatch, addressId={}, userId={}",
          entity.getId(),
          userId);
      throw new BusinessException("No permission to operate this address");
    }

    if (Integer.valueOf(1).equals(entity.getIsDefault())) {
      resetDefaultAddress(userId);
    }

    boolean updated = super.updateById(entity);
    if (!updated) {
      log.error("Failed to update user address, addressId={}", entity.getId());
      throw new BusinessException(500, "Failed to update user address");
    }
    return true;
  }

  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = USER_ADDRESS_CACHE_NAME,
      key = "'detail:' + #id",
      unless = "#result == null")
  public UserAddressDTO getUserAddressByIdWithCache(Long id) {
    UserAddress userAddress = userAddressMapper.selectById(id);
    return userAddress != null ? userAddressConverter.toDTO(userAddress) : null;
  }

  @Override
  @Transactional(readOnly = true)
  public UserAddressDTO getAddressById(Long id) {
    if (id == null) {
      return null;
    }
    return getUserAddressByIdWithCache(id);
  }

  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = USER_ADDRESS_CACHE_NAME,
      key = "'list:' + #userId",
      unless = "#result == null || #result.isEmpty()")
  public List<UserAddressDTO> getUserAddressListByUserIdWithCache(Long userId) {
    List<UserAddress> userAddresses = lambdaQuery().eq(UserAddress::getUserId, userId).list();
    return userAddressConverter.toDTOList(userAddresses);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public UserAddressDTO createAddress(Long userId, UserAddressRequestDTO requestDTO) {
    if (userId == null) {
      throw new BusinessException("user id is required");
    }
    if (requestDTO == null) {
      throw new BusinessException("address payload is required");
    }
    UserAddress entity = userAddressConverter.toEntity(requestDTO);
    if (entity == null) {
      throw new BusinessException("invalid address payload");
    }
    entity.setUserId(userId);
    save(entity);
    return userAddressConverter.toDTO(entity);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public UserAddressDTO updateAddress(Long addressId, UserAddressRequestDTO requestDTO) {
    if (addressId == null) {
      throw new BusinessException("address id is required");
    }
    if (requestDTO == null) {
      throw new BusinessException("address payload is required");
    }
    UserAddress entity = userAddressConverter.toEntity(requestDTO);
    if (entity == null) {
      throw new BusinessException("invalid address payload");
    }
    entity.setId(addressId);
    updateById(entity);
    return userAddressConverter.toDTO(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserAddressVO> listAddressesByUserId(Long userId) {
    if (userId == null) {
      return List.of();
    }
    List<UserAddress> userAddresses =
        lambdaQuery()
            .eq(UserAddress::getUserId, userId)
            .orderByDesc(UserAddress::getIsDefault)
            .orderByDesc(UserAddress::getUpdatedAt)
            .list();
    return userAddressConverter.toVOList(userAddresses);
  }

  @Override
  @Transactional(readOnly = true)
  public UserAddressVO getDefaultAddress(Long userId) {
    if (userId == null) {
      return null;
    }
    UserAddress userAddress =
        lambdaQuery().eq(UserAddress::getUserId, userId).eq(UserAddress::getIsDefault, 1).one();
    return userAddress == null ? null : userAddressConverter.toVO(userAddress);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<UserAddressVO> pageAddresses(UserAddressPageDTO pageDTO) {
    if (pageDTO == null) {
      throw new BusinessException("page query payload is required");
    }
    Page<UserAddress> page = PageUtils.buildPage(pageDTO);
    LambdaQueryWrapper<UserAddress> queryWrapper = new LambdaQueryWrapper<>();

    if (pageDTO.getUserId() != null) {
      queryWrapper.eq(UserAddress::getUserId, pageDTO.getUserId());
    }
    if (pageDTO.getConsignee() != null && !pageDTO.getConsignee().isEmpty()) {
      queryWrapper.like(UserAddress::getConsignee, pageDTO.getConsignee());
    }
    queryWrapper.orderByDesc(UserAddress::getCreatedAt);

    Page<UserAddress> resultPage = page(page, queryWrapper);
    List<UserAddressVO> userAddressVOList = userAddressConverter.toVOList(resultPage.getRecords());

    return PageResult.of(
        resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), userAddressVOList);
  }

  @Override
  public int deleteAddressBatch(List<Long> addressIds, Authentication authentication) {
    if (addressIds == null || addressIds.isEmpty()) {
      return 0;
    }
    int successCount = 0;
    for (Long addressId : addressIds) {
      if (addressId == null) {
        continue;
      }
      try {
        UserAddressDTO existingAddress = getAddressById(addressId);
        if (existingAddress == null) {
          continue;
        }
        if (SecurityPermissionUtils.isAdminOrOwner(authentication, existingAddress.getUserId())
            && removeById(addressId)) {
          successCount++;
        }
      } catch (Exception e) {
        log.error("Failed to delete address, addressId={}", addressId, e);
      }
    }
    return successCount;
  }

  @Override
  public int updateAddressBatch(
      List<UserAddressRequestDTO> addressList, Authentication authentication) {
    if (addressList == null || addressList.isEmpty()) {
      return 0;
    }
    int successCount = 0;
    for (UserAddressRequestDTO addressDTO : addressList) {
      if (addressDTO == null || addressDTO.getId() == null) {
        continue;
      }
      try {
        UserAddressDTO existingAddress = getAddressById(addressDTO.getId());
        if (existingAddress == null) {
          continue;
        }
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, existingAddress.getUserId())) {
          continue;
        }
        if (updateAddress(addressDTO.getId(), addressDTO) != null) {
          successCount++;
        }
      } catch (Exception e) {
        log.error("Failed to update address, addressId={}", addressDTO.getId(), e);
      }
    }
    return successCount;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public boolean removeById(Long id) {
    UserAddress existingAddress = this.getById(id);
    if (existingAddress == null) {
      log.warn("Failed to delete user address because it does not exist, addressId={}", id);
      throw new ResourceNotFoundException("address", String.valueOf(id));
    }

    return removeByIdWithCacheEvict(id, existingAddress.getUserId());
  }

  @Caching(
      evict = {
        @CacheEvict(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'detail:' + #addressId"),
        @CacheEvict(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'list:' + #userId")
      })
  protected boolean removeByIdWithCacheEvict(Long addressId, Long userId) {
    boolean removed = super.removeById(addressId);
    if (!removed) {
      log.error("Failed to delete user address, addressId={}", addressId);
      throw new BusinessException(500, "Failed to delete user address");
    }
    return true;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  @CacheEvict(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'list:' + #userId")
  public boolean resetDefaultAddress(Long userId) {
    if (userId == null) {
      throw new BusinessException("user id is required");
    }

    List<Long> defaultIds =
        lambdaQuery()
            .eq(UserAddress::getUserId, userId)
            .eq(UserAddress::getIsDefault, 1)
            .list()
            .stream()
            .map(UserAddress::getId)
            .toList();
    if (defaultIds.isEmpty()) {
      return true;
    }

    UserAddress updateEntity = new UserAddress();
    updateEntity.setIsDefault(0);
    boolean updated =
        update(
            updateEntity,
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1));
    Cache cache = cacheManager.getCache(USER_ADDRESS_CACHE_NAME);
    if (cache != null) {
      for (Long addressId : defaultIds) {
        cache.evict("detail:" + addressId);
      }
    }
    return updated;
  }
}
