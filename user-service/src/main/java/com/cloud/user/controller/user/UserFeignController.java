package com.cloud.user.controller.user;

import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.user.converter.MerchantConverter;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.MerchantService;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户内部接口", description = "供其他服务调用的用户相关内部接口")
public class UserFeignController {
    private final UserService userService;
    private final MerchantService merchantService;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final MerchantConverter merchantConverter = MerchantConverter.INSTANCE;

    @GetMapping("/internal/username/{username}")
    @Operation(summary = "根据用户名查询用户", description = "根据用户名查询用户详细信息")
    public UserDTO findByUsername(@PathVariable
                                  @Parameter(description = "用户名")
                                  @NotBlank(message = "用户名不能为空") String username) {
        log.debug("开始调用用户服务, 查询用户信息, username: {}", username);
        return userService.findByUsername(username);
    }

    @GetMapping("/internal/id/{id}")
    @Operation(summary = "根据ID查询用户", description = "根据用户ID查询用户详细信息")
    public UserDTO findById(@PathVariable
                            @Parameter(description = "用户ID")
                            @NotNull(message = "用户ID不能为空") Long id) {
        log.debug("开始调用用户服务, 查询用户信息, id: {}", id);
        // 使用服务层的getUserById方法，享受缓存和异常处理逻辑
        return userService.getUserById(id);
    }

