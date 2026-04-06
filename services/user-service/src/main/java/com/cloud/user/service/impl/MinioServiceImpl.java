package com.cloud.user.service.impl;

import com.cloud.common.utils.UserContextUtils;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.MinioService;
import com.cloud.user.service.cache.TransactionalUserCacheService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

  private static final List<String> SUPPORTED_IMAGE_TYPES =
      Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp");
  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
  private static final List<String> SUPPORTED_CERT_TYPES =
      Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf");
  private static final long MAX_CERT_FILE_SIZE = 10 * 1024 * 1024;
  private static final int MIN_PRESIGN_EXPIRE_SECONDS = 60;
  private static final int MAX_PRESIGN_EXPIRE_SECONDS = 604800;

  private final UserMapper userMapper;
  private final MinioClient minioClient;
  private final TransactionalUserCacheService userCacheService;

  @Value("${minio.bucket-name:cloud-shop-avatars}")
  private String bucketName;

  @Value("${minio.public-endpoint}")
  private String publicEndpoint;

  @Value("${minio.cert-bucket-name:cloud-shop-certs}")
  private String certBucketName;

  @Value("${minio.cert-presign-expire-seconds:3600}")
  private Integer certPresignExpireSeconds;

  @Transactional(rollbackFor = Exception.class)
  @Override
  public String uploadAvatar(MultipartFile file) {
    validateFile(file);

    try {
      Long userId = getCurrentUserId();
      User existingUser = userMapper.selectById(userId);

      String originalFilename = file.getOriginalFilename();
      String extension = "";
      if (originalFilename != null && originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
      }

      String objectName = String.format("avatar/%d/%s%s", userId, UUID.randomUUID(), extension);
      try (InputStream inputStream = file.getInputStream()) {
        minioClient.putObject(
            PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                    inputStream, file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
      }

      String avatarUrl = String.format("%s/%s/%s", publicEndpoint, bucketName, objectName);
      User user = new User();
      user.setId(userId);
      user.setAvatarUrl(avatarUrl);
      userMapper.updateById(user);
      refreshUserCache(existingUser, avatarUrl);
      return avatarUrl;
    } catch (Exception e) {
      log.error("Failed to upload avatar", e);
      throw new RuntimeException("failed to upload avatar: " + e.getMessage(), e);
    }
  }

  @Override
  public String uploadBusinessLicense(Long merchantId, MultipartFile file) {
    if (merchantId == null) {
      throw new IllegalArgumentException("merchant id is required");
    }
    validateCertFile(file);

    try {
      String extension = getFileExtension(file.getOriginalFilename());
      String objectName =
          String.format("cert/license/%d/%s%s", merchantId, UUID.randomUUID(), extension);
      try (InputStream inputStream = file.getInputStream()) {
        minioClient.putObject(
            PutObjectArgs.builder().bucket(certBucketName).object(objectName).stream(
                    inputStream, file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
      }
      return objectName;
    } catch (Exception e) {
      log.error("Failed to upload business license, merchantId={}", merchantId, e);
      throw new RuntimeException("failed to upload business license: " + e.getMessage(), e);
    }
  }

  @Override
  public String getCertPresignedUrl(String objectName) {
    if (objectName == null || objectName.isBlank()) {
      return null;
    }
    try {
      int expirySeconds = normalizePresignExpirySeconds(certPresignExpireSeconds);
      return minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .bucket(certBucketName)
              .object(objectName)
              .method(Method.GET)
              .expiry(expirySeconds)
              .build());
    } catch (Exception e) {
      log.error("Failed to generate certificate presigned url, objectName={}", objectName, e);
      throw new RuntimeException("failed to generate certificate url: " + e.getMessage(), e);
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

  private void validateCertFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("file is required");
    }

    if (file.getSize() > MAX_CERT_FILE_SIZE) {
      throw new IllegalArgumentException("file size exceeds 10MB");
    }

    String contentType = file.getContentType();
    if (contentType == null || !SUPPORTED_CERT_TYPES.contains(contentType.toLowerCase())) {
      throw new IllegalArgumentException("unsupported file type");
    }

    if (contentType.toLowerCase().startsWith("image/")) {
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
  }

  private int normalizePresignExpirySeconds(Integer expirySeconds) {
    int normalized = (expirySeconds == null || expirySeconds <= 0) ? 3600 : expirySeconds;
    if (normalized < MIN_PRESIGN_EXPIRE_SECONDS) {
      return MIN_PRESIGN_EXPIRE_SECONDS;
    }
    return Math.min(normalized, MAX_PRESIGN_EXPIRE_SECONDS);
  }

  private String getFileExtension(String originalFilename) {
    if (originalFilename == null || !originalFilename.contains(".")) {
      return "";
    }
    return originalFilename.substring(originalFilename.lastIndexOf('.'));
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

  private void refreshUserCache(User existingUser, String avatarUrl) {
    if (existingUser == null || existingUser.getId() == null) {
      return;
    }
    existingUser.setAvatarUrl(avatarUrl);
    userCacheService.putTransactional(existingUser);
  }
}
