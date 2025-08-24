package com.cloud.user.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 用户头像服务接口
 */
public interface UserAvatarService {

    /**
     * 上传用户头像
     *
     * @param file     头像文件
     * @param userId   用户ID
     * @return 头像访问URL
     */
    String uploadAvatar(MultipartFile file, Long userId);

    /**
     * 获取用户头像URL
     *
     * @param userId 用户ID
     * @return 头像URL
     */
    String getAvatar(Long userId);
}