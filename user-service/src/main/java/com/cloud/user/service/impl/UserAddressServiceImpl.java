package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.user.mapper.UserAddressMapper;
import com.cloud.user.module.entity.UserAddress;
import com.cloud.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

    private final UserAddressMapper userAddressMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // 用户地址缓存前缀
    private static final String USER_ADDRESS_CACHE_PREFIX = "user:address:";

    /**
     * 根据用户ID获取用户地址列表
     *
     * @param userId 用户ID
     * @return 用户地址列表
     */
    @Override
    public List<UserAddress> getAddressByUserId(Long userId) {
        try {
            // 参数验证
            if (userId == null) {
                log.warn("获取用户地址列表失败: 用户ID为空");
                throw new BusinessException(400, "用户ID不能为空");
            }

            // 先从缓存中获取
            String cacheKey = USER_ADDRESS_CACHE_PREFIX + userId;
            List<UserAddress> cachedAddresses = (List<UserAddress>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedAddresses != null) {
                log.info("从缓存中获取用户地址列表, userId: {}", userId);
                return cachedAddresses;
            }

            // 缓存未命中，查询数据库
            QueryWrapper<UserAddress> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId);
            List<UserAddress> addresses = userAddressMapper.selectList(queryWrapper);

            // 将结果存入缓存
            redisTemplate.opsForValue().set(cacheKey, addresses, 30, TimeUnit.MINUTES);
            log.info("将用户地址列表存入缓存, userId: {}, 地址数量: {}", userId, addresses.size());

            return addresses;
        } catch (BusinessException e) {
            // 直接抛出已知的BusinessException
            throw e;
        } catch (Exception e) {
            log.error("获取用户地址列表时发生错误, userId: {}", userId, e);
            throw new BusinessException(500, "获取用户地址列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据地址ID获取用户地址详情
     *
     * @param addressId 地址ID
     * @return 用户地址详情
     */
    @Override
    public UserAddress getAddressById(Long addressId) {
        try {
            // 参数验证
            if (addressId == null) {
                log.warn("获取用户地址详情失败: 地址ID为空");
                throw new BusinessException(400, "地址ID不能为空");
            }

            // 先从缓存中获取
            String cacheKey = USER_ADDRESS_CACHE_PREFIX + "detail:" + addressId;
            UserAddress cachedAddress = (UserAddress) redisTemplate.opsForValue().get(cacheKey);
            if (cachedAddress != null) {
                log.info("从缓存中获取用户地址详情, addressId: {}", addressId);
                return cachedAddress;
            }

            // 缓存未命中，查询数据库
            UserAddress address = this.getById(addressId);
            if (address == null) {
                log.warn("获取用户地址详情失败：地址不存在, addressId: {}", addressId);
                throw new ResourceNotFoundException("address", String.valueOf(addressId));
            }

            // 将结果存入缓存
            redisTemplate.opsForValue().set(cacheKey, address, 30, TimeUnit.MINUTES);
            log.info("将用户地址详情存入缓存, addressId: {}", addressId);

            return address;
        } catch (BusinessException e) {
            // 直接抛出已知的BusinessException
            throw e;
        } catch (Exception e) {
            log.error("获取用户地址详情时发生错误, addressId: {}", addressId, e);
            throw new BusinessException(500, "获取用户地址详情失败: " + e.getMessage());
        }
    }

    /**
     * 保存用户地址信息
     *
     * @param entity 用户地址实体
     * @return 保存成功返回true
     */
    @Override
    public boolean save(UserAddress entity) {
        try {
            // 保存到数据库
            boolean saved = super.save(entity);
            if (saved) {
                log.info("保存用户地址成功, addressId: {}", entity.getId());

                // 同步更新缓存
                try {
                    // 更新用户地址列表缓存
                    String listCacheKey = USER_ADDRESS_CACHE_PREFIX + entity.getUserId();
                    // 清除用户地址列表缓存，下次查询时会重新加载
                    redisTemplate.delete(listCacheKey);

                    // 缓存地址详情
                    String detailCacheKey = USER_ADDRESS_CACHE_PREFIX + "detail:" + entity.getId();
                    redisTemplate.opsForValue().set(detailCacheKey, entity, 30, TimeUnit.MINUTES);

                    log.info("同步更新用户地址缓存成功, userId: {}, addressId: {}", entity.getUserId(), entity.getId());
                } catch (Exception e) {
                    log.error("同步更新用户地址缓存时发生异常, userId: {}, addressId: {}", entity.getUserId(), entity.getId(), e);
                    // 缓存更新失败不影响主流程
                }
            } else {
                log.error("保存用户地址失败");
                throw new BusinessException(500, "保存用户地址失败");
            }
            return saved;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("保存用户地址时发生异常", e);
            throw new BusinessException(500, "保存用户地址失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户地址信息
     *
     * @param entity 用户地址实体
     * @return 更新成功返回true
     */
    @Override
    public boolean updateById(UserAddress entity) {
        try {
            // 更新数据库
            boolean updated = super.updateById(entity);
            if (updated) {
                log.info("更新用户地址成功, addressId: {}", entity.getId());

                // 同步更新缓存
                try {
                    // 清除用户地址列表缓存
                    String listCacheKey = USER_ADDRESS_CACHE_PREFIX + entity.getUserId();
                    redisTemplate.delete(listCacheKey);

                    // 更新地址详情缓存
                    String detailCacheKey = USER_ADDRESS_CACHE_PREFIX + "detail:" + entity.getId();
                    redisTemplate.opsForValue().set(detailCacheKey, entity, 30, TimeUnit.MINUTES);

                    log.info("同步更新用户地址缓存成功, userId: {}, addressId: {}", entity.getUserId(), entity.getId());
                } catch (Exception e) {
                    log.error("同步更新用户地址缓存时发生异常, userId: {}, addressId: {}", entity.getUserId(), entity.getId(), e);
                    // 缓存更新失败不影响主流程
                }
            } else {
                log.error("更新用户地址失败, addressId: {}", entity.getId());
                throw new BusinessException(500, "更新用户地址失败");
            }
            return updated;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新用户地址时发生异常, addressId: {}", entity.getId(), e);
            throw new BusinessException(500, "更新用户地址失败: " + e.getMessage());
        }
    }

    /**
     * 清除用户地址相关缓存
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     */
    public void clearAddressCache(Long userId, Long addressId) {
        try {
            // 清除用户地址列表缓存
            if (userId != null) {
                redisTemplate.delete(USER_ADDRESS_CACHE_PREFIX + userId);
            }

            // 清除用户地址详情缓存
            if (addressId != null) {
                redisTemplate.delete(USER_ADDRESS_CACHE_PREFIX + "detail:" + addressId);
            }

            log.info("清除用户地址相关缓存, userId: {}, addressId: {}", userId, addressId);
        } catch (Exception e) {
            log.error("清除用户地址缓存时发生异常, userId: {}, addressId: {}", userId, addressId, e);
            // 缓存清除失败不应影响主流程，仅记录日志即可
        }
    }

    /**
     * 根据ID删除用户地址
     * 使用逻辑删除方式，将deleted字段设置为1
     *
     * @param id 地址ID
     * @return 删除成功返回true
     * @throws ResourceNotFoundException 当地址不存在时抛出
     * @throws BusinessException         当删除失败时抛出
     */
    @Override
    public boolean removeById(Long id) {
        try {
            // 参数验证
            if (id == null) {
                log.warn("删除用户地址失败: 地址ID为空");
                return false;
            }

            // 检查地址是否存在
            UserAddress address = this.getById(id);
            if (address == null) {
                log.warn("删除用户地址失败：地址不存在, addressId: {}", id);
                return false;
            }

            // 逻辑删除记录
            boolean removed = this.removeById(id);
            if (removed) {
                // 清除相关缓存
                clearAddressCache(address.getUserId(), id);
                userAddressMapper.updateDeleted(id);
                log.info("逻辑删除用户地址成功, addressId: {}", id);
            } else {
                log.error("逻辑删除用户地址失败, addressId: {}", id);
                throw new BusinessException(500, "删除用户地址失败");
            }
            return true;
        } catch (BusinessException e) {
            log.error("逻辑删除用户地址时发生错误, addressId: {}", id, e);
            throw new BusinessException(500, "删除用户地址失败: " + e.getMessage());
        }
    }

}