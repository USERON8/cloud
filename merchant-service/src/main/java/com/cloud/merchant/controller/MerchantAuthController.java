package com.cloud.merchant.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.merchant.MerchantAuthDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.merchant.converter.MerchantConverter;
import com.cloud.merchant.module.entity.MerchantAuth;
import com.cloud.merchant.service.MerchantAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家认证控制器
 * 处理商家认证信息的审核操作
 */
@Slf4j
@RestController
@RequestMapping("/merchant-auth")
@RequiredArgsConstructor
@Tag(name = "商家认证", description = "商家认证审核接口")
public class MerchantAuthController {

    private final MerchantAuthService merchantAuthService;
    private final MerchantConverter merchantConverter = MerchantConverter.INSTANCE;

    /**
     * 提交认证信息（商家专用）
     *
     * @param authDTO        认证信息
     * @param authentication 认证信息
     * @return 提交结果
     */
    @PostMapping("/submit")
    @Operation(summary = "提交认证信息", description = "商家提交认证信息等待审核")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    public Result<MerchantAuthDTO> submitAuthInfo(
            @Parameter(description = "认证信息") @RequestBody MerchantAuthDTO authDTO,
            Authentication authentication) {
        try {
            log.info("商家提交认证信息");

            Long merchantId = Long.valueOf(authentication.getName());

            // 检查是否已提交过认证信息
            LambdaQueryWrapper<MerchantAuth> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MerchantAuth::getMerchantId, merchantId);
            MerchantAuth existingAuth = merchantAuthService.getOne(queryWrapper);

            if (existingAuth != null && existingAuth.getAuthStatus() != 2) {
                // 如果已提交且不是被拒绝状态，则不允许重复提交
                log.warn("认证信息已存在，商家ID: {}", merchantId);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "认证信息已提交，请等待审核或查看审核结果");
            }

            // 转换DTO为实体
            MerchantAuth auth = merchantConverter.toAuthEntity(authDTO);

            // 设置商家ID
            auth.setMerchantId(merchantId);

            // 设置认证状态为待审核
            auth.setAuthStatus(0);

