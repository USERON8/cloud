package com.cloud.merchant.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.merchant.MerchantDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.merchant.converter.MerchantConverter;
import com.cloud.merchant.module.entity.Merchant;
import com.cloud.merchant.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家查询控制器
 * 提供商家查询相关功能
 */
@Slf4j
@RestController
@RequestMapping("/merchant/query")
@RequiredArgsConstructor
@Tag(name = "商家查询", description = "商家查询接口")
public class MerchantQueryController {

    private final MerchantService merchantService;
    private final MerchantConverter merchantConverter = MerchantConverter.INSTANCE;

    /**
     * 获取当前商家信息
     * 只允许商家获取自己的信息
     *
     * @param authentication 认证信息
     * @return 商家信息
     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "获取当前商家信息", description = "获取当前登录商家的详细信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<MerchantDTO> getCurrentMerchant(Authentication authentication) {
        try {
            log.info("获取当前商家信息");

            Long merchantId = getCurrentMerchantId(authentication);
            Merchant merchant = merchantService.getById(merchantId);

            if (merchant == null) {
                log.warn("商家信息不存在，商家ID: {}", merchantId);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商家信息不存在");
            }

            MerchantDTO merchantDTO = merchantConverter.toDTO(merchant);
            log.info("获取商家信息成功，商家ID: {}", merchantId);
            return Result.success(merchantDTO);
        } catch (Exception e) {
            log.error("获取商家信息失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取商家信息失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取商家信息（管理员专用）
     *
     * @param id 商家ID
     * @return 商家信息
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "根据ID获取商家信息", description = "管理员根据ID获取商家详细信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<MerchantDTO> getMerchantById(
            @Parameter(description = "商家ID") @PathVariable Long id) {
        try {
            log.info("获取商家信息，商家ID: {}", id);

            Merchant merchant = merchantService.getById(id);
            if (merchant == null) {
                log.warn("商家信息不存在，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商家信息不存在");
            }

            MerchantDTO merchantDTO = merchantConverter.toDTO(merchant);
            log.info("获取商家信息成功，商家ID: {}", id);
            return Result.success(merchantDTO);
        } catch (Exception e) {
            log.error("获取商家信息失败，商家ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取商家信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有商家列表（管理员专用）
     *
     * @return 商家列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "获取所有商家列表", description = "管理员获取所有商家列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<List<MerchantDTO>> getAllMerchants() {
        try {
            log.info("获取所有商家列表");

            List<Merchant> merchants = merchantService.list();
            List<MerchantDTO> merchantDTOs = merchantConverter.toDTOList(merchants);

            log.info("获取商家列表成功，共{}条记录", merchantDTOs.size());
            return Result.success(merchantDTOs);
        } catch (Exception e) {
            log.error("获取商家列表失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取商家列表失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询商家列表（管理员专用）
     *
     * @param page   页码
     * @param size   每页数量
     * @param name   商家名称（可选）
     * @param status 商家状态（可选）
     * @return 商家列表
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "分页查询商家列表", description = "管理员分页查询商家列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PageResult.class)))
    public PageResult<MerchantDTO> getMerchants(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "商家名称") @RequestParam(required = false) String name,
            @Parameter(description = "商家状态") @RequestParam(required = false) Integer status) {
        try {
            log.info("分页查询商家列表，页码: {}，每页数量: {}，名称: {}，状态: {}", page, size, name, status);

            Page<Merchant> merchantPage = new Page<>(page, size);
            LambdaQueryWrapper<Merchant> queryWrapper = new LambdaQueryWrapper<>();

            // 添加查询条件
            if (name != null && !name.isEmpty()) {
                queryWrapper.like(Merchant::getMerchantName, name);
            }
            if (status != null) {
                queryWrapper.eq(Merchant::getStatus, status);
            }

            // 按创建时间倒序排列
            queryWrapper.orderByDesc(Merchant::getCreatedAt);

            Page<Merchant> resultPage = merchantService.page(merchantPage, queryWrapper);

            // 转换为DTO
            List<MerchantDTO> dtoList = merchantConverter.toDTOList(resultPage.getRecords());

            log.info("分页查询商家成功，共{}条记录", resultPage.getTotal());
            return PageResult.of(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), dtoList);
        } catch (Exception e) {
            log.error("分页查询商家失败", e);
            return PageResult.of(page.longValue(), size.longValue(), 0L, null);
        }
    }

    /**
     * 从认证信息中获取当前商家ID
     *
     * @param authentication 认证信息
     * @return 商家ID
     */
    private Long getCurrentMerchantId(Authentication authentication) {
        if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) {
            org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth = 
                (org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) authentication;
            String subject = jwtAuth.getToken().getSubject();
            return Long.valueOf(subject);
        }
        throw new IllegalArgumentException("无法从认证信息中获取商家ID");
    }
}