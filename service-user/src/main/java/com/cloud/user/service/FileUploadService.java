package com.cloud.user.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    /**
     * 上传头像文件
     *
     * @param file   文件
     * @param userId 用户ID
     * @return 文件访问URL
     */
    String uploadAvatar(MultipartFile file, Long userId);

    /**
     * 删除头像文件
     *
     * @param fileName 文件名
     * @return 是否删除成功
     */
    boolean deleteAvatar(String fileName);

    /**
     * 获取文件访问URL
     *
     * @param fileName 文件名
     * @return 访问URL
     */
    String getFileUrl(String fileName);
}