package com.cloud.user.service.impl;

import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.user.converter.AdminConverter;
import com.cloud.user.exception.AdminException;
import com.cloud.user.mapper.AdminMapper;
import com.cloud.user.module.entity.Admin;
import com.cloud.user.service.support.AuthPrincipalRemoteService;
import com.cloud.user.service.support.UserPrincipalSyncService;
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
class AdminServiceImplTest {

    @Mock
    private AdminMapper adminMapper;

    @Mock
    private AdminConverter adminConverter;

    @Mock
    private AuthPrincipalRemoteService authPrincipalRemoteService;

    @Mock
    private UserPrincipalSyncService userPrincipalSyncService;

    @Mock
    private CacheManager cacheManager;

    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        adminService = Mockito.spy(new AdminServiceImpl(
                adminMapper,
                adminConverter,
                authPrincipalRemoteService,
                userPrincipalSyncService,
                cacheManager
        ));
        ReflectionTestUtils.setField(adminService, "baseMapper", adminMapper);
    }

    @Test
    void createAdmin_success_callsRemoteSync() {
        LambdaQueryChainWrapper<Admin> queryWrapper = Mockito.mock(LambdaQueryChainWrapper.class, Mockito.RETURNS_SELF);
        doReturn(queryWrapper).when(adminService).lambdaQuery();
        doReturn(queryWrapper).when(queryWrapper).eq(Mockito.any(), Mockito.any());
        when(queryWrapper.count()).thenReturn(0L);
        when(adminMapper.insert(org.mockito.ArgumentMatchers.<Admin>any())).thenAnswer(invocation -> {
            Admin admin = invocation.getArgument(0);
            admin.setId(1L);
            return 1;
        });
        when(adminConverter.toDTO(any())).thenAnswer(invocation -> {
            AdminDTO dto = new AdminDTO();
            Admin admin = invocation.getArgument(0);
            dto.setId(admin.getId());
            dto.setUsername(admin.getUsername());
            return dto;
        });

        AdminUpsertRequestDTO request = new AdminUpsertRequestDTO();
        request.setUsername("admin");
        request.setRealName("Admin");
        request.setPassword("pwd");

        AdminDTO result = adminService.createAdmin(request);

        assertThat(result.getUsername()).isEqualTo("admin");
        verify(authPrincipalRemoteService).assertUsernameAvailable("admin", null);
        verify(userPrincipalSyncService).upsertUserPrincipal(anyLong(), any(), any(), any(), any(), any());
        verify(authPrincipalRemoteService).createPrincipal(any(AuthPrincipalDTO.class));
    }

    @Test
    void changePassword_oldPasswordMismatch_throws() {
        Admin admin = new Admin();
        admin.setId(9L);
        admin.setUsername("admin");
        when(adminMapper.selectById(9L)).thenReturn(admin);
        when(authPrincipalRemoteService.changePassword(9L, "old", "new")).thenReturn(false);

        assertThatThrownBy(() -> adminService.changePassword(9L, "old", "new"))
                .isInstanceOf(AdminException.AdminPasswordException.class);
    }
}
