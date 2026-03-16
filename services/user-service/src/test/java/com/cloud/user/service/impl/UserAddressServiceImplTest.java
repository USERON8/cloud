package com.cloud.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.user.converter.UserAddressConverter;
import com.cloud.user.mapper.UserAddressMapper;
import com.cloud.user.module.entity.UserAddress;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceImplTest {

  @Mock private UserAddressMapper userAddressMapper;

  @Mock private UserAddressConverter userAddressConverter;

  @Mock private CacheManager cacheManager;

  @Mock private Cache cache;

  private UserAddressServiceImpl userAddressService;

  @BeforeEach
  void setUp() {
    userAddressService =
        Mockito.spy(
            new UserAddressServiceImpl(userAddressMapper, userAddressConverter, cacheManager));
    ReflectionTestUtils.setField(userAddressService, "baseMapper", userAddressMapper);
    lenient().when(cacheManager.getCache("userAddressCache")).thenReturn(cache);
  }

  @Test
  void save_defaultAddress_resetsOldDefaults() {
    @SuppressWarnings("unchecked")
    LambdaQueryChainWrapper<UserAddress> queryWrapper =
        Mockito.mock(LambdaQueryChainWrapper.class, Mockito.RETURNS_SELF);
    doReturn(queryWrapper).when(userAddressService).lambdaQuery();
    doReturn(queryWrapper).when(queryWrapper).eq(Mockito.any(), Mockito.any());

    UserAddress defaultAddress = new UserAddress();
    defaultAddress.setId(9L);
    defaultAddress.setUserId(1L);
    defaultAddress.setIsDefault(1);

    UserAddress entity = new UserAddress();
    entity.setUserId(1L);
    entity.setIsDefault(1);

    when(queryWrapper.list()).thenReturn(List.of(defaultAddress));
    when(userAddressMapper.update(any(), any())).thenReturn(1);
    when(userAddressMapper.insert(org.mockito.ArgumentMatchers.<UserAddress>any())).thenReturn(1);

    boolean result = userAddressService.save(entity);

    assertThat(result).isTrue();
    verify(cache).evict("detail:9");
  }

  @Test
  void updateById_permissionMismatch_throws() {
    UserAddress existing = new UserAddress();
    existing.setId(2L);
    existing.setUserId(1L);

    UserAddress update = new UserAddress();
    update.setId(2L);
    update.setUserId(3L);

    when(userAddressMapper.selectById(2L)).thenReturn(existing);

    assertThatThrownBy(() -> userAddressService.updateById(update))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("No permission");
  }

  @Test
  void removeById_missing_throwsNotFound() {
    when(userAddressMapper.selectById(5L)).thenReturn(null);

    assertThatThrownBy(() -> userAddressService.removeById(5L))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
