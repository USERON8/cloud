package com.cloud.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.user.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
@Tag(name = "管理员管理", description = "管理员资源 REST 接口")
public class AdminController {

  private final AdminService adminService;

  @GetMapping
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "分页查询管理员", description = "获取管理员分页列表")
  public Result<PageResult<AdminDTO>> getAdmins(
      @Parameter(description = "页码")
          @RequestParam(defaultValue = "1")
          @Min(value = 1, message = "page must be greater than 0")
          Integer page,
      @Parameter(description = "每页数量")
          @RequestParam(defaultValue = "10")
          @Min(value = 1, message = "size must be greater than 0")
          @Max(value = 100, message = "size must be less than or equal to 100")
          Integer size,
      Authentication authentication) {
    Page<AdminDTO> pageResult = adminService.getAdminsPage(page, size);
    PageResult<AdminDTO> result =
        PageResult.of(
            pageResult.getCurrent(),
            pageResult.getSize(),
            pageResult.getTotal(),
            pageResult.getRecords());
    return Result.success(result);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "查询管理员详情", description = "按 ID 查询管理员详情")
  public Result<AdminDTO> getAdminById(
      @Parameter(description = "管理员 ID")
          @PathVariable
          @NotNull(message = "admin id cannot be null")
          @Positive(message = "admin id must be positive")
          Long id,
      Authentication authentication) {
    AdminDTO admin = adminService.getAdminById(id);
    if (admin == null) {
      throw new BizException(ResultCode.ADMIN_NOT_FOUND);
    }
    return Result.success("Query successful", admin);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "创建管理员", description = "创建一个管理员")
  public Result<AdminDTO> createAdmin(
      @Parameter(description = "管理员请求体")
          @RequestBody
          @Valid
          @NotNull(message = "admin payload cannot be null")
          AdminUpsertRequestDTO requestDTO) {
    AdminDTO created = adminService.createAdmin(requestDTO);
    return Result.success("Admin created", created);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "更新管理员", description = "更新管理员详情")
  public Result<Boolean> updateAdmin(
      @Parameter(description = "管理员 ID") @PathVariable Long id,
      @Parameter(description = "管理员请求体")
          @RequestBody
          @Valid
          @NotNull(message = "admin payload cannot be null")
          AdminUpsertRequestDTO requestDTO,
      Authentication authentication) {
    boolean result = adminService.updateAdmin(id, requestDTO);
    return Result.success("Admin updated", result);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "删除管理员", description = "按 ID 删除管理员")
  public Result<Boolean> deleteAdmin(
      @Parameter(description = "管理员 ID")
          @PathVariable
          @NotNull(message = "admin id cannot be null")
          Long id) {
    boolean result = adminService.deleteAdmin(id);
    return Result.success("Deleted successfully", result);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "更新管理员状态", description = "启用或禁用管理员")
  public Result<Boolean> updateAdminStatus(
      @Parameter(description = "管理员 ID") @PathVariable Long id,
      @Parameter(description = "管理员状态") @RequestParam Integer status) {
    boolean result = adminService.updateAdminStatus(id, status);
    return Result.success("Status updated", result);
  }

  @PostMapping("/{id}/password-resets")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "重置管理员密码", description = "重置管理员默认密码")
  public Result<String> resetPassword(@Parameter(description = "管理员 ID") @PathVariable Long id) {
    String temporaryPassword =
        "Tmp#" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    adminService.resetPassword(id, temporaryPassword);
    return Result.success("Password reset successful", temporaryPassword);
  }
}
