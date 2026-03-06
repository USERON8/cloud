package com.cloud.user.service.impl;

import com.cloud.common.utils.UserContextUtils;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.MinioService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final UserMapper userMapper;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name:user-avatars}")
    private String bucketName;

    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String uploadAvatar(MultipartFile file) {
        validateFile(file);

        try {
            Long userId = getCurrentUserId();

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }

            String objectName = String.format("avatar/%d/%s%s", userId, UUID.randomUUID(), extension);
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            String avatarUrl = String.format("%s/%s/%s", publicEndpoint, bucketName, objectName);
            User user = new User();
            user.setId(userId);
            user.setAvatarUrl(avatarUrl);
            userMapper.updateById(user);
            return avatarUrl;
        } catch (Exception e) {
            log.error("Failed to upload avatar", e);
            throw new RuntimeException("failed to upload avatar: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("file size exceeds 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("unsupported image type");
        }

        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IllegalArgumentException("invalid image content");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to validate image: " + e.getMessage(), e);
        }
    }

    private Long getCurrentUserId() {
        String userId = UserContextUtils.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("current user id is missing");
        }

        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("invalid current user id: " + userId, e);
        }
    }
}