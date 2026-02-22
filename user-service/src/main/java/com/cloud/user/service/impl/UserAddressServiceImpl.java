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

        
        boolean saved = super.save(entity);
        if (!saved) {
            log.error("淇濆瓨鐢ㄦ埛鍦板潃澶辫触");
            throw new BusinessException(500, "淇濆瓨鐢ㄦ埛鍦板潃澶辫触");
        }
        
        return true;
    }

    





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

        
        UserAddress existingAddress = this.getById(entity.getId());
        if (existingAddress == null) {
            log.warn("鏇存柊鐢ㄦ埛鍦板潃澶辫触锛氬湴鍧€涓嶅瓨鍦? addressId: {}", entity.getId());
            throw new ResourceNotFoundException("address", String.valueOf(entity.getId()));
        }

        if (!existingAddress.getUserId().equals(entity.getUserId())) {
            log.warn("鏇存柊鐢ㄦ埛鍦板潃澶辫触锛氭病鏈夋潈闄愭搷浣滆鍦板潃, addressId: {}, userId: {}", entity.getId(), entity.getUserId());
            throw new BusinessException("娌℃湁鏉冮檺鎿嶄綔璇ュ湴鍧€");
        }

        
        boolean updated = super.updateById(entity);
        if (!updated) {
            log.error("鏇存柊鐢ㄦ埛鍦板潃澶辫触, addressId: {}", entity.getId());
            throw new BusinessException(500, "鏇存柊鐢ㄦ埛鍦板潃澶辫触");
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
            log.warn("鍒犻櫎鐢ㄦ埛鍦板潃澶辫触锛氬湴鍧€涓嶅瓨鍦? addressId: {}", id);
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
            log.error("鍒犻櫎鐢ㄦ埛鍦板潃澶辫触, addressId: {}", addressId);
            throw new BusinessException(500, "鍒犻櫎鐢ㄦ埛鍦板潃澶辫触");
        }
        
        return true;
    }
}
