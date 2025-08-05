package com.cloud.user.service.impl;

import com.cloud.common.exception.BusinessException;
import com.cloud.user.config.MinioConfig;
import com.cloud.user.service.FileUploadService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final Tika tika = new Tika();

    // 支持的图片格式
    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };

    // 最大文件大小 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 头像尺寸限制
    private static final int AVATAR_MAX_WIDTH = 500;
    private static final int AVATAR_MAX_HEIGHT = 500;

    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
        try {
            // 检查存储桶是否存在
            ensureBucketExists();

            // 文件验证
            validateFile(file);

            // 处理图片（压缩、裁剪）
            byte[] processedImageData = processImage(file);

            // 生成文件名
            String fileName = generateFileName(file.getOriginalFilename(), userId);

            // 上传文件
            uploadToMinio(processedImageData, fileName, file.getContentType());

            // 返回访问URL
            return getFileUrl(fileName);

        } catch (Exception e) {
            log.error("上传头像失败, userId: {}, fileName: {}", userId, file.getOriginalFilename(), e);
            throw new BusinessException("头像上传失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteAvatar(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            log.error("删除头像失败, fileName: {}", fileName, e);
            return false;
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        return minioConfig.getPublicEndpoint() + "/" + minioConfig.getBucketName() + "/" + fileName;
    }

    /**
     * 确保存储桶存在
     */
    private void ensureBucketExists() throws Exception {
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .build()
        );

        if (!bucketExists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build()
            );

            // 设置存储桶策略为公开读
            String policy = """
                    {
                        "Version": "2012-10-17",
                        "Statement": [
                            {
                                "Effect": "Allow",
                                "Principal": "*",
                                "Action": "s3:GetObject",
                                "Resource": "arn:aws:s3:::%s/*"
                            }
                        ]
                    }
                    """.formatted(minioConfig.getBucketName());

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .config(policy)
                            .build()
            );
        }
    }

    /**
     * 文件验证
     */
    private void validateFile(MultipartFile file) throws Exception {
        // 检查文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过5MB");
        }

        // 检查文件类型
        String mimeType = tika.detect(file.getInputStream());
        boolean isValidType = false;
        for (String allowedType : ALLOWED_IMAGE_TYPES) {
            if (allowedType.equals(mimeType)) {
                isValidType = true;
                break;
            }
        }

        if (!isValidType) {
            throw new BusinessException("不支持的文件格式，仅支持: jpg, jpeg, png, gif, webp");
        }
    }

    /**
     * 处理图片（压缩、裁剪）
     */
    private byte[] processImage(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 使用Thumbnailator进行图片处理
            Thumbnails.of(inputStream)
                    .size(AVATAR_MAX_WIDTH, AVATAR_MAX_HEIGHT)
                    .keepAspectRatio(true)
                    .outputQuality(0.8)
                    .outputFormat("jpg")
                    .toOutputStream(outputStream);

            return outputStream.toByteArray();
        }
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String originalFilename, Long userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String extension = getFileExtension(originalFilename);

        return String.format("avatars/%s/%d_%s.%s", timestamp, userId, uuid, extension);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "jpg"; // 默认扩展名
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 上传到MinIO
     */
    private void uploadToMinio(byte[] data, String fileName, String contentType) throws Exception {
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .stream(inputStream, data.length, -1)
                            .contentType(contentType)
                            .build()
            );
        }
    }
}