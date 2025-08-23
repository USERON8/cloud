package com.cloud.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户查询", description = "用户信息查询接口")
public class UserQueryController {
    private final UserService userService;


    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息", description = "根据请求头获取当前用户信息")
    @Parameters({
            @Parameter(name = "X-User-ID", description = "用户ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<UserDTO> getCurrentUserInfo(@RequestHeader("X-User-ID") String userId) {
        try {
            log.info("获取当前用户信息, userId: {}", userId);

            if (userId == null || userId.isEmpty()) {
                log.warn("获取当前用户信息失败: 用户ID为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
            }

            UserDTO userDTO = userService.getUserById(Long.valueOf(userId));
            log.info("获取当前用户信息成功, userId: {}", userId);
            return Result.success(userDTO);
        } catch (NumberFormatException e) {
            log.error("获取当前用户信息失败: 用户ID格式错误, userId: {}", userId, e);
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID格式错误");
        } catch (BusinessException e) {
            log.error("获取当前用户信息失败: 业务异常, userId: {}", userId, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取当前用户信息失败: 系统异常, userId: {}", userId, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "获取用户信息失败: " + e.getMessage());
        }
    }


    @GetMapping("/info/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "根据ID获取用户信息", description = "管理员根据用户ID获取用户信息")
    @Parameters({
            @Parameter(name = "id", description = "用户ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<UserDTO> getUserInfoById(@PathVariable Long id) {
        try {
            log.info("管理员获取用户信息, userId: {}", id);

            if (id == null) {
                log.warn("获取用户信息失败: 用户ID为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID不能为空");
            }

            UserDTO userDTO = userService.getUserById(id);
            log.info("管理员获取用户信息成功, userId: {}", id);
            return Result.success(userDTO);
        } catch (BusinessException e) {
            log.error("获取用户信息失败: 业务异常, userId: {}", id, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取用户信息失败: 系统异常, userId: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "获取用户信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "分页获取用户列表", description = "管理员分页获取用户列表")
    @Parameters({
            @Parameter(name = "page", description = "页码", example = "1"),
            @Parameter(name = "size", description = "每页大小", example = "10"),
            @Parameter(name = "username", description = "用户名（模糊查询）")
    })
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<IPage<UserDTO>> getUserList(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String username) {
        try {
            log.info("管理员分页获取用户列表, page: {}, size: {}, username: {}", page, size, username);

            IPage<UserDTO> userPage = userService.getUsersWithPagination(page, size, username);
            log.info("管理员分页获取用户列表成功, page: {}, size: {}, total: {}", page, size, userPage.getTotal());
            return Result.success(userPage);
        } catch (BusinessException e) {
            log.error("分页获取用户列表失败: 业务异常, page: {}, size: {}, username: {}", page, size, username, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("分页获取用户列表失败: 系统异常, page: {}, size: {}, username: {}", page, size, username, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "获取用户列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "根据用户名获取用户信息", description = "管理员根据用户名获取用户信息")
    @Parameters({
            @Parameter(name = "username", description = "用户名", required = true)
    })
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<UserDTO> getUserInfoByUsername(@PathVariable String username) {
        try {
            log.info("管理员根据用户名获取用户信息, username: {}", username);

            if (username == null || username.isEmpty()) {
                log.warn("获取用户信息失败: 用户名为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户名不能为空");
            }

            UserDTO userDTO = userService.getUserByUsername(username);
            log.info("管理员根据用户名获取用户信息成功, username: {}", username);
            return Result.success(userDTO);
        } catch (BusinessException e) {
            log.error("根据用户名获取用户信息失败: 业务异常, username: {}", username, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("根据用户名获取用户信息失败: 系统异常, username: {}", username, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "获取用户信息失败: " + e.getMessage());
        }
    }
}