    @PostMapping("/internal/register")
    @Operation(summary = "用户注册", description = "注册新用户，支持批量商家用户创建")
    public UserDTO register(@RequestBody
                            @Parameter(description = "注册请求信息")
                            @Valid @NotNull(message = "注册请求信息不能为空") RegisterRequestDTO registerRequest) {
        
        String username = registerRequest.getUsername();
        String userType = registerRequest.getUserType();
        
        log.info("🚀 开始用户注册流程, username: {}, userType: {}", username, userType);

        try {
            // 1. 参数预处理和验证
            validateRegisterRequest(registerRequest);
            
            // 2. 检查用户是否已存在（避免重复注册）
            UserDTO existingUser = userService.findByUsername(username);
            if (existingUser != null) {
                log.warn("⚠️ 用户注册失败，用户名已存在: {}", username);
                throw new BusinessException("用户名已存在: " + username);
            }

            // 3. 使用converter转换并设置默认值
            User user = prepareUserEntity(registerRequest);
            log.debug("✅ 用户实体准备完成: username={}, userType={}", user.getUsername(), user.getUserType());

            // 4. 保存用户（事务处理）
            boolean saved = userService.save(user);
            if (!saved) {
                log.error("❌ 用户注册失败，数据保存失败: {}", username);
                throw new BusinessException("用户注册失败");
            }

            // 5. 重新查询用户以获取完整信息（包括自动填充字段）
            UserDTO userDTO = userService.findByUsername(username);
            if (userDTO == null) {
                log.error("❌ 用户注册后查询失败: {}", username);
                throw new BusinessException("用户注册失败，无法获取用户信息");
            }

            // 6. 处理商家用户的特殊逻辑
            if ("MERCHANT".equals(userType)) {
                handleMerchantUserRegistration(userDTO, registerRequest);
            }

            log.info("🎉 用户注册成功: username={}, userId={}, userType={}", 
                    userDTO.getUsername(), userDTO.getId(), userDTO.getUserType());
            
            return userDTO;
            
        } catch (BusinessException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            log.error("💥 用户注册过程中发生未预期异常, username: {}", username, e);
            throw new BusinessException("用户注册失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证注册请求参数
     * 
     * @param registerRequest 注册请求
     */
    private void validateRegisterRequest(RegisterRequestDTO registerRequest) {
        if (registerRequest.getUsername() == null || registerRequest.getUsername().trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (registerRequest.getPassword() == null || registerRequest.getPassword().trim().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        if (registerRequest.getNickname() == null || registerRequest.getNickname().trim().isEmpty()) {
            throw new BusinessException("昵称不能为空");
        }
        if (registerRequest.getPhone() == null || registerRequest.getPhone().trim().isEmpty()) {
            throw new BusinessException("手机号不能为空");
        }
    }
    
    /**
     * 准备用户实体对象
     * 
     * @param registerRequest 注册请求
     * @return 用户实体
     */
    private User prepareUserEntity(RegisterRequestDTO registerRequest) {
        // 使用converter转换
        User user = userConverter.toEntity(registerRequest);
        
        // 设置加密密码
        String rawPassword = registerRequest.getPassword();
        if (rawPassword != null && !rawPassword.trim().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(rawPassword.trim());
            user.setPassword(encodedPassword);
            log.debug("🔐 密码已加密, username: {}", registerRequest.getUsername());
        } else {
            // 如果没有提供密码，设置默认密码
            String encodedPassword = passwordEncoder.encode("123456");
            user.setPassword(encodedPassword);
            log.debug("🔐 使用默认密码, username: {}", registerRequest.getUsername());
        }

        // 设置默认值
        if (user.getStatus() == null) {
            user.setStatus(1); // 默认启用
        }
        if (user.getUserType() == null || user.getUserType().trim().isEmpty()) {
            user.setUserType("USER"); // 默认用户类型
        }
        
        return user;
    }
    
    /**
     * 处理商家用户注册的特殊逻辑
     * 
     * @param userDTO 注册成功的用户信息
     * @param registerRequest 原始注册请求
     */
    private void handleMerchantUserRegistration(UserDTO userDTO, RegisterRequestDTO registerRequest) {
        try {
            log.info("🏪 开始创建商家记录, username: {}", userDTO.getUsername());
            
            MerchantDTO merchantDTO = new MerchantDTO();
            merchantDTO.setId(userDTO.getId()); // 使用用户ID作为商家ID
            merchantDTO.setUsername(userDTO.getUsername());
            merchantDTO.setMerchantName(userDTO.getNickname() != null ? userDTO.getNickname() : userDTO.getUsername());
            merchantDTO.setEmail(userDTO.getEmail());
            merchantDTO.setPhone(userDTO.getPhone());
            merchantDTO.setUserType(userDTO.getUserType());
            merchantDTO.setStatus(userDTO.getStatus());
            merchantDTO.setAuthStatus(0); // 默认为待审核状态

            // 调用商家服务创建商家记录
            boolean merchantSaved = merchantService.save(merchantConverter.toEntity(merchantDTO));
            if (merchantSaved) {
                log.info("✅ 成功为用户 {} 创建商家记录", userDTO.getUsername());
            } else {
                log.warn("⚠️ 为用户 {} 创建商家记录失败", userDTO.getUsername());
            }
            
        } catch (Exception e) {
            log.error("❌ 为用户 {} 创建商家记录时发生异常", userDTO.getUsername(), e);
            // 注意：这里即使创建商家记录失败，也不应影响用户注册的主流程
            // 商家记录可以后续手动创建或通过定时任务补偿
        }
    }

    @PutMapping("/internal/update")
    @Operation(summary = "更新用户信息", description = "更新用户信息")
    public Boolean update(@RequestBody
                          @Parameter(description = "用户信息")
                          @Valid @NotNull(message = "用户信息不能为空") UserDTO userDTO) {
        log.debug("开始调用用户服务, 更新用户信息, userId: {}", userDTO.getId());
        return userService.updateById(userConverter.toEntity(userDTO));
    }

    @GetMapping("/internal/password/{username}")
    @Operation(summary = "获取用户密码", description = "仅供 auth-service 认证使用，直接查询数据库避免缓存问题")
    public String getUserPassword(@PathVariable
                                  @Parameter(description = "用户名")
                                  @NotBlank(message = "用户名不能为空") String username) {
        
        log.debug("🔐 开始获取用户密码, username: {}", username);
        
        try {
            // 参数验证
            if (username == null || username.trim().isEmpty()) {
                log.warn("⚠️ 用户名为空");
                return null;
            }
            
            username = username.trim();
            
            // 直接查询数据库获取密码（避免缓存干扰）
            User user = userService.getOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                            .eq(User::getUsername, username)
                            .select(User::getUsername, User::getPassword, User::getStatus)
                            .last("LIMIT 1")
            );

            if (user == null) {
                log.warn("❌ 用户不存在, username: {}", username);
                return null;
            }
            
            // 检查用户状态
            if (user.getStatus() == null || user.getStatus() != 1) {
                log.warn("❌ 用户账户已禁用, username: {}, status: {}", username, user.getStatus());
                return null;
            }

            String password = user.getPassword();
            if (password != null && !password.trim().isEmpty()) {
                log.debug("✅ 成功获取用户密码, username: {}", username);
                return password;
            } else {
                log.warn("⚠️ 用户密码为空, 返回默认密码, username: {}", username);
                // 返回默认密码的BCrypt哈希值 ("123456")
                // 注意：这里不应该重新加密，应该返回预先加密的值
                return "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9P3mTd.lQBHBR8y";
            }
            
        } catch (Exception e) {
            log.error("💥 获取用户密码时发生异常, username: {}", username, e);
            return null;
        }
    }

}
