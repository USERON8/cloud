package com.cloud.user.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminPageDTO;
import com.cloud.common.domain.vo.user.AdminVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.utils.PageUtils;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/query")
@RequiredArgsConstructor
@Tag(name = "管理员查询", description = "管理员信息查询相关操作")
public class AdminQueryController {
    private final AdminService adminService;
    private final AdminConverter adminConverter;

    @GetMapping("/getById/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "根据ID获取管理员信息", description = "根据管理员ID获取详细信息")
    public Result<AdminDTO> getById(@PathVariable("id")
                                    @Parameter(description = "管理员ID")
                                    @NotNull(message = "管理员ID不能为空") Long id) {
        Admin admin = adminService.getById(id);
        return Result.success("查询成功", adminConverter.toDTO(admin));
    }

    @GetMapping("/getAll")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "获取所有管理员", description = "获取系统中所有管理员的信息")
    public Result<List<AdminDTO>> getAll() {
        List<Admin> admins = adminService.list();
        return Result.success(adminConverter.toDTOList(admins));
    }

    @PostMapping("/page")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "分页查询管理员", description = "根据条件分页查询管理员信息")
    public Result<PageResult<AdminVO>> page(@RequestBody
                                            @Parameter(description = "分页查询条件")
                                            @Valid @NotNull(message = "分页查询条件不能为空") AdminPageDTO pageDTO) {
        // 构造分页对象
        Page<Admin> page = PageUtils.buildPage(pageDTO);

        // 构造查询条件
        LambdaQueryWrapper<Admin> queryWrapper = Wrappers.lambdaQuery();
        if (pageDTO.getUsername() != null && !pageDTO.getUsername().isEmpty()) {
            queryWrapper.like(Admin::getUsername, pageDTO.getUsername());
        }
        if (pageDTO.getPhone() != null && !pageDTO.getPhone().isEmpty()) {
            queryWrapper.like(Admin::getPhone, pageDTO.getPhone());
        }
        if (pageDTO.getStatus() != null) {
            queryWrapper.eq(Admin::getStatus, pageDTO.getStatus());
        }
        if (pageDTO.getRole() != null && !pageDTO.getRole().isEmpty()) {
            queryWrapper.eq(Admin::getRole, pageDTO.getRole());
        }
        queryWrapper.orderByDesc(Admin::getCreatedAt);

        // 执行分页查询
        Page<Admin> resultPage = adminService.page(page, queryWrapper);

        // 转换为VO列表
        List<AdminVO> adminVOList = adminConverter.toVOList(resultPage.getRecords());

        // 封装分页结果
        PageResult<AdminVO> pageResult = PageResult.of(
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getTotal(),
                adminVOList
        );

        return Result.success(pageResult);
    }
}