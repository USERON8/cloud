package com.cloud.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.user.AdminDTO;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "Admin resource REST APIs")
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "Get admins with pagination", description = "Get paged admin list")
    public Result<PageResult<AdminDTO>> getAdmins(
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "page must be greater than 0") Integer page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size must be greater than 0")
            @Max(value = 100, message = "size must be less than or equal to 100") Integer size,
            Authentication authentication) {
        try {
            Page<AdminDTO> pageResult = adminService.getAdminsPage(page, size);
            PageResult<AdminDTO> result = PageResult.of(
                    pageResult.getCurrent(),
                    pageResult.getSize(),
                    pageResult.getTotal(),
                    pageResult.getRecords()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to query admin page", e);
            return Result.error("Failed to query admin page: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "Get admin details", description = "Get admin details by ID")
    public Result<AdminDTO> getAdminById(
            @Parameter(description = "Admin ID")
            @PathVariable
            @NotNull(message = "admin id cannot be null")
            @Positive(message = "admin id must be positive") Long id,
            Authentication authentication) {
        try {
            AdminDTO admin = adminService.getAdminById(id);
            if (admin == null) {
                return Result.error("Admin not found");
            }
            return Result.success("Query successful", admin);
        } catch (Exception e) {
            log.error("Failed to get admin details: {}", id, e);
            return Result.error("Failed to get admin details: " + e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Create admin", description = "Create a new admin")
    public Result<AdminDTO> createAdmin(
            @Parameter(description = "Admin payload")
            @RequestBody
            @Valid
            @NotNull(message = "admin payload cannot be null") AdminDTO adminDTO) {
        try {
            AdminDTO created = adminService.createAdmin(adminDTO);
            return Result.success("Admin created", created);
        } catch (Exception e) {
            log.error("Failed to create admin", e);
            return Result.error("Failed to create admin: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Update admin", description = "Update admin details")
    public Result<Boolean> updateAdmin(
            @Parameter(description = "Admin ID") @PathVariable Long id,
            @Parameter(description = "Admin payload")
            @RequestBody
            @Valid
            @NotNull(message = "admin payload cannot be null") AdminDTO adminDTO,
            Authentication authentication) {
        adminDTO.setId(id);
        try {
            boolean result = adminService.updateAdmin(adminDTO);
            return Result.success("Admin updated", result);
        } catch (Exception e) {
            log.error("Failed to update admin: {}", id, e);
            return Result.error("Failed to update admin: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Delete admin", description = "Delete admin by ID")
    public Result<Boolean> deleteAdmin(
            @Parameter(description = "Admin ID")
            @PathVariable
            @NotNull(message = "admin id cannot be null") Long id) {
        try {
            boolean result = adminService.deleteAdmin(id);
            return Result.success("Deleted successfully", result);
        } catch (Exception e) {
            log.error("Failed to delete admin: {}", id, e);
            return Result.error("Failed to delete admin: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Update admin status", description = "Enable or disable admin")
    public Result<Boolean> updateAdminStatus(
            @Parameter(description = "Admin ID") @PathVariable Long id,
            @Parameter(description = "Admin status") @RequestParam Integer status) {
        try {
            boolean result = adminService.updateAdminStatus(id, status);
            return Result.success("Status updated", result);
        } catch (Exception e) {
            log.error("Failed to update admin status: {}, status={}", id, status, e);
            return Result.error("Failed to update admin status: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Reset admin password", description = "Reset admin password to default")
    public Result<Boolean> resetPassword(
            @Parameter(description = "Admin ID") @PathVariable Long id) {
        try {
            boolean result = adminService.resetPassword(id, "123456");
            return Result.success("Password reset successful", result);
        } catch (Exception e) {
            log.error("Failed to reset admin password: {}", id, e);
            return Result.error("Failed to reset password: " + e.getMessage());
        }
    }
}
