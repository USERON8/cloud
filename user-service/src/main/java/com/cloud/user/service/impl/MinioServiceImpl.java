package com.cloud.user.service.impl;


import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.MinioService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.cloud.common.utils.UserContextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author what's up
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    // 支持的图片类型
    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    // 最大文件大小 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private final UserMapper userMapper;
    private final MinioClient minioClient;
    @Value("${minio.bucket-name:user-avatars}")
    private String bucketName;
    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @return 头像URL
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String uploadAvatar(MultipartFile file) {
        // 参数校验
        validateFile(file);

        try {
            // 获取当前用户ID
            Long userId = getCurrentUserId();

            // 生成文件名: avatar/{userId}/{uuid}.{extension}
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String objectName = String.format("avatar/%d/%s%s", userId, UUID.randomUUID(), fileExtension);

            // 上传文件到MinIO
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

            // 构造文件访问URL
            String avatarUrl = String.format("%s/%s/%s", publicEndpoint, bucketName, objectName);

            // 更新用户头像URL
            User user = new User();
            user.setId(userId);
            user.setAvatarUrl(avatarUrl);
            userMapper.updateById(user);

            return avatarUrl;
        } catch (Exception e) {
            log.error("头像上传失败", e);
            throw new RuntimeException("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 验证上传的文件
     *
     * @param file 文件
     */

    private void validateFile(MultipartFile file) {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过5MB");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件类型，请上传jpeg、png、gif或webp格式的图片");
        }

        // 检查文件内容是否为图片
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IllegalArgumentException("文件不是有效的图片格式");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("文件读取失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    private Long getCurrentUserId() {
        String userId = UserContextUtils.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("无法获取当前用户信息");
        }
        
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("用户ID格式错误: " + userId);
        }
    }
}
