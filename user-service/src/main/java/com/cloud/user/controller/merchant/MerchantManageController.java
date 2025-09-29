package com.cloud.user.controller.merchant;

import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.result.Result;
import com.cloud.common.utils.UserContextUtils;
import com.cloud.user.converter.MerchantAuthConverter;
import com.cloud.user.converter.MerchantConverter;
import com.cloud.user.module.entity.Merchant;
import com.cloud.user.module.entity.MerchantAuth;
import com.cloud.user.service.MerchantAuthService;
import com.cloud.user.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/merchants")
@RequiredArgsConstructor
@Tag(name = "商家管理", description = "商家审核、店铺管理等相关操作")
public class MerchantManageController {

    private final MerchantService merchantService;
    private final MerchantAuthService merchantAuthService;
    private final MerchantConverter merchantConverter;
    private final MerchantAuthConverter merchantAuthConverter;

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "审核通过商家", description = "审核通过商家的申请")
    public Result<MerchantDTO> approveMerchant(@PathVariable
                                               @Parameter(description = "商家ID")
                                               @NotNull(message = "商家ID不能为空") Long id) {


        Merchant merchant = merchantService.getById(id);
        if (merchant == null) {
            return Result.error("商家不存在");
        }
        merchant.setStatus(1); // 设置为启用状态
        merchant.setUpdatedAt(LocalDateTime.now());
        boolean updated = merchantService.updateById(merchant);
        if (updated) {
            MerchantDTO merchantDTO = merchantConverter.toDTO(merchant);
            return Result.success("审核通过", merchantDTO);
        } else {
            return Result.error("审核失败");
        }
    }

    @PutMapping("/rejectMerchant/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "拒绝商家申请", description = "拒绝商家的申请")
    public Result<MerchantDTO> rejectMerchant(@PathVariable("id")
                                              @Parameter(description = "商家ID")
                                              @NotNull(message = "商家ID不能为空") Long id) {


        Merchant merchant = merchantService.getById(id);
        if (merchant == null) {
            return Result.error("商家不存在");
        }
        merchant.setStatus(0); // 设置为禁用状态
        merchant.setUpdatedAt(LocalDateTime.now());
        boolean updated = merchantService.updateById(merchant);
        if (updated) {
            MerchantDTO merchantDTO = merchantConverter.toDTO(merchant);
            return Result.success("拒绝申请", merchantDTO);
        } else {
            return Result.error("操作失败");
        }
    }

    @PutMapping("/updateMerchant/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "更新商家信息", description = "更新商家的信息")
    public Result<MerchantDTO> updateMerchant(@PathVariable("id")
                                              @Parameter(description = "商家ID")
                                              @NotNull(message = "商家ID不能为空") Long id,
                                              @RequestBody
                                              @Parameter(description = "商家信息")
                                              @Valid @NotNull(message = "商家信息不能为空") MerchantDTO merchantDTO) {

        // 权限检查：商户只能更新自己的信息，管理员可以更新任何商家信息
        String currentUserId = UserContextUtils.getCurrentUserId();
        String currentUserType = UserContextUtils.getCurrentUserType();

        // 检查是否为管理员或商户本人
        if (!"ADMIN".equals(currentUserType)) {
            // 如果不是管理员，检查是否为商户本人
            if (!"MERCHANT".equals(currentUserType) || !currentUserId.equals(id.toString())) {
                return Result.error("无权执行此操作");
            }
        }

        merchantDTO.setId(id);
        Merchant merchant = merchantConverter.toEntity(merchantDTO);
        merchant.setUpdatedAt(LocalDateTime.now());
        boolean updated = merchantService.updateById(merchant);
        if (updated) {
            Merchant updatedMerchant = merchantService.getById(id);
            MerchantDTO updatedMerchantDTO = merchantConverter.toDTO(updatedMerchant);
            return Result.success("更新成功", updatedMerchantDTO);
        } else {
            return Result.error("更新失败");
        }
    }

    @PutMapping("/reviewMerchantAuth/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "审核商家认证", description = "审核商家的认证申请")
    public Result<MerchantAuthDTO> reviewMerchantAuth(@PathVariable("id")
                                                      @Parameter(description = "认证ID")
                                                      @NotNull(message = "认证ID不能为空") Long id,
                                                      @RequestBody
                                                      @Parameter(description = "认证信息")
                                                      @Valid @NotNull(message = "认证信息不能为空") MerchantAuthDTO authDTO) {


        MerchantAuth merchantAuth = merchantAuthService.getById(id);
        if (merchantAuth == null) {
            return Result.error("认证信息不存在");
        }

        merchantAuth.setAuthStatus(authDTO.getAuthStatus());
        merchantAuth.setAuthRemark(authDTO.getAuthRemark());
        merchantAuth.setUpdatedAt(LocalDateTime.now());
        boolean updated = merchantAuthService.updateById(merchantAuth);
        if (updated) {
            MerchantAuth updatedAuth = merchantAuthService.getById(id);
            MerchantAuthDTO updatedAuthDTO = merchantAuthConverter.toDTO(updatedAuth);
            return Result.success("审核完成", updatedAuthDTO);
        } else {
            return Result.error("审核失败");
        }
    }

}