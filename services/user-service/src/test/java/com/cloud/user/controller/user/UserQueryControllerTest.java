package com.cloud.user.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.result.PageResult;
import com.cloud.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserQueryControllerTest {

  @Mock private UserService userService;

  @Test
  void searchShouldForwardAllSupportedFilters() {
    when(userService.pageQuery(org.mockito.ArgumentMatchers.any(UserPageDTO.class)))
        .thenReturn(PageResult.empty(2L, 30L));

    UserQueryController controller = new UserQueryController(userService);
    controller.search(2, 30, "alice", "alice@example.com", "1380000", "Alice", 1, "ROLE_USER");

    ArgumentCaptor<UserPageDTO> captor = ArgumentCaptor.forClass(UserPageDTO.class);
    verify(userService).pageQuery(captor.capture());
    UserPageDTO request = captor.getValue();
    assertThat(request.getCurrent()).isEqualTo(2L);
    assertThat(request.getSize()).isEqualTo(30L);
    assertThat(request.getUsername()).isEqualTo("alice");
    assertThat(request.getEmail()).isEqualTo("alice@example.com");
    assertThat(request.getPhone()).isEqualTo("1380000");
    assertThat(request.getNickname()).isEqualTo("Alice");
    assertThat(request.getStatus()).isEqualTo(1);
    assertThat(request.getRoleCode()).isEqualTo("ROLE_USER");
  }
}
