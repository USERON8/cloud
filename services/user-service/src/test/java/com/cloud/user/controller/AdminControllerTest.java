package com.cloud.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.user.service.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

  @Mock private AdminService adminService;

  @Test
  void resetPasswordShouldReturnGeneratedTemporaryPassword() {
    when(adminService.resetPassword(org.mockito.ArgumentMatchers.eq(9L), anyString()))
        .thenReturn(true);

    AdminController controller = new AdminController(adminService);
    var result = controller.resetPassword(9L);

    ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
    verify(adminService)
        .resetPassword(org.mockito.ArgumentMatchers.eq(9L), passwordCaptor.capture());
    assertThat(result.getData()).isEqualTo(passwordCaptor.getValue());
    assertThat(result.getData()).startsWith("Tmp#");
    assertThat(result.getData()).hasSize(16);
  }
}
