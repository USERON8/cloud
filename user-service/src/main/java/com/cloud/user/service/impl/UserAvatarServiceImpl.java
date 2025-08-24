package com.cloud.user.service.impl;

import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserAvatarService;
import com.cloud.user.service.UserService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * 用户头像服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAvatarServiceImpl implements UserAvatarService {

    private final UserService userService;
    private final MinioClient minioClient;

    // 从配置文件中获取存储桶名称
    @Value("${minio.bucket-name}")
    private String bucketName;

    // 从配置文件中获取公共访问地址
    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

    /**
     * 上传用户头像
     *
     * @param file   头像文件
     * @param userId 用户ID
     * @return 头像访问URL
     */
    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
        try {
            log.info("开始上传用户头像, 用户ID: {}", userId);

            // 验证用户是否存在
            User user = userService.getById(userId);
            if (user == null) {
                log.warn("上传用户头像失败: 用户不存在, 用户ID: {}", userId);
                throw new ResourceNotFoundException("user", String.valueOf(userId));
            }

            // 验证文件
            if (file == null || file.isEmpty()) {
                log.warn("上传用户头像失败: 上传的文件为空, 用户ID: {}", userId);
                throw new BusinessException(400, "上传的文件为空");
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = "avatar/" + userId + "/" + UUID.randomUUID() + fileExtension;

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !isValidImageType(contentType)) {
                log.warn("上传用户头像失败: 不支持的文件类型, 用户ID: {}, 文件类型: {}", userId, contentType);
                throw new BusinessException(400, "不支持的文件类型，请上传jpg、png或gif格式的图片");
            }

            // 验证文件大小 (限制为5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                log.warn("上传用户头像失败: 文件大小超出限制, 用户ID: {}, 文件大小: {}", userId, file.getSize());
                throw new BusinessException(400, "文件大小不能超过5MB");
            }

            // 上传文件到Minio
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(uniqueFilename)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            } catch (Exception e) {
                log.error("上传用户头像到MinIO失败, 用户ID: {}, 文件名: {}", userId, uniqueFilename, e);
                throw new BusinessException(500, "文件上传到存储服务器失败");
            }

            // 构造访问URL
            String avatarUrl = publicEndpoint + "/" + bucketName + "/" + uniqueFilename;

            // 如果用户已有头像，则删除旧头像
            String oldAvatarUrl = user.getAvatarUrl();
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                deleteOldAvatar(oldAvatarUrl);
            }

            // 更新用户头像信息
            user.setAvatarUrl(avatarUrl);
            userService.updateById(user);

            log.info("用户头像上传成功, 用户ID: {}, 头像URL: {}", userId, avatarUrl);
            return avatarUrl;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传用户头像时发生未预期错误, 用户ID: {}", userId, e);
            throw new BusinessException(500, "上传头像失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户头像URL
     *
     * @param userId 用户ID
     * @return 头像URL
     */
    @Override
    public String getAvatar(Long userId) {
        try {
            log.info("开始获取用户头像URL, 用户ID: {}", userId);

            // 验证用户是否存在
            User user = userService.getById(userId);
            if (user == null) {
                log.warn("获取用户头像URL失败: 用户不存在, 用户ID: {}", userId);
                throw new ResourceNotFoundException("user", String.valueOf(userId));
            }

            String avatarUrl = user.getAvatarUrl();
            if (avatarUrl == null || avatarUrl.isEmpty()) {
                log.info("获取用户头像URL: 用户未设置头像, 用户ID: {}", userId);
                return null;
            }

            log.info("获取用户头像URL成功, 用户ID: {}, 头像URL: {}", userId, avatarUrl);
            return avatarUrl;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户头像URL时发生未预期错误, 用户ID: {}", userId, e);
            throw new BusinessException(500, "获取头像失败: " + e.getMessage());
        }
    }

    /**
     * 删除旧头像文件
     *
     * @param oldAvatarUrl 旧头像URL
     */
    private void deleteOldAvatar(String oldAvatarUrl) {
        try {
            // 从URL中提取对象名称
            String objectName = extractObjectNameFromUrl(oldAvatarUrl);
            if (objectName != null && !objectName.isEmpty()) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
                log.info("成功删除旧头像文件: {}", objectName);
            }
        } catch (Exception e) {
            log.warn("删除旧头像文件失败: {}", oldAvatarUrl, e);
        }
    }

    /**
     * 从URL中提取对象名称
     *
     * @param url 完整的URL
     * @return 对象名称
     */
    private String extractObjectNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // 从URL中提取对象名称，例如从 http://minio:9000/avatar-bucket/avatar/1/xxx.jpg 提取 avatar/1/xxx.jpg
        int bucketIndex = url.indexOf("/" + bucketName + "/");
        if (bucketIndex != -1) {
            return url.substring(bucketIndex + bucketName.length() + 2);
        }
        return null;
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    /**
     * 验证图片类型
     *
     * @param contentType 内容类型
     * @return 是否为有效的图片类型
     */
    private boolean isValidImageType(String contentType) {
        return "image/jpeg".equals(contentType) || 
               "image/png".equals(contentType) || 
               "image/gif".equals(contentType) || 
               "image/webp".equals(contentType);
    }
}