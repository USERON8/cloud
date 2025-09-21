package com.cloud.user.controller.user;


import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.result.Result;
import com.cloud.user.converter.UserConverter;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user/manage")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息更新、删除等相关操作")
public class UserManageController {
    private final UserService userService;
    private final UserConverter userConverter = UserConverter.INSTANCE;

    @PostMapping("/update")
    @Operation(summary = "更新用户信息", description = "更新用户信息")
    public Result<Boolean> update(@RequestBody
                                  @Parameter(description = "用户信息")
                                  @Valid @NotNull(message = "用户信息不能为空") UserDTO userDTO,
                                  Authentication authentication) {

        // 使用统一的权限检查工具
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, userDTO.getId())) {
            return Result.forbidden("无权限更新此用户信息");
        }

        try {
            boolean result = userService.updateById(userConverter.toEntity(userDTO));
            return Result.success("用户更新成功", result);
        } catch (Exception e) {
            log.error("更新用户信息失败，用户ID: {}", userDTO.getId(), e);
            return Result.systemError("更新用户信息失败");
        }
    }

    @PostMapping("/delete")
    @Operation(summary = "删除用户", description = "逻辑删除指定用户")
    public Result<Boolean> delete(@RequestBody
                                  @Parameter(description = "用户ID")
                                  @NotNull(message = "用户ID不能为空") Long id,
                                  Authentication authentication) {

        // 使用统一的权限检查工具
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, id)) {
            return Result.forbidden("无权限删除此用户");
        }

        try {
            boolean result = userService.deleteUserById(id);
            return Result.success("用户删除成功", result);
        } catch (Exception e) {
            log.error("删除用户失败, 用户ID: {}", id, e);
            return Result.systemError("删除用户失败");
        }
    }

    @PostMapping("/deleteBatch")
    @Operation(summary = "批量删除用户", description = "批量逻辑删除用户")
    public Result<Boolean> deleteBatch(@RequestBody
                                       @Parameter(description = "用户ID数组")
                                       @NotNull(message = "用户ID数组不能为空") Long[] ids,
                                       Authentication authentication) {

        // 权限检查：只有管理员可以批量删除用户
        if (!SecurityPermissionUtils.isAdmin(authentication)) {
            return Result.forbidden("只有管理员可以批量删除用户");
        }

        if (ids == null || ids.length == 0) {
            return Result.badRequest("请选择要删除的用户");
        }

        try {
            List<Long> userIds = Arrays.asList(ids);
            boolean result = userService.deleteUsersByIds(userIds);

            return Result.success(String.format("批量删除%d个用户成功", userIds.size()), result);
        } catch (Exception e) {
            log.error("批量删除用户失败, 用户IDs: {}", Arrays.toString(ids), e);
            return Result.systemError("批量删除用户失败");
        }
    }
}
