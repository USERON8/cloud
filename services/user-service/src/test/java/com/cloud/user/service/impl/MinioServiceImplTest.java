package com.cloud.user.service.impl;

import com.cloud.user.mapper.UserMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private MinioClient minioClient;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache userCache;

    @Mock
    private Cache userListCache;

    @InjectMocks
    private MinioServiceImpl minioService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(minioService, "bucketName", "avatars");
        ReflectionTestUtils.setField(minioService, "publicEndpoint", "http://cdn.test");
        when(cacheManager.getCache("user")).thenReturn(userCache);
        when(cacheManager.getCache("userList")).thenReturn(userListCache);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("user_id", "5")
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadAvatar_success_updatesUserAndCache() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                outputStream.toByteArray()
        );

        when(userMapper.updateById(org.mockito.ArgumentMatchers.<com.cloud.user.module.entity.User>any())).thenReturn(1);

        String url = minioService.uploadAvatar(file);

        assertThat(url).startsWith("http://cdn.test/avatars/avatar/5/");
        verify(minioClient).putObject(any(PutObjectArgs.class));
        verify(userCache).evict(5L);
        verify(userListCache).clear();
    }

    @Test
    void uploadAvatar_invalidType_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.txt",
                "text/plain",
                "bad".getBytes()
        );

        assertThatThrownBy(() -> minioService.uploadAvatar(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported image type");
    }
}
