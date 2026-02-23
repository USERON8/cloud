package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.user.UserAddressDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.user.converter.UserAddressConverter;
import com.cloud.user.mapper.UserAddressMapper;
import com.cloud.user.module.entity.UserAddress;
import com.cloud.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress>
        implements UserAddressService {

    private static final String USER_ADDRESS_CACHE_NAME = "userAddressCache";
    private final UserAddressMapper userAddressMapper;
    private final UserAddressConverter userAddressConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'detail:' + #entity.id"),
                    @CacheEvict(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'list:' + #entity.userId")
            }
    )
    public boolean save(UserAddress entity) {
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

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
            }
    )
    public boolean updateById(UserAddress entity) {
        entity.setUpdatedAt(LocalDateTime.now());

        UserAddress existingAddress = this.getById(entity.getId());
        if (existingAddress == null) {
            log.warn("Failed to update user address because it does not exist, addressId={}", entity.getId());
            throw new ResourceNotFoundException("address", String.valueOf(entity.getId()));
        }

        if (!existingAddress.getUserId().equals(entity.getUserId())) {
            log.warn("Failed to update user address due to permission mismatch, addressId={}, userId={}", entity.getId(), entity.getUserId());
            throw new BusinessException("No permission to operate this address");
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
            unless = "#result == null"
    )
    public UserAddressDTO getUserAddressByIdWithCache(Long id) {
        UserAddress userAddress = userAddressMapper.selectById(id);
        return userAddress != null ? userAddressConverter.toDTO(userAddress) : null;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = USER_ADDRESS_CACHE_NAME,
            key = "'list:' + #userId",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<UserAddressDTO> getUserAddressListByUserIdWithCache(Long userId) {
        List<UserAddress> userAddresses = lambdaQuery().eq(UserAddress::getUserId, userId).list();
        return userAddressConverter.toDTOList(userAddresses);
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
            }
    )
    protected boolean removeByIdWithCacheEvict(Long addressId, Long userId) {
        boolean removed = super.removeById(addressId);
        if (!removed) {
            log.error("Failed to delete user address, addressId={}", addressId);
            throw new BusinessException(500, "Failed to delete user address");
        }
        return true;
    }
}