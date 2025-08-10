package com.cloud.user.service.impl;

import com.cloud.common.utils.UserContextUtil;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.FileUploadService;
import com.cloud.user.service.UserAvatarService;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户头像服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAvatarServiceImpl implements UserAvatarService {

    private final FileUploadService fileUploadService;
    private final UserService userService;

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @return 文件访问URL
     */
    @Override
    public String uploadAvatar(MultipartFile file) {
        Long currentUserId = UserContextUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new IllegalStateException("无法获取当前用户信息");
        }

        log.info("上传用户头像, userId: {}", currentUserId);

        // 上传文件
        String fileUrl = fileUploadService.uploadAvatar(file, currentUserId);

        // 更新用户头像信息
        User user = userService.getById(currentUserId);
        if (user != null) {
            user.setAvatarUrl(fileUrl);
            userService.updateById(user);
        }

        log.info("用户头像上传成功, userId: {}, url: {}", currentUserId, fileUrl);
        return fileUrl;
    }

    /**
     * 获取用户头像
     *
     * @param userId 用户ID
     * @return 头像URL
     */
    @Override
    public String getAvatar(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user.getAvatarUrl() != null ? user.getAvatarUrl() : "";
    }

    /**
     * 删除用户头像
     *
     * @param userId 用户ID
     */
    @Override
    public void deleteAvatar(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            // 从存储中删除文件
            // 这里简化处理，实际项目中可能需要从URL中提取文件名
            fileUploadService.deleteAvatar(user.getAvatarFileName());
            // 清除用户头像信息
            user.setAvatarUrl(null);
            user.setAvatarFileName(null);
            userService.updateById(user);
        }
    }
}