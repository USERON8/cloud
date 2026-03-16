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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "Admin resource REST APIs")
public class AdminController {

  private final AdminService adminService;

  @GetMapping
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Get admins with pagination", description = "Get paged admin list")
  public Result<PageResult<AdminDTO>> getAdmins(
      @Parameter(description = "Page number")
          @RequestParam(defaultValue = "1")
          @Min(value = 1, message = "page must be greater than 0")
          Integer page,
      @Parameter(description = "Page size")
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
  @Operation(summary = "Get admin details", description = "Get admin details by ID")
  public Result<AdminDTO> getAdminById(
      @Parameter(description = "Admin ID")
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
  @Operation(summary = "Create admin", description = "Create a new admin")
  public Result<AdminDTO> createAdmin(
      @Parameter(description = "Admin payload")
          @RequestBody
      @Valid
      @NotNull(message = "admin payload cannot be null")
          AdminUpsertRequestDTO requestDTO) {
    AdminDTO created = adminService.createAdmin(requestDTO);
    return Result.success("Admin created", created);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Update admin", description = "Update admin details")
  public Result<Boolean> updateAdmin(
      @Parameter(description = "Admin ID") @PathVariable Long id,
      @Parameter(description = "Admin payload")
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
  @Operation(summary = "Delete admin", description = "Delete admin by ID")
  public Result<Boolean> deleteAdmin(
      @Parameter(description = "Admin ID")
          @PathVariable
          @NotNull(message = "admin id cannot be null")
          Long id) {
    boolean result = adminService.deleteAdmin(id);
    return Result.success("Deleted successfully", result);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Update admin status", description = "Enable or disable admin")
  public Result<Boolean> updateAdminStatus(
      @Parameter(description = "Admin ID") @PathVariable Long id,
      @Parameter(description = "Admin status") @RequestParam Integer status) {
    boolean result = adminService.updateAdminStatus(id, status);
    return Result.success("Status updated", result);
  }

  @PostMapping("/{id}/reset-password")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Reset admin password", description = "Reset admin password to default")
  public Result<Boolean> resetPassword(@Parameter(description = "Admin ID") @PathVariable Long id) {
    String temporaryPassword =
        "Tmp#" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    boolean result = adminService.resetPassword(id, temporaryPassword);
    return Result.success("Password reset successful", result);
  }
}
