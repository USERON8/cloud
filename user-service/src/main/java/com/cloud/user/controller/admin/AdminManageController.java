package com.cloud.user.controller.admin;

import com.cloud.common.annotation.RequireScope;
import com.cloud.common.annotation.RequireUserType;
import com.cloud.common.annotation.RequireUserType.UserType;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.result.Result;
import com.cloud.user.converter.AdminConverter;
import com.cloud.user.module.entity.Admin;
import com.cloud.user.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin/manage")
@RequiredArgsConstructor
@Tag(name = "管理员管理", description = "管理员创建、更新、删除等相关操作")
public class AdminManageController {
    private final AdminService adminService;
    private final PasswordEncoder passwordEncoder;
    private final AdminConverter adminConverter;

    @PostMapping("/create")
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:write")
    @Operation(summary = "创建管理员", description = "创建一个新的管理员账户")
    public Result<AdminDTO> create(@RequestBody
                                   @Parameter(description = "管理员信息")
                                   @Valid @NotNull(message = "管理员信息不能为空") AdminDTO adminDTO) {
        // 密码加密
        Admin admin = adminConverter.toEntity(adminDTO);
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));

        // 保存管理员
        adminService.save(admin);

        // 返回保存后的管理员信息
        AdminDTO result = adminConverter.toDTO(admin);
        return Result.success("创建成功", result);
    }

    @PutMapping("/update/{id}")
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:write")
    @Operation(summary = "更新管理员", description = "根据ID更新管理员信息")
    public Result<AdminDTO> update(@PathVariable("id")
                                   @Parameter(description = "管理员ID")
                                   @NotNull(message = "管理员ID不能为空") Long id,
                                   @RequestBody
                                   @Parameter(description = "管理员信息")
                                   @Valid @NotNull(message = "管理员信息不能为空") AdminDTO adminDTO) {
        // 设置ID
        adminDTO.setId(id);

        // 更新管理员
        Admin admin = adminConverter.toEntity(adminDTO);
        adminService.updateById(admin);

        // 返回更新后的管理员信息
        AdminDTO result = adminConverter.toDTO(admin);
        return Result.success("更新成功", result);
    }

    @DeleteMapping("/delete/{id}")
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:write")
    @Operation(summary = "删除管理员", description = "根据ID删除管理员")
    public Result<Boolean> delete(@PathVariable("id")
                                  @Parameter(description = "管理员ID")
                                  @NotNull(message = "管理员ID不能为空") Long id) {
        boolean result = adminService.removeById(id);
        return Result.success("删除成功", result);
    }

    @PutMapping("/changeStatus/{id}")
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:write")
    @Operation(summary = "更改管理员状态", description = "根据ID更改管理员状态")
    public Result<Boolean> changeStatus(@PathVariable("id")
                                        @Parameter(description = "管理员ID")
                                        @NotNull(message = "管理员ID不能为空") Long id,
                                        @RequestBody
                                        @Parameter(description = "管理员状态信息")
                                        @Valid @NotNull(message = "管理员状态信息不能为空") AdminDTO adminDTO) {
        Admin admin = new Admin();
        admin.setId(id);
        admin.setStatus(adminDTO.getStatus());
        boolean result = adminService.updateById(admin);
        return Result.success("状态更新成功", result);
    }
}