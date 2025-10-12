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
import org.springframework.web.bind.annotation.*;

/**
 * 管理员RESTful API控制器
 * 提供管理员资源的CRUD操作
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "管理员服务", description = "管理员资源的RESTful API接口")
public class AdminController {

    private final AdminService adminService;

    /**
     * 分页查询管理员
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "分页查询管理员", description = "获取管理员列表，支持分页")
    public Result<PageResult<AdminDTO>> getAdmins(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于0") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页数量必须大于0")
            @Max(value = 100, message = "每页数量不能超过100") Integer size,
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
            log.error("分页查询管理员失败", e);
            return Result.error("分页查询管理员失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取管理员详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "获取管理员详情", description = "根据管理员ID获取详细信息")
    public Result<AdminDTO> getAdminById(
            @Parameter(description = "管理员ID") @PathVariable
            @NotNull(message = "管理员ID不能为空")
            @Positive(message = "管理员ID必须为正整数") Long id,
            Authentication authentication) {

        try {
            AdminDTO admin = adminService.getAdminById(id);
            if (admin == null) {
                return Result.error("管理员不存在");
            }
            return Result.success("查询成功", admin);
        } catch (Exception e) {
            log.error("获取管理员详情失败，管理员ID: {}", id, e);
            return Result.error("获取管理员详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建管理员
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "创建管理员", description = "创建新的管理员")
    public Result<AdminDTO> createAdmin(
            @Parameter(description = "管理员信息") @RequestBody
            @Valid @NotNull(message = "管理员信息不能为空") AdminDTO adminDTO) {

        try {
            AdminDTO created = adminService.createAdmin(adminDTO);
            return Result.success("管理员创建成功", created);
        } catch (Exception e) {
            log.error("创建管理员失败", e);
            return Result.error("创建管理员失败: " + e.getMessage());
        }
    }

    /**
     * 更新管理员信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "更新管理员信息", description = "更新管理员信息")
    public Result<Boolean> updateAdmin(
            @Parameter(description = "管理员ID") @PathVariable Long id,
            @Parameter(description = "管理员信息") @RequestBody
            @Valid @NotNull(message = "管理员信息不能为空") AdminDTO adminDTO,
            Authentication authentication) {

        adminDTO.setId(id);

        try {
            boolean result = adminService.updateAdmin(adminDTO);
            return Result.success("管理员更新成功", result);
        } catch (Exception e) {
            log.error("更新管理员失败，管理员ID: {}", id, e);
            return Result.error("更新管理员失败: " + e.getMessage());
        }
    }

    /**
     * 删除管理员
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "删除管理员", description = "删除管理员")
    public Result<Boolean> deleteAdmin(
            @Parameter(description = "管理员ID") @PathVariable
            @NotNull(message = "管理员ID不能为空") Long id) {

        try {
            boolean result = adminService.deleteAdmin(id);
            return Result.success("删除成功", result);
        } catch (Exception e) {
            log.error("删除管理员失败，管理员ID: {}", id, e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新管理员状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "更新管理员状态", description = "启用或禁用管理员")
    public Result<Boolean> updateAdminStatus(
            @Parameter(description = "管理员ID") @PathVariable Long id,
            @Parameter(description = "管理员状态") @RequestParam Integer status) {

        try {
            boolean result = adminService.updateAdminStatus(id, status);
            return Result.success("状态更新成功", result);
        } catch (Exception e) {
            log.error("更新管理员状态失败，管理员ID: {}, 状态: {}", id, status, e);
            return Result.error("更新状态失败: " + e.getMessage());
        }
    }

    /**
     * 重置管理员密码
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "重置管理员密码", description = "重置管理员密码为默认密码")
    public Result<Boolean> resetPassword(
            @Parameter(description = "管理员ID") @PathVariable Long id) {

        try {
            // 重置为默认密码
            boolean result = adminService.resetPassword(id, "123456");
            return Result.success("密码重置成功", result);
        } catch (Exception e) {
            log.error("重置密码失败，管理员ID: {}", id, e);
            return Result.error("密码重置失败: " + e.getMessage());
        }
    }
}
