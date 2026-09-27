package com.cloud.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.domain.dto.user.MerchantUpsertRequestDTO;
import com.cloud.user.converter.AdminConverter;
import com.cloud.user.converter.AuthPrincipalConverter;
import com.cloud.user.converter.MerchantConverter;
import com.cloud.user.mapper.AdminMapper;
import com.cloud.user.mapper.MerchantAuthMapper;
import com.cloud.user.mapper.MerchantMapper;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.Admin;
import com.cloud.user.module.entity.Merchant;
import com.cloud.user.service.cache.TransactionalAdminCacheService;
import com.cloud.user.service.cache.TransactionalMerchantCacheService;
import com.cloud.user.service.support.AuthPrincipalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentityResourceCreationTest {

  @Mock private AdminMapper adminMapper;
  @Mock private MerchantMapper merchantMapper;
  @Mock private MerchantAuthMapper merchantAuthMapper;
  @Mock private UserMapper userMapper;
  @Mock private AdminConverter adminConverter;
  @Mock private MerchantConverter merchantConverter;
  @Mock private AuthPrincipalConverter authPrincipalConverter;
  @Mock private AuthPrincipalService authPrincipalService;
  @Mock private TransactionalAdminCacheService adminCacheService;
  @Mock private TransactionalMerchantCacheService merchantCacheService;

  @Test
  void createAdminUsesAnExplicitPrincipalIdInsteadOfSharingTheAdminId() {
    AdminServiceImpl service =
        spy(
            new AdminServiceImpl(
                adminMapper,
                adminConverter,
                authPrincipalConverter,
                authPrincipalService,
                adminCacheService));

    AdminUpsertRequestDTO request = new AdminUpsertRequestDTO();
    request.setUsername("new-admin");
    request.setRealName("New Admin");
    request.setPassword("Secret#123");
    Admin admin = new Admin();
    admin.setUsername(request.getUsername());
    admin.setRealName(request.getRealName());
    AuthPrincipalDTO principal = new AuthPrincipalDTO();
    AdminDTO response = new AdminDTO();

    @SuppressWarnings("unchecked")
    LambdaQueryChainWrapper<Admin> query = mock(LambdaQueryChainWrapper.class);
    doReturn(query).when(service).lambdaQuery();
    when(query.eq(any(), any())).thenReturn(query);
    when(query.count()).thenReturn(0L);
    when(adminConverter.toEntity(request)).thenReturn(admin);
    when(authPrincipalConverter.toDTO(admin)).thenReturn(principal);
    when(authPrincipalService.createPrincipal(any())).thenReturn(9001L);
    doAnswer(
            ignored -> {
              admin.setId(7001L);
              return true;
            })
        .when(service)
        .save(admin);
    when(adminConverter.toDTO(admin)).thenReturn(response);

    service.createAdmin(request);

    assertThat(admin.getId()).isEqualTo(7001L);
    assertThat(admin.getPrincipalId()).isEqualTo(9001L);
    assertThat(admin.getPrincipalId()).isNotEqualTo(admin.getId());
    verify(adminCacheService).putTransactional(admin);
  }

  @Test
  void createMerchantUsesTheCreatedPrincipalAsOwnerAndKeepsItsOwnId() {
    MerchantServiceImpl service =
        spy(
            new MerchantServiceImpl(
                merchantAuthMapper,
                userMapper,
                authPrincipalConverter,
                merchantConverter,
                authPrincipalService,
                merchantCacheService));

    MerchantUpsertRequestDTO request = new MerchantUpsertRequestDTO();
    request.setUsername("new-merchant");
    request.setMerchantName("New Merchant");
    request.setPassword("Secret#123");
    Merchant merchant = new Merchant();
    merchant.setUsername(request.getUsername());
    merchant.setMerchantName(request.getMerchantName());
    AuthPrincipalDTO principal = new AuthPrincipalDTO();
    MerchantDTO response = new MerchantDTO();
    response.setId(8001L);
    response.setOwnerUserId(9002L);

    @SuppressWarnings("unchecked")
    LambdaQueryChainWrapper<Merchant> query = mock(LambdaQueryChainWrapper.class);
    doReturn(query).when(service).lambdaQuery();
    when(query.eq(any(), any())).thenReturn(query);
    when(query.count()).thenReturn(0L);
    when(merchantConverter.toEntity(request)).thenReturn(merchant);
    when(authPrincipalConverter.toDTO(merchant)).thenReturn(principal);
    when(authPrincipalService.createPrincipal(any())).thenReturn(9002L);
    doAnswer(
            ignored -> {
              merchant.setId(8001L);
              return true;
            })
        .when(service)
        .save(merchant);
    when(merchantConverter.toDTO(merchant)).thenReturn(response);
    when(merchantAuthMapper.selectOne(any())).thenReturn(null);
    when(userMapper.selectById(9002L)).thenReturn(null);

    service.createMerchant(request);

    assertThat(merchant.getId()).isEqualTo(8001L);
    assertThat(merchant.getOwnerUserId()).isEqualTo(9002L);
    assertThat(merchant.getOwnerUserId()).isNotEqualTo(merchant.getId());
    verify(merchantCacheService).putTransactional(merchant);
  }
}
