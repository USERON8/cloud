package com.cloud.user.service.impl;

import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.user.converter.MerchantAuthConverter;
import com.cloud.user.mapper.MerchantAuthMapper;
import com.cloud.user.module.entity.MerchantAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantAuthServiceImplTest {

    @Mock
    private MerchantAuthMapper merchantAuthMapper;

    @Mock
    private MerchantAuthConverter merchantAuthConverter;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private MerchantAuthServiceImpl merchantAuthService;

    @BeforeEach
    void setUp() {
        merchantAuthService = new MerchantAuthServiceImpl(merchantAuthMapper, merchantAuthConverter, cacheManager);
        ReflectionTestUtils.setField(merchantAuthService, "baseMapper", merchantAuthMapper);
        when(cacheManager.getCache("merchantAuthCache")).thenReturn(cache);
    }

    @Test
    void getMerchantAuthByIdWithCache_returnsDto() {
        MerchantAuth auth = new MerchantAuth();
        auth.setId(1L);
        MerchantAuthDTO dto = new MerchantAuthDTO();
        dto.setId(1L);
        when(merchantAuthMapper.selectById(1L)).thenReturn(auth);
        when(merchantAuthConverter.toDTO(auth)).thenReturn(dto);

        MerchantAuthDTO result = merchantAuthService.getMerchantAuthByIdWithCache(1L);

        assertThat(result).isSameAs(dto);
    }

    @Test
    void removeByMerchantId_evictionOnSuccess() {
        MerchantAuth auth = new MerchantAuth();
        auth.setId(2L);
        auth.setMerchantId(9L);
        when(merchantAuthMapper.selectOne(any())).thenReturn(auth);
        when(merchantAuthMapper.deleteById(2L)).thenReturn(1);

        boolean removed = merchantAuthService.removeByMerchantId(9L);

        assertThat(removed).isTrue();
        verify(cache).evict("id:2");
        verify(cache).evict("merchantId:9");
    }

    @Test
    void save_setsTimestamps() {
        MerchantAuth auth = new MerchantAuth();
        when(merchantAuthMapper.insert(any())).thenReturn(1);

        boolean saved = merchantAuthService.save(auth);

        assertThat(saved).isTrue();
        assertThat(auth.getCreatedAt()).isNotNull();
        assertThat(auth.getUpdatedAt()).isNotNull();
    }
}
