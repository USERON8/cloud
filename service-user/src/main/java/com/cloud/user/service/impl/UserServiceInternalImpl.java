package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.api.user.UserServiceInternal;
import com.cloud.common.domain.dto.UserDTO;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@DubboService
public class UserServiceInternalImpl extends ServiceImpl<UserMapper, User>
        implements UserServiceInternal {
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;
    private final UserMapper userMapper;

    public UserServiceInternalImpl(UserConverter userConverter, PasswordEncoder passwordEncoder, FileUploadService fileUploadService, UserMapper userMapper) {
        this.userConverter = userConverter;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadService = fileUploadService;
        this.userMapper = userMapper;
    }


    @Override
    public UserDTO findByUsername(String username) {
        User user = this.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("username", username));
        return user != null ? userConverter.toDTO(user) : null;
    }

    @Override
    public UserDTO findById(Long id) {
        User user = this.getById(id);
        return user != null ? userConverter.toDTO(user) : null;
    }

    @Override
    public void save(UserDTO userDTO) {
        User user = userConverter.toEntity(userDTO);
        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        this.save(user);
    }

    @Override
    public boolean updatePassword(Long userId, String newPassword) {
        // 检查用户是否存在
        User existingUser = this.getById(userId);
        if (existingUser == null) {
            log.warn("尝试更新不存在的用户密码，userId: {}", userId);
            return false;
        }

        // 更新密码
        User user = new User();
        user.setId(userId);
        // 使用BCrypt加密新密码
        user.setPassword(passwordEncoder.encode(newPassword));

        // 更新用户密码
        boolean result = this.updateById(user);
        if (result) {
            log.info("用户密码更新成功, userId: {}", userId);
        } else {
            log.error("用户密码更新失败, userId: {}", userId);
        }
        return result;
    }

    @Override
    public List<UserDTO> findByUserType(String userType) {
        java.util.List<User> users = this.list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("user_type", userType));
        return users.stream().map(userConverter::toDTO).toList();
    }
    
    @Override
    public void updateUserAvatar(Long userId, String avatarUrl, String fileName) {
        // 先获取用户原有头像信息
        User existUser = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getId, userId)
                        .select(User::getAvatarFileName)
        );

        if (existUser == null) {
            log.warn("尝试更新不存在的用户头像, userId: {}", userId);
            return;
        }

        // 删除原有头像文件
        if (existUser.getAvatarFileName() != null && !existUser.getAvatarFileName().isEmpty()) {
            try {
                fileUploadService.deleteAvatar(existUser.getAvatarFileName());
            } catch (Exception e) {
                log.warn("删除用户原有头像文件失败，userId: {}, fileName: {}", userId, existUser.getAvatarFileName(), e);
            }
        }

        // 更新用户头像信息
        userMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getAvatarUrl, avatarUrl)
                        .set(User::getAvatarFileName, fileName)
        );

        log.info("用户头像更新成功, userId: {}, avatarUrl: {}", userId, avatarUrl);
    }

    @Override
    public void deleteUserAvatar(Long userId) {
        // 获取用户头像信息
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getId, userId)
                        .select(User::getAvatarFileName)
        );

        if (user == null) {
            log.warn("尝试删除不存在的用户头像, userId: {}", userId);
            return;
        }

        if (user.getAvatarFileName() != null && !user.getAvatarFileName().isEmpty()) {
            // 删除文件
            try {
                fileUploadService.deleteAvatar(user.getAvatarFileName());
            } catch (Exception e) {
                log.warn("删除用户头像文件失败，userId: {}, fileName: {}", userId, user.getAvatarFileName(), e);
            }

            // 清空数据库记录
            userMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                            .eq(User::getId, userId)
                            .set(User::getAvatarUrl, null)
                            .set(User::getAvatarFileName, null)
            );

            log.info("用户头像删除成功, userId: {}", userId);
        }
    }
}