            // 保存或更新认证信息
            boolean saved = merchantAuthService.saveOrUpdate(auth);
            if (saved) {
                MerchantAuth savedAuth = merchantAuthService.getOne(queryWrapper);
                MerchantAuthDTO savedAuthDTO = merchantConverter.toAuthDTO(savedAuth);
                log.info("提交认证信息成功，商家ID: {}", merchantId);
                return Result.success(savedAuthDTO);
            } else {
                log.error("提交认证信息失败，商家ID: {}", merchantId);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "提交认证信息失败");
            }
        } catch (Exception e) {
            log.error("提交认证信息失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "提交认证信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前商家的认证信息（商家专用）
     *
     * @param authentication 认证信息
     * @return 认证信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前商家认证信息", description = "获取当前认证商家的认证信息")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    public Result<MerchantAuthDTO> getCurrentMerchantAuth(Authentication authentication) {
        try {
            log.info("获取当前商家认证信息");

            Long merchantId = Long.valueOf(authentication.getName());

            LambdaQueryWrapper<MerchantAuth> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MerchantAuth::getMerchantId, merchantId);
            MerchantAuth auth = merchantAuthService.getOne(queryWrapper);

            if (auth == null) {
                log.warn("认证信息不存在，商家ID: {}", merchantId);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "认证信息不存在");
            }

            MerchantAuthDTO authDTO = merchantConverter.toAuthDTO(auth);
            log.info("获取认证信息成功，商家ID: {}", merchantId);
            return Result.success(authDTO);
        } catch (Exception e) {
            log.error("获取认证信息失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取认证信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有待审核的认证信息（管理员专用）
     *
     * @return 认证信息列表
     */
    @GetMapping("/pending")
    @Operation(summary = "获取待审核认证信息", description = "获取所有待审核的商家认证信息（仅管理员可访问）")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<List<MerchantAuthDTO>> getPendingAuths() {
        try {
            log.info("获取待审核认证信息");

            LambdaQueryWrapper<MerchantAuth> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MerchantAuth::getAuthStatus, 0);
            List<MerchantAuth> auths = merchantAuthService.list(queryWrapper);
            List<MerchantAuthDTO> authDTOs = merchantConverter.toAuthDTOList(auths);

            log.info("获取待审核认证信息成功，共{}条记录", authDTOs.size());
            return Result.success(authDTOs);
        } catch (Exception e) {
            log.error("获取待审核认证信息失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取待审核认证信息失败: " + e.getMessage());
        }
    }

    /**
     * 审核商家认证信息（管理员专用）
     *
     * @param id      认证信息ID
     * @param authDTO 审核信息
     * @return 审核结果
     */
    @PutMapping("/review/{id}")
    @Operation(summary = "审核商家认证信息", description = "审核商家认证信息（仅管理员可访问）")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<MerchantAuthDTO> reviewMerchantAuth(
            @Parameter(description = "认证信息ID") @PathVariable Long id,
            @Parameter(description = "审核信息") @RequestBody MerchantAuthDTO authDTO) {
        try {
            log.info("审核商家认证信息，认证ID: {}", id);

            // 验证认证信息是否存在
            MerchantAuth existingAuth = merchantAuthService.getById(id);
            if (existingAuth == null) {
                log.warn("认证信息不存在，认证ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "认证信息不存在");
            }

            // 更新审核状态和备注
            existingAuth.setAuthStatus(authDTO.getAuthStatus());
            existingAuth.setAuthRemark(authDTO.getAuthRemark());

            boolean updated = merchantAuthService.updateById(existingAuth);
            if (updated) {
                MerchantAuth updatedAuth = merchantAuthService.getById(id);
                MerchantAuthDTO updatedAuthDTO = merchantConverter.toAuthDTO(updatedAuth);
                log.info("审核商家认证信息成功，认证ID: {}，状态: {}", id, authDTO.getAuthStatus());
                return Result.success(updatedAuthDTO);
            } else {
                log.error("审核商家认证信息失败，认证ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核商家认证信息失败");
            }
        } catch (Exception e) {
            log.error("审核商家认证信息失败，认证ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核商家认证信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有认证信息（管理员专用）
     *
     * @return 认证信息列表
     */
    @GetMapping
    @Operation(summary = "获取所有认证信息", description = "获取所有商家认证信息（仅管理员可访问）")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<List<MerchantAuthDTO>> getAllAuths() {
        try {
            log.info("获取所有认证信息");

            List<MerchantAuth> auths = merchantAuthService.list();
            List<MerchantAuthDTO> authDTOs = merchantConverter.toAuthDTOList(auths);

            log.info("获取所有认证信息成功，共{}条记录", authDTOs.size());
            return Result.success(authDTOs);
        } catch (Exception e) {
            log.error("获取所有认证信息失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取所有认证信息失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取认证信息（管理员专用）
     *
     * @param id 认证信息ID
     * @return 认证信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取认证信息", description = "根据ID获取商家认证信息（仅管理员可访问）")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<MerchantAuthDTO> getAuthById(
            @Parameter(description = "认证信息ID") @PathVariable Long id) {
        try {
            log.info("获取认证信息，认证ID: {}", id);

            MerchantAuth auth = merchantAuthService.getById(id);
            if (auth == null) {
                log.warn("认证信息不存在，认证ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "认证信息不存在");
            }

            MerchantAuthDTO authDTO = merchantConverter.toAuthDTO(auth);
            log.info("获取认证信息成功，认证ID: {}", id);
            return Result.success(authDTO);
        } catch (Exception e) {
            log.error("获取认证信息失败，认证ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取认证信息失败: " + e.getMessage());
        }
    }
}