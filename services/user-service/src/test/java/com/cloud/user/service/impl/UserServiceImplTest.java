package com.cloud.user.service.impl;

import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.user.UserUpsertRequestDTO;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.support.AuthPrincipalRemoteService;
import com.cloud.user.service.support.UserInfoHashCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserConverter userConverter;

    @Mock
    private AuthPrincipalRemoteService authPrincipalRemoteService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private UserInfoHashCacheService userInfoHashCacheService;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new UserServiceImpl(
                userConverter,
                authPrincipalRemoteService,
                cacheManager,
                userInfoHashCacheService
        ));
    }

    @Test
    void createUserShouldCreatePrincipalWithDefaultRole() {
        UserUpsertRequestDTO request = new UserUpsertRequestDTO();
        request.setUsername("alice");
        request.setPassword("pwd-1");

        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(101L);
            return true;
        }).when(service).save(any(User.class));

        Long userId = service.createUser(request);

        assertThat(userId).isEqualTo(101L);
        ArgumentCaptor<AuthPrincipalDTO> captor = ArgumentCaptor.forClass(AuthPrincipalDTO.class);
        verify(authPrincipalRemoteService).createPrincipal(captor.capture());
        AuthPrincipalDTO dto = captor.getValue();
        assertThat(dto.getId()).isEqualTo(101L);
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getRoles()).contains("USER");
    }

    @Test
    void deleteUserByIdShouldThrowWhenUserMissing() {
        doReturn(null).when(service).getById(9L);

        assertThatThrownBy(() -> service.deleteUserById(9L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteUserByIdShouldRemoveAndDeletePrincipal() {
        User user = new User();
        user.setId(11L);
        doReturn(user).when(service).getById(11L);
        doReturn(true).when(service).removeById(11L);

        boolean result = service.deleteUserById(11L);

        assertThat(result).isTrue();
        verify(authPrincipalRemoteService).deletePrincipal(11L);
    }
}
