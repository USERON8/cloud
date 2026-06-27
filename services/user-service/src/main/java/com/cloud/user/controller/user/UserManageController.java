package com.cloud.user.controller.user;

import com.cloud.common.domain.dto.user.UserUpsertRequestDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.Result;
import com.cloud.common.validation.BatchValidationUtils;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户管理接口")
public class UserManageController {

  private final UserService userService;

  @PutMapping("/{id}")
  @Operation(summary = "更新用户", description = "按用户 ID 更新用户")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> update(
      @PathVariable @Parameter(description = "用户 ID") Long id,
      @RequestBody
          @Parameter(description = "用户请求体")
          @Valid
          @NotNull(message = "user payload is required")
          UserUpsertRequestDTO requestDTO,
      Authentication authentication) {
    Boolean result = userService.updateUser(id, requestDTO);
    return Result.success("user updated", Boolean.TRUE.equals(result));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "删除用户", description = "按用户 ID 删除用户")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> delete(
      @PathVariable @Parameter(description = "用户 ID") Long id, Authentication authentication) {
    boolean result = userService.deleteUserById(id);
    return Result.success("user deleted", result);
  }

  @DeleteMapping("/batch")
  @Operation(summary = "批量删除用户", description = "按用户 ID 批量删除用户")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> deleteBatch(
      @RequestBody
          @Parameter(description = "用户 ID 数组")
          @NotNull(message = "user ids are required")
          Long[] ids,
      Authentication authentication) {
    BatchValidationUtils.validateIdArray(ids, "批量删除用户");
    List<Long> userIds = Arrays.asList(ids);
    boolean result = userService.deleteUsersByIds(userIds);
    return Result.success(String.format("batch delete completed: %d", userIds.size()), result);
  }

  @PutMapping("/batch")
  @Operation(summary = "批量更新用户", description = "按请求列表批量更新用户")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> updateBatch(
      @RequestBody
          @Parameter(description = "用户请求体列表")
          @Valid
          @NotNull(message = "user payload list is required")
          List<UserUpsertRequestDTO> requestDTOList,
      Authentication authentication) {
    BatchValidationUtils.validateBatchSize(requestDTOList, "批量更新用户");
    long missingIdCount = requestDTOList.stream().filter(dto -> dto.getId() == null).count();
    if (missingIdCount > 0) {
      throw new BizException(
          ResultCode.BAD_REQUEST, "all user payloads must include id for batch update");
    }
    boolean result = userService.updateUsersBatch(requestDTOList);
    return Result.success(
        String.format("batch update completed: %d", requestDTOList.size()), result);
  }

  @PatchMapping("/status/batch")
  @Operation(summary = "批量更新用户状态", description = "按 ID 批量更新用户状态")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> updateStatusBatch(
      @RequestParam @Parameter(description = "用户 ID 列表") @NotNull(message = "user ids are required")
          List<Long> ids,
      @RequestParam @Parameter(description = "用户状态") @NotNull(message = "status is required")
          Integer status,
      Authentication authentication) {
    BatchValidationUtils.validateIdList(ids, "批量更新用户状态");
    Integer successCount = userService.batchUpdateUserStatus(ids, status);
    if (successCount == null) {
      successCount = 0;
    }
    String message =
        String.format("batch status update completed: %d/%d", successCount, ids.size());
    return Result.success(message, true);
  }
}
