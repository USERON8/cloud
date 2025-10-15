package com.cloud.user.controller.user;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.result.Result;
import com.cloud.common.validation.BatchValidationUtils;
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

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/manage/users")
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

        // Service层会抛出特定异常，由全局异常处理器统一处理
        boolean result = userService.updateById(userConverter.toEntity(userDTO));
        log.info("用户更新成功，用户ID: {}, 操作人: {}", userDTO.getId(), authentication.getName());
        return Result.success("用户更新成功", result);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除用户", description = "逻辑删除指定用户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> delete(@RequestBody
                                  @Parameter(description = "用户ID")
                                  @NotNull(message = "用户ID不能为空") Long id,
                                  Authentication authentication) {

        // Service层会抛出特定异常，由全局异常处理器统一处理
        boolean result = userService.deleteUserById(id);
        log.info("用户删除成功，用户ID: {}, 操作人: {}", id, authentication.getName());
        return Result.success("用户删除成功", result);
    }

    @PostMapping("/deleteBatch")
    @Operation(summary = "批量删除用户", description = "批量逻辑删除用户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> deleteBatch(@RequestBody
                                       @Parameter(description = "用户ID数组")
                                       @NotNull(message = "用户ID数组不能为空") Long[] ids,
                                       Authentication authentication) {

        BatchValidationUtils.validateIdArray(ids, "删除用户");
        List<Long> userIds = Arrays.asList(ids);
        boolean result = userService.deleteUsersByIds(userIds);

        log.info("批量删除用户完成，数量: {}, 成功数量: {}", userIds.size(), userIds.size());
        return Result.success(String.format("批量删除%d个用户成功", userIds.size()), result);
    }

    @PostMapping("/updateBatch")
    @Operation(summary = "批量更新用户信息", description = "批量更新多个用户的信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> updateBatch(@RequestBody
                                       @Parameter(description = "用户信息列表")
                                       @Valid @NotNull(message = "用户信息列表不能为空") List<UserDTO> userDTOList,
                                       Authentication authentication) {

        BatchValidationUtils.validateBatchSize(userDTOList, "批量更新用户");
        log.info("开始批量更新用户，数量: {}", userDTOList.size());
        boolean result = userService.updateBatchById(
                userDTOList.stream()
                        .map(userConverter::toEntity)
                        .collect(java.util.stream.Collectors.toList())
        );

        log.info("批量更新用户完成，数量: {}, 成功数量: {}", userDTOList.size(), userDTOList.size());
        return Result.success(String.format("批量更新%d个用户成功", userDTOList.size()), result);
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

        BatchValidationUtils.validateIdList(ids, "批量更新用户状态");
        log.info("开始批量更新用户状态，数量: {}, 操作人: {}", ids.size(), authentication.getName());

        // 使用批量更新方法，替代循环单个更新
        Integer successCount = userService.batchUpdateUserStatus(ids, status);

        log.info("批量更新用户状态完成，总数: {}, 成功: {}, 操作人: {}",
                ids.size(), successCount, authentication.getName());
        return Result.success(String.format("批量更新用户状态成功: %d/%d", successCount, ids.size()), true);
    }
}
