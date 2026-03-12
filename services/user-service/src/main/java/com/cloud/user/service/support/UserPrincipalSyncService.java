package com.cloud.user.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPrincipalSyncService {

    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public void assertUsernameAvailable(String username, Long currentUserId) {
        if (username == null || username.isBlank()) {
            return;
        }
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("limit 1"));
        if (existing != null && (currentUserId == null || !existing.getId().equals(currentUserId))) {
            throw new BusinessException("username already exists");
        }
    }

    @Transactional
    public void upsertUserPrincipal(Long userId,
                                    String username,
                                    String nickname,
                                    String email,
                                    String phone,
                                    Integer status) {
        if (userId == null) {
            return;
        }

        User existing = userMapper.selectById(userId);
        if (existing == null) {
            User user = new User();
            user.setId(userId);
            user.setUsername(username);
            user.setNickname(nickname == null || nickname.isBlank() ? username : nickname);
            user.setEmail(email);
            user.setPhone(phone);
            user.setStatus(status == null ? 1 : status);
            userMapper.insert(user);
            return;
        }

        User user = new User();
        user.setId(userId);
        if (username != null && !username.isBlank()) {
            user.setUsername(username);
        }
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
        }
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }
        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone);
        }
        if (status != null) {
            user.setStatus(status);
        }
        userMapper.updateById(user);
    }

    @Transactional
    public void deleteUserPrincipal(Long userId) {
        if (userId != null) {
            userMapper.deleteById(userId);
        }
    }
}
