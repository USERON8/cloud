package com.cloud.user.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
import com.cloud.common.result.Result;
import com.cloud.user.converter.UserProfileCommandConverter;
import com.cloud.user.service.MinioService;
import com.cloud.user.service.UserService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

  @Mock private UserService userService;

  @Mock private MinioService minioService;

  @Mock private UserProfileCommandConverter userProfileCommandConverter;

  private UserProfileController controller;

  @BeforeEach
  void setUp() {
    controller = new UserProfileController(userService, minioService, userProfileCommandConverter);
  }

  @Test
  void uploadCurrentAvatarShouldPersistAvatarUrlToProfile() {
    MockMultipartFile file =
        new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1, 2, 3});
    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(
            Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("user_id", "42")
                .subject("alice")
                .issuedAt(Instant.parse("2026-03-21T00:00:00Z"))
                .expiresAt(Instant.parse("2026-03-21T01:00:00Z"))
                .build());

    when(minioService.uploadAvatar(file)).thenReturn("https://cdn.example.com/avatar.png");
    when(userService.updateProfile(any(UserProfileUpsertDTO.class))).thenReturn(true);

    Result<String> result = controller.uploadCurrentAvatar(file, authentication);

    assertThat(result.getData()).isEqualTo("https://cdn.example.com/avatar.png");
    ArgumentCaptor<UserProfileUpsertDTO> captor =
        ArgumentCaptor.forClass(UserProfileUpsertDTO.class);
    verify(userService).updateProfile(captor.capture());
    UserProfileUpsertDTO payload = captor.getValue();
    assertThat(payload.getId()).isEqualTo(42L);
    assertThat(payload.getAvatarUrl()).isEqualTo("https://cdn.example.com/avatar.png");
  }
}
