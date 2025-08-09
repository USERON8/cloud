package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.api.user.UserInternalService;
import com.cloud.common.domain.dto.RegisterRequestDTO;
import com.cloud.common.domain.dto.UserDTO;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@DubboService
public class UserInternalServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserInternalService {
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;
    private final UserMapper userMapper;

    public UserInternalServiceImpl(UserConverter userConverter, PasswordEncoder passwordEncoder, FileUploadService fileUploadService, UserMapper userMapper) {
        this.userConverter = userConverter;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadService = fileUploadService;
        this.userMapper = userMapper;
    }


    @Override
    @Transactional
    public UserDTO findByUsername(String username) {
        User user = this.getOne(new QueryWrapper<User>()
                .eq("username", username));
        return user != null ? userConverter.toDTO(user) : null;
    }

    @Override
    @Transactional
    public UserDTO findById(Long id) {
        User user = this.getById(id);
        return user != null ? userConverter.toDTO(user) : null;
    }

    @Override
    @Transactional
    public void save(RegisterRequestDTO registerRequestDTO) {
        User user = new User();
        user.setUsername(registerRequestDTO.getUsername());
        // 对密码进行BCrypt加密
        user.setPasswordHash(passwordEncoder.encode(registerRequestDTO.getPassword()));
        user.setNickname(registerRequestDTO.getNickname());
        user.setEmail(registerRequestDTO.getEmail());
        user.setPhone(registerRequestDTO.getPhone());
        user.setUserType(registerRequestDTO.getUserType() != null ? registerRequestDTO.getUserType() : "USER");
        user.setStatus(1); // 默认启用状态
        user.setDeleted(0); // 默认未删除

        this.save(user);
        log.info("用户注册成功, username: {}", registerRequestDTO.getUsername());
    }

    @Override
    @Transactional
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
        user.setPasswordHash(passwordEncoder.encode(newPassword));
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
    @Transactional
    public List<UserDTO> findByUserType(String userType) {
        List<User> users = this.list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("user_type", userType));
        return users.stream().map(userConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateUserAvatar(Long userId, String avatarUrl, String fileName) {
        // 先获取用户原有头像信息
        User existUser = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getId, userId));

        if (existUser == null) {
            log.warn("用户不存在, userId: {}", userId);
            return;
        }

        // 删除旧头像文件
        if (existUser.getAvatarFileName() != null && !existUser.getAvatarFileName().isEmpty()) {
            fileUploadService.deleteFile(existUser.getAvatarFileName());
        }

        // 更新用户头像信息
        User user = new User();
        user.setId(userId);
        user.setAvatarUrl(avatarUrl);
        user.setAvatarFileName(fileName);
        this.updateById(user);
        log.info("用户头像更新成功, userId: {}", userId);
    }

    @Override
    @Transactional
    public void deleteUserAvatar(Long userId) {
        // 先获取用户原有头像信息
        User existUser = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getId, userId));

        if (existUser == null) {
            log.warn("用户不存在, userId: {}", userId);
            return;
        }

        // 删除旧头像文件
        if (existUser.getAvatarFileName() != null && !existUser.getAvatarFileName().isEmpty()) {
            fileUploadService.deleteFile(existUser.getAvatarFileName());
        }

        // 清除用户头像信息
        User user = new User();
        user.setId(userId);
        user.setAvatarUrl(null);
        user.setAvatarFileName(null);
        this.updateById(user);
        log.info("用户头像删除成功, userId: {}", userId);
    }
}