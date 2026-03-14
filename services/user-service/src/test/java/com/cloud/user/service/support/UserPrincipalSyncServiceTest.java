package com.cloud.user.service.support;

import com.cloud.common.exception.BusinessException;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPrincipalSyncServiceTest {

    @Mock
    private UserMapper userMapper;

    private UserPrincipalSyncService userPrincipalSyncService;

    @BeforeEach
    void setUp() {
        userPrincipalSyncService = new UserPrincipalSyncService(userMapper);
    }

    @Test
    void assertUsernameAvailable_existingUser_throws() {
        User existing = new User();
        existing.setId(10L);
        existing.setUsername("name");
        when(userMapper.selectOne(any())).thenReturn(existing);

        assertThatThrownBy(() -> userPrincipalSyncService.assertUsernameAvailable("name", 11L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("username already exists");
    }

    @Test
    void upsertUserPrincipal_insertsWhenMissing() {
        when(userMapper.selectById(1L)).thenReturn(null);

        userPrincipalSyncService.upsertUserPrincipal(1L, "u1", "nick", "mail", "phone", 1);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getUsername()).isEqualTo("u1");
        assertThat(saved.getNickname()).isEqualTo("nick");
        assertThat(saved.getEmail()).isEqualTo("mail");
    }

    @Test
    void upsertUserPrincipal_updatesWhenExists() {
        User existing = new User();
        existing.setId(2L);
        existing.setUsername("u2");
        when(userMapper.selectById(2L)).thenReturn(existing);

        userPrincipalSyncService.upsertUserPrincipal(2L, "u2", "new", null, null, null);

        verify(userMapper).updateById(any(User.class));
    }
}
