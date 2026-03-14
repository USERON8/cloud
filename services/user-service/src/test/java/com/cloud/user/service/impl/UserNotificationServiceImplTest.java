package com.cloud.user.service.impl;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.user.notification.UserNotificationDeliveryProvider;
import com.cloud.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserNotificationServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private UserNotificationDeliveryProvider deliveryProvider;

    @InjectMocks
    private UserNotificationServiceImpl userNotificationService;

    @Test
    void sendWelcomeEmail_missingEmail_returnsFalse() {
        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setEmail(" ");
        when(userService.getUserById(1L)).thenReturn(user);

        boolean result = userNotificationService.sendWelcomeEmail(1L);

        assertThat(result).isFalse();
        verify(deliveryProvider, never()).deliverWelcome(1L);
    }

    @Test
    void sendPasswordResetEmail_trimsToken() {
        UserDTO user = new UserDTO();
        user.setId(2L);
        user.setEmail("user@example.com");
        when(userService.getUserById(2L)).thenReturn(user);
        when(deliveryProvider.deliverPasswordResetToken(2L, "token"))
                .thenReturn(true);

        boolean result = userNotificationService.sendPasswordResetEmail(2L, "  token ");

        assertThat(result).isTrue();
        verify(deliveryProvider).deliverPasswordResetToken(2L, "token");
    }
}
