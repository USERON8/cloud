package com.cloud.user.rpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.user.UserProfileDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
import com.cloud.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDubboServiceTest {

  @Mock private UserService userService;

  private UserDubboService userDubboService;

  @BeforeEach
  void setUp() {
    userDubboService = new UserDubboService(userService);
  }

  @Test
  void findById_delegates() {
    UserProfileDTO dto = new UserProfileDTO();
    when(userService.getProfileById(1L)).thenReturn(dto);

    UserProfileDTO result = userDubboService.findById(1L);

    assertThat(result).isSameAs(dto);
  }

  @Test
  void update_delegates() {
    UserProfileUpsertDTO request = new UserProfileUpsertDTO();
    when(userService.updateProfile(request)).thenReturn(true);

    Boolean result = userDubboService.update(request);

    assertThat(result).isTrue();
  }
}
