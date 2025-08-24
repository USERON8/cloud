package com.cloud.admin.controller;

import com.cloud.admin.module.entity.Admin;
import com.cloud.admin.service.AdminService;
import com.cloud.common.domain.Result;
import com.cloud.common.enums.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员控制器
 * 实现管理员基础功能
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "管理员", description = "管理员相关接口")
public class AdminController {

    private final AdminService adminService;

    /**
     * 获取管理员信息
     *
     * @param adminId 管理员ID
     * @return 管理员信息
     */
    @GetMapping("/info/{adminId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "获取管理员信息", description = "根据ID获取管理员信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Admin> getAdminInfo(
            @Parameter(description = "管理员ID") @PathVariable Long adminId) {
        log.info("获取管理员信息，管理员ID: {}", adminId);

        Admin admin = adminService.getById(adminId);
        if (admin == null) {
            log.warn("管理员不存在，管理员ID: {}", adminId);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "管理员不存在");
        }

        log.info("获取管理员信息成功，管理员ID: {}", adminId);
        return Result.success(admin);
    }
}