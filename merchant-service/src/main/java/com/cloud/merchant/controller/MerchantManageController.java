package com.cloud.merchant.controller;

import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.merchant.MerchantDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.merchant.converter.MerchantConverter;
import com.cloud.merchant.module.entity.Merchant;
import com.cloud.merchant.service.MerchantService;
import com.cloud.merchant.service.impl.MerchantLogMessageService;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家管理控制器
 * 处理商家信息的增删改查操作，只允许商家操作自己的信息
 */
@Slf4j
@RestController
@RequestMapping("/merchant/manage")
@RequiredArgsConstructor
@Tag(name = "商家管理", description = "商家信息管理接口")
public class MerchantManageController {

    private final MerchantService merchantService;
    private final MerchantConverter merchantConverter = MerchantConverter.INSTANCE;
    private final MerchantLogMessageService merchantLogMessageService;

    /**
     * 获取当前商家信息
     * 只允许商家获取自己的信息
     *
     * @param authentication 认证信息
     * @return 商家信息
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MERCHANT')")
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
     * 更新当前商家信息
     * 只允许商家更新自己的信息
     *
     * @param merchantDTO    商家信息
     * @param authentication 认证信息
     * @return 更新结果
     */
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('MERCHANT')")
    @Operation(summary = "更新当前商家信息", description = "更新当前登录商家的信息")
    @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<MerchantDTO> updateCurrentMerchant(
            @Parameter(description = "商家信息") @RequestBody MerchantDTO merchantDTO,
            Authentication authentication) {
        try {
            log.info("更新当前商家信息");

            Long merchantId = getCurrentMerchantId(authentication);

            // 验证商家是否存在
            Merchant existingMerchant = merchantService.getById(merchantId);
            if (existingMerchant == null) {
                log.warn("商家信息不存在，商家ID: {}", merchantId);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商家信息不存在");
            }

            // 转换DTO为实体
            Merchant merchant = merchantConverter.toEntity(merchantDTO);

            // 确保只能更新自己的信息
            merchant.setId(merchantId);

            // 不能更新用户名和密码
            merchant.setUsername(null);
            merchant.setPassword(null);

            boolean updated = merchantService.updateById(merchant);
            if (updated) {
                Merchant updatedMerchant = merchantService.getById(merchantId);
                MerchantDTO updatedMerchantDTO = merchantConverter.toDTO(updatedMerchant);
                
                // 异步发送商家变更消息到日志服务
                merchantLogMessageService.sendMerchantChangeMessage(
                        updatedMerchant.getId(),
                        updatedMerchant.getMerchantName(),
                        existingMerchant.getStatus(), // 变更前状态
                        updatedMerchant.getStatus(),  // 变更后状态
                        2, // 2表示更新商家
                        "system" // 操作人，实际项目中应该从上下文中获取
                );
                
                log.info("更新商家信息成功，商家ID: {}", merchantId);
                return Result.success(updatedMerchantDTO);
            } else {
                log.error("更新商家信息失败，商家ID: {}", merchantId);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新商家信息失败");
            }
        } catch (Exception e) {
            log.error("更新商家信息失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新商家信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取商家列表（管理员专用）
     *
     * @return 商家列表
     */
    @GetMapping
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
     * 更新商家信息（管理员专用）
     *
     * @param id          商家ID
     * @param merchantDTO 商家信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "更新商家信息", description = "管理员更新商家信息")
    @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<MerchantDTO> updateMerchant(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Parameter(description = "商家信息") @RequestBody MerchantDTO merchantDTO) {
        try {
            log.info("更新商家信息，商家ID: {}", id);

            // 验证商家是否存在
            Merchant existingMerchant = merchantService.getById(id);
            if (existingMerchant == null) {
                log.warn("商家信息不存在，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商家信息不存在");
            }

            // 转换DTO为实体
            Merchant merchant = merchantConverter.toEntity(merchantDTO);

            // 确保只能更新指定的商家
            merchant.setId(id);

            // 不能更新用户名和密码
            merchant.setUsername(null);
            merchant.setPassword(null);

            boolean updated = merchantService.updateById(merchant);
            if (updated) {
                Merchant updatedMerchant = merchantService.getById(id);
                MerchantDTO updatedMerchantDTO = merchantConverter.toDTO(updatedMerchant);
                
                // 异步发送商家变更消息到日志服务
                merchantLogMessageService.sendMerchantChangeMessage(
                        updatedMerchant.getId(),
                        updatedMerchant.getMerchantName(),
                        existingMerchant.getStatus(), // 变更前状态
                        updatedMerchant.getStatus(),  // 变更后状态
                        2, // 2表示更新商家
                        "system" // 操作人，实际项目中应该从上下文中获取
                );
                
                log.info("更新商家信息成功，商家ID: {}", id);
                return Result.success(updatedMerchantDTO);
            } else {
                log.error("更新商家信息失败，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新商家信息失败");
            }
        } catch (Exception e) {
            log.error("更新商家信息失败，商家ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新商家信息失败: " + e.getMessage());
        }
    }

    /**
     * 从认证信息中获取当前商家ID
     *
     * @param authentication 认证信息
     * @return 商家ID
     */
    private Long getCurrentMerchantId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String subject = jwtAuth.getToken().getSubject();
            return Long.valueOf(subject);
        }
        throw new IllegalArgumentException("无法从认证信息中获取商家ID");
    }
}