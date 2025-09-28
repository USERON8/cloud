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
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author what's up
 * @description 针对表【user_address(用户地址表)】的数据库操作Service实现
 * @createDate 2025-08-20 12:35:31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress>
        implements UserAddressService {

    // 用户地址缓存名称
    private static final String USER_ADDRESS_CACHE_NAME = "userAddressCache";
    private final UserAddressMapper userAddressMapper;
    private final UserAddressConverter userAddressConverter;

    /**
     * 保存用户地址信息
     *
     * @param entity 用户地址实体
     * @return 保存成功返回true
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            put = {
                    @CachePut(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'detail:' + #entity.id")
            },
            evict = {
                    @CacheEvict(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'list:' + #entity.userId")
            }
    )
    public boolean save(UserAddress entity) {
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        // 保存到数据库
        boolean saved = super.save(entity);
        if (!saved) {
            log.error("保存用户地址失败");
            throw new BusinessException(500, "保存用户地址失败");
        }
        log.info("保存用户地址成功, addressId: {}", entity.getId());
        return true;
    }

    /**
     * 更新用户地址信息
     *
     * @param entity 用户地址实体
     * @return 更新成功返回true
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            put = {
                    @CachePut(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'detail:' + #entity.id")
            },
            evict = {
                    @CacheEvict(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'list:' + #entity.userId")
            }
    )
    public boolean updateById(UserAddress entity) {
        entity.setUpdatedAt(LocalDateTime.now());

        // 检查地址是否存在且属于当前用户
        UserAddress existingAddress = this.getById(entity.getId());
        if (existingAddress == null) {
            log.warn("更新用户地址失败：地址不存在, addressId: {}", entity.getId());
            throw new ResourceNotFoundException("address", String.valueOf(entity.getId()));
        }

        if (!existingAddress.getUserId().equals(entity.getUserId())) {
            log.warn("更新用户地址失败：没有权限操作该地址, addressId: {}, userId: {}", entity.getId(), entity.getUserId());
            throw new BusinessException("没有权限操作该地址");
        }

        // 更新数据库
        boolean updated = super.updateById(entity);
        if (!updated) {
            log.error("更新用户地址失败, addressId: {}", entity.getId());
            throw new BusinessException(500, "更新用户地址失败");
        }
        log.info("更新用户地址成功, addressId: {}", entity.getId());
        return true;
    }

    /**
     * 根据ID获取用户地址详情(带缓存)
     *
     * @param id 地址ID
     * @return 用户地址DTO
     */
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

    /**
     * 根据用户ID获取用户地址列表(带缓存)
     *
     * @param userId 用户ID
     * @return 用户地址DTO列表
     */
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = USER_ADDRESS_CACHE_NAME,
            key = "'list:' + #userId", // 列表缓存时间短一些，15分钟
            unless = "#result == null || #result.isEmpty()"
    )
    public List<UserAddressDTO> getUserAddressListByUserIdWithCache(Long userId) {
        List<UserAddress> userAddresses = lambdaQuery().eq(UserAddress::getUserId, userId).list();
        return userAddressConverter.toDTOList(userAddresses);
    }

    /**
     * 删除用户地址
     *
     * @param id 地址ID
     * @return 删除成功返回true
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeById(Long id) {
        // 检查地址是否存在
        UserAddress existingAddress = this.getById(id);
        if (existingAddress == null) {
            log.warn("删除用户地址失败：地址不存在, addressId: {}", id);
            throw new ResourceNotFoundException("address", String.valueOf(id));
        }

        // 使用组合注解来清理缓存
        return removeByIdWithCacheEvict(id, existingAddress.getUserId());
    }

    @Caching(
            evict = {
                    @CacheEvict(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'detail:' + #addressId"),
                    @CacheEvict(cacheNames = USER_ADDRESS_CACHE_NAME, key = "'list:' + #userId")
            }
    )
    private boolean removeByIdWithCacheEvict(Long addressId, Long userId) {
        // 删除数据库记录
        boolean removed = super.removeById(addressId);
        if (!removed) {
            log.error("删除用户地址失败, addressId: {}", addressId);
            throw new BusinessException(500, "删除用户地址失败");
        }
        log.info("删除用户地址成功, addressId: {}", addressId);
        return true;
    }
}