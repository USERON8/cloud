package com.cloud.user.service;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {
    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @return 头像URL
     */
    String uploadAvatar(MultipartFile file);
}
