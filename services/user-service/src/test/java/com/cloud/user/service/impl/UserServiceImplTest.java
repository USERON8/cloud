package com.cloud.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.user.UserUpsertRequestDTO;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.cache.TransactionalUserCacheService;
import com.cloud.user.service.support.AuthPrincipalService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserConverter userConverter;

  @Mock private AuthPrincipalService authPrincipalService;

  @Mock private CacheManager cacheManager;

  @Mock private TransactionalUserCacheService transactionalUserCacheService;

  private UserServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        spy(
            new UserServiceImpl(
                userConverter, authPrincipalService, cacheManager, transactionalUserCacheService));
  }

  @Test
  void createUserShouldCreatePrincipalWithDefaultRole() {
    UserUpsertRequestDTO request = new UserUpsertRequestDTO();
    request.setUsername("alice");
    request.setPassword("pwd-1");

    doReturn(101L).when(authPrincipalService).createPrincipal(any(AuthPrincipalDTO.class));
    User created = new User();
    created.setId(101L);
    doReturn(created).when(service).getById(101L);

    Long userId = service.createUser(request);

    assertThat(userId).isEqualTo(101L);
    ArgumentCaptor<AuthPrincipalDTO> captor = ArgumentCaptor.forClass(AuthPrincipalDTO.class);
    verify(authPrincipalService).createPrincipal(captor.capture());
    AuthPrincipalDTO dto = captor.getValue();
    assertThat(dto.getId()).isNull();
    assertThat(dto.getUsername()).isEqualTo("alice");
    assertThat(dto.getRoles()).contains("ROLE_USER");
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
    verify(authPrincipalService).deletePrincipal(11L);
  }

  @Test
  void updateUsersBatchShouldSyncPrincipalPayload() {
    UserUpsertRequestDTO request = new UserUpsertRequestDTO();
    request.setId(21L);
    request.setUsername("alice-next");
    request.setPassword("pwd-2");
    request.setNickname("Alice");
    request.setEmail("alice@example.com");
    request.setPhone("13800138000");
    request.setStatus(1);
    request.setRoles(List.of("ROLE_ADMIN"));

    User existing = new User();
    existing.setId(21L);
    existing.setUsername("alice");
    existing.setNickname("Old");
    existing.setEmail("old@example.com");
    existing.setPhone("13900139000");
    existing.setStatus(0);

    doReturn(existing).when(service).getById(21L);
    doReturn(true).when(service).persistUserBatch(anyCollection());

    boolean updated = service.updateUsersBatch(List.of(request));

    assertThat(updated).isTrue();
    ArgumentCaptor<AuthPrincipalDTO> captor = ArgumentCaptor.forClass(AuthPrincipalDTO.class);
    verify(authPrincipalService).updatePrincipal(captor.capture());
    AuthPrincipalDTO dto = captor.getValue();
    assertThat(dto.getId()).isEqualTo(21L);
    assertThat(dto.getUsername()).isEqualTo("alice-next");
    assertThat(dto.getPassword()).isEqualTo("pwd-2");
    assertThat(dto.getRoles()).containsExactly("ROLE_ADMIN");
  }
}
