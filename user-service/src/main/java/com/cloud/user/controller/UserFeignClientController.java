package com.cloud.user.controller;

import com.cloud.api.user.UserFeignClient;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户服务Feign客户端接口实现控制器
 * 实现用户服务对外提供的Feign接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class UserFeignClientController implements UserFeignClient {

    private final UserService userService;
    private final UserConverter userConverter = UserConverter.INSTANCE;

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Override
    public UserDTO findByUsername(String username) {
        try {
            log.info("Feign调用：根据用户名查找用户，用户名: {}", username);

            UserDTO userDTO = userService.getUserByUsername(username);
            if (userDTO == null) {
                log.warn("用户不存在，用户名: {}", username);
                return null;
            }

            log.info("Feign调用：根据用户名查找用户成功，用户名: {}", username);
            return userDTO;
        } catch (Exception e) {
            log.error("Feign调用：根据用户名查找用户失败，用户名: {}", username, e);
            return null;
        }
    }

    /**
     * 根据ID查找用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public UserDTO findById(Long id) {
        try {
            log.info("Feign调用：根据ID查找用户，用户ID: {}", id);

            User user = userService.getById(id);
            if (user == null) {
                log.warn("用户不存在，用户ID: {}", id);
                return null;
            }

            UserDTO userDTO = userConverter.toDTO(user);
            log.info("Feign调用：根据ID查找用户成功，用户ID: {}", id);
            return userDTO;
        } catch (Exception e) {
            log.error("Feign调用：根据ID查找用户失败，用户ID: {}", id, e);
            return null;
        }
    }

    /**
     * 保存用户信息
     *
     * @param registerRequest 用户注册信息
     * @return 保存结果
     */
    @Override
    public Result<Boolean> register(RegisterRequestDTO registerRequest) {
        try {
            log.info("Feign调用：保存用户信息，用户名: {}", registerRequest.getUsername());

            boolean saved = userService.register(registerRequest);

            if (saved) {
                log.info("Feign调用：保存用户信息成功，用户名: {}", registerRequest.getUsername());
                return Result.success(true);
            } else {
                log.error("Feign调用：保存用户信息失败，用户名: {}", registerRequest.getUsername());
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "保存用户信息失败");
            }
        } catch (BusinessException e) {
            log.error("Feign调用：保存用户信息失败，用户名: {}", registerRequest.getUsername(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Feign调用：保存用户信息失败，用户名: {}", registerRequest.getUsername(), e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "保存用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     *
     * @param id      用户ID
     * @param userDTO 用户信息
     * @return 更新结果
     */
    @Override
    public Result<Void> update(Long id, UserDTO userDTO) {
        try {
            log.info("Feign调用：更新用户信息，用户ID: {}", id);

            User user = userConverter.toEntity(userDTO);
            user.setId(id);
            // 不能更新用户名
            user.setUsername(null);

            boolean updated = userService.updateById(user);
            if (updated) {
                log.info("Feign调用：更新用户信息成功，用户ID: {}", id);
                return Result.success();
            } else {
                log.error("Feign调用：更新用户信息失败，用户ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新用户信息失败");
            }
        } catch (BusinessException e) {
            log.error("Feign调用：更新用户信息失败，用户ID: {}", id, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Feign调用：更新用户信息失败，用户ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有用户
     *
     * @return 用户列表
     */
    @Override
    public Result<List<UserDTO>> getAllUsers() {
        try {
            log.info("Feign调用：获取所有用户");

            List<User> users = userService.list();
            List<UserDTO> userDTOs = userConverter.toDTOList(users);

            log.info("Feign调用：获取所有用户成功，共{}条记录", userDTOs.size());
            return Result.success(userDTOs);
        } catch (BusinessException e) {
            log.error("Feign调用：获取所有用户失败", e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Feign调用：获取所有用户失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取所有用户失败: " + e.getMessage());
        }
    }
}