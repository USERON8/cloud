package com.cloud.user.controller.user;


import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/manage/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息更新、删除等相关操作")
public class UserManageController {
    private final UserService userService;
    private final UserConverter userConverter = UserConverter.INSTANCE;

    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息", description = "更新用户信息")
    @PreAuthorize("hasRole('ADMIN') or @securityPermissionUtils.isAdminOrOwner(authentication, #id)")
    public Result<Boolean> update(@PathVariable
                                  @Parameter(description = "用户ID") Long id,
                                  @RequestBody
                                  @Parameter(description = "用户信息")
                                  @Valid @NotNull(message = "用户信息不能为空") UserDTO userDTO,
                                  Authentication authentication) {

        // 确保路径参数与请求体中的ID一致
        userDTO.setId(id);

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
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> delete(@RequestBody
                                  @Parameter(description = "用户ID")
                                  @NotNull(message = "用户ID不能为空") Long id,
                                  Authentication authentication) {

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
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> deleteBatch(@RequestBody
                                       @Parameter(description = "用户ID数组")
                                       @NotNull(message = "用户ID数组不能为空") Long[] ids,
                                       Authentication authentication) {

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

    @PostMapping("/updateBatch")
    @Operation(summary = "批量更新用户信息", description = "批量更新多个用户的信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> updateBatch(@RequestBody
                                       @Parameter(description = "用户信息列表")
                                       @Valid @NotNull(message = "用户信息列表不能为空") List<UserDTO> userDTOList,
                                       Authentication authentication) {

        if (userDTOList == null || userDTOList.isEmpty()) {
            return Result.badRequest("用户信息列表不能为空");
        }

        if (userDTOList.size() > 100) {
            return Result.badRequest("批量更新数量不能超过100个");
        }

        try {
            boolean result = userService.updateBatchById(
                    userDTOList.stream()
                            .map(userConverter::toEntity)
                            .collect(java.util.stream.Collectors.toList())
            );
            return Result.success(String.format("批量更新%d个用户成功", userDTOList.size()), result);
        } catch (Exception e) {
            log.error("批量更新用户失败", e);
            return Result.systemError("批量更新用户失败");
        }
    }

    @PostMapping("/updateStatusBatch")
    @Operation(summary = "批量更新用户状态", description = "批量启用或禁用用户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> updateStatusBatch(@RequestParam
                                             @Parameter(description = "用户ID列表")
                                             @NotNull(message = "用户ID列表不能为空") List<Long> ids,
                                             @RequestParam
                                             @Parameter(description = "用户状态")
                                             @NotNull(message = "用户状态不能为空") Integer status,
                                             Authentication authentication) {

        if (ids == null || ids.isEmpty()) {
            return Result.badRequest("用户ID列表不能为空");
        }

        if (ids.size() > 100) {
            return Result.badRequest("批量操作数量不能超过100个");
        }

        try {
            int successCount = 0;
            for (Long id : ids) {
                if (userService.updateUserStatus(id, status)) {
                    successCount++;
                }
            }
            log.info("批量更新用户状态完成, 成功: {}/{}", successCount, ids.size());
            return Result.success(String.format("批量更新用户状态成功: %d/%d", successCount, ids.size()), true);
        } catch (Exception e) {
            log.error("批量更新用户状态失败, IDs: {}", ids, e);
            return Result.systemError("批量更新用户状态失败");
        }
    }
}
