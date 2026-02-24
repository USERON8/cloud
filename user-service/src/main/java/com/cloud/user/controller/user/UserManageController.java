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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/manage/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management APIs")
public class UserManageController {

    private final UserService userService;
    private final UserConverter userConverter = UserConverter.INSTANCE;

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update user by user ID")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    public Result<Boolean> update(
            @PathVariable
            @Parameter(description = "User ID") Long id,
            @RequestBody
            @Parameter(description = "User payload")
            @Valid @NotNull(message = "user payload is required") UserDTO userDTO,
            Authentication authentication) {
        try {
            userDTO.setId(id);
            Boolean result = userService.updateUser(userDTO);
            return Result.success("user updated", Boolean.TRUE.equals(result));
        } catch (Exception e) {
            log.error("Failed to update user, id={}", id, e);
            return Result.error("failed to update user: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete user", description = "Delete user by user ID")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    public Result<Boolean> delete(
            @RequestBody
            @Parameter(description = "User ID")
            @NotNull(message = "user id is required") Long id,
            Authentication authentication) {
        try {
            boolean result = userService.deleteUserById(id);
            return Result.success("user deleted", result);
        } catch (Exception e) {
            log.error("Failed to delete user, id={}", id, e);
            return Result.error("failed to delete user: " + e.getMessage());
        }
    }

    @PostMapping("/deleteBatch")
    @Operation(summary = "Batch delete users", description = "Batch delete users by user IDs")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    public Result<Boolean> deleteBatch(
            @RequestBody
            @Parameter(description = "User ID array")
            @NotNull(message = "user ids are required") Long[] ids,
            Authentication authentication) {
        try {
            BatchValidationUtils.validateIdArray(ids, "Batch delete users");
            List<Long> userIds = Arrays.asList(ids);
            boolean result = userService.deleteUsersByIds(userIds);
            return Result.success(String.format("batch delete completed: %d", userIds.size()), result);
        } catch (Exception e) {
            log.error("Failed to batch delete users", e);
            return Result.error("failed to batch delete users: " + e.getMessage());
        }
    }

    @PostMapping("/updateBatch")
    @Operation(summary = "Batch update users", description = "Batch update users by payload list")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    public Result<Boolean> updateBatch(
            @RequestBody
            @Parameter(description = "User payload list")
            @Valid @NotNull(message = "user payload list is required") List<UserDTO> userDTOList,
            Authentication authentication) {
        try {
            BatchValidationUtils.validateBatchSize(userDTOList, "Batch update users");
            long missingIdCount = userDTOList.stream().filter(dto -> dto.getId() == null).count();
            if (missingIdCount > 0) {
                return Result.badRequest("all user payloads must include id for batch update");
            }
            boolean result = userService.updateBatchById(
                    userDTOList.stream().map(userConverter::toEntity).toList()
            );
            return Result.success(String.format("batch update completed: %d", userDTOList.size()), result);
        } catch (Exception e) {
            log.error("Failed to batch update users", e);
            return Result.error("failed to batch update users: " + e.getMessage());
        }
    }

    @PostMapping("/updateStatusBatch")
    @Operation(summary = "Batch update user status", description = "Batch update user status by IDs")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    public Result<Boolean> updateStatusBatch(
            @RequestParam
            @Parameter(description = "User IDs")
            @NotNull(message = "user ids are required") List<Long> ids,
            @RequestParam
            @Parameter(description = "User status")
            @NotNull(message = "status is required") Integer status,
            Authentication authentication) {
        try {
            BatchValidationUtils.validateIdList(ids, "Batch update user status");
            Integer successCount = userService.batchUpdateUserStatus(ids, status);
            if (successCount == null) {
                successCount = 0;
            }
            String message = String.format("batch status update completed: %d/%d", successCount, ids.size());
            return Result.success(message, true);
        } catch (Exception e) {
            log.error("Failed to batch update user status, status={}", status, e);
            return Result.error("failed to batch update user status: " + e.getMessage());
        }
    }
}
