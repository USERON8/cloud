package com.cloud.user.service.impl;

import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.domain.dto.user.MerchantUpsertRequestDTO;
import com.cloud.user.converter.MerchantConverter;
import com.cloud.user.exception.MerchantException;
import com.cloud.user.mapper.MerchantAuthMapper;
import com.cloud.user.mapper.MerchantMapper;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.Merchant;
import com.cloud.user.service.support.AuthPrincipalService;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private MerchantAuthMapper merchantAuthMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MerchantConverter merchantConverter;

    @Mock
    private AuthPrincipalService authPrincipalService;

    @Mock
    private CacheManager cacheManager;

    private MerchantServiceImpl merchantService;

    @BeforeEach
    void setUp() {
        merchantService = Mockito.spy(new MerchantServiceImpl(
                merchantAuthMapper,
                userMapper,
                merchantConverter,
                authPrincipalService,
                cacheManager
        ));
        ReflectionTestUtils.setField(merchantService, "baseMapper", merchantMapper);
    }

    @Test
    void createMerchant_success_callsRemoteSync() {
        LambdaQueryChainWrapper<Merchant> queryWrapper = Mockito.mock(LambdaQueryChainWrapper.class, Mockito.RETURNS_SELF);
        doReturn(queryWrapper).when(merchantService).lambdaQuery();
        doReturn(queryWrapper).when(queryWrapper).eq(Mockito.any(), Mockito.any());
        when(queryWrapper.count()).thenReturn(0L);
        when(merchantMapper.insert(org.mockito.ArgumentMatchers.<Merchant>any())).thenAnswer(invocation -> {
            Merchant merchant = invocation.getArgument(0);
            merchant.setId(1L);
            return 1;
        });
        when(merchantConverter.toDTO(any())).thenAnswer(invocation -> {
            MerchantDTO dto = new MerchantDTO();
            Merchant merchant = invocation.getArgument(0);
            dto.setId(merchant.getId());
            dto.setUsername(merchant.getUsername());
            return dto;
        });
        when(authPrincipalService.getRoleCodesByUserId(anyLong())).thenReturn(java.util.List.of("ROLE_MERCHANT"));

        MerchantUpsertRequestDTO request = new MerchantUpsertRequestDTO();
        request.setUsername("shop1");
        request.setMerchantName("Shop One");
        request.setPassword("pwd");
        request.setEmail("shop@example.com");

        MerchantDTO result = merchantService.createMerchant(request);

        assertThat(result.getUsername()).isEqualTo("shop1");
        verify(authPrincipalService).assertUsernameAvailable("shop1", null);
        verify(authPrincipalService).createPrincipal(any(AuthPrincipalDTO.class));
    }

    @Test
    void updateMerchantStatus_invalidStatus_throws() {
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setStatus(1);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);

        assertThatThrownBy(() -> merchantService.updateMerchantStatus(1L, 3))
                .isInstanceOf(MerchantException.MerchantStatusException.class);
    }
}
