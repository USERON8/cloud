package com.cloud.user.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantAuthRequestDTO;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.converter.MerchantAuthConverter;
import com.cloud.user.module.entity.MerchantAuth;
import com.cloud.user.service.MerchantAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/merchant/auth")
@RequiredArgsConstructor
@Tag(name = "Merchant Auth", description = "Merchant authentication APIs")
public class MerchantAuthController {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;

    private final MerchantAuthService merchantAuthService;
    private final MerchantAuthConverter merchantAuthConverter;

    @PostMapping("/apply/{merchantId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Apply merchant auth", description = "Create or update merchant auth application")
    public Result<MerchantAuthDTO> applyForAuth(
            @PathVariable("merchantId")
            @Parameter(description = "Merchant ID")
            @NotNull(message = "merchant id is required") Long merchantId,
            @RequestBody
            @Parameter(description = "Merchant auth request body")
            @Valid @NotNull(message = "merchant auth request is required") MerchantAuthRequestDTO merchantAuthRequestDTO) {
        if (!SecurityPermissionUtils.isAdminOrMerchantOwner(merchantId)) {
            return Result.forbidden("no permission to apply merchant auth");
        }

        LambdaQueryWrapper<MerchantAuth> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(MerchantAuth::getMerchantId, merchantId);
        MerchantAuth existingAuth = merchantAuthService.getOne(queryWrapper);

        MerchantAuth merchantAuth = merchantAuthConverter.toEntity(merchantAuthRequestDTO);
        merchantAuth.setMerchantId(merchantId);
        merchantAuth.setAuthStatus(STATUS_PENDING);

        if (existingAuth != null) {
            merchantAuth.setId(existingAuth.getId());
            merchantAuth.setCreatedAt(existingAuth.getCreatedAt());
            merchantAuth.setUpdatedAt(LocalDateTime.now());
            merchantAuthService.updateById(merchantAuth);
        } else {
            merchantAuthService.save(merchantAuth);
        }

        return Result.success("merchant auth application submitted", merchantAuthConverter.toDTO(merchantAuth));
    }

    @GetMapping("/get/{merchantId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get merchant auth", description = "Get merchant auth information by merchant ID")
    public Result<MerchantAuthDTO> getAuthInfo(
            @PathVariable("merchantId")
            @Parameter(description = "Merchant ID")
            @NotNull(message = "merchant id is required") Long merchantId) {
        if (!SecurityPermissionUtils.isAdminOrMerchantOwner(merchantId)) {
            return Result.forbidden("no permission to query merchant auth");
        }

        LambdaQueryWrapper<MerchantAuth> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(MerchantAuth::getMerchantId, merchantId);
        MerchantAuth merchantAuth = merchantAuthService.getOne(queryWrapper);
        if (merchantAuth == null) {
            return Result.success("merchant auth not found", null);
        }
        return Result.success(merchantAuthConverter.toDTO(merchantAuth));
    }

    @DeleteMapping("/revoke/{merchantId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Revoke merchant auth", description = "Delete merchant auth application by merchant ID")
    public Result<Boolean> revokeAuth(
            @PathVariable("merchantId")
            @Parameter(description = "Merchant ID")
            @NotNull(message = "merchant id is required") Long merchantId) {
        if (!SecurityPermissionUtils.isAdminOrMerchantOwner(merchantId)) {
            return Result.forbidden("no permission to revoke merchant auth");
        }

        LambdaQueryWrapper<MerchantAuth> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(MerchantAuth::getMerchantId, merchantId);
        boolean removed = merchantAuthService.remove(queryWrapper);
        if (!removed) {
            return Result.success("merchant auth not found", false);
        }
        return Result.success("merchant auth revoked", true);
    }

    @PostMapping("/review/{merchantId}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Review merchant auth", description = "Review merchant auth application by merchant ID")
    public Result<Boolean> reviewAuth(
            @PathVariable("merchantId")
            @Parameter(description = "Merchant ID")
            @NotNull(message = "merchant id is required") Long merchantId,
            @RequestParam("authStatus")
            @Parameter(description = "Auth status")
            @NotNull(message = "auth status is required") Integer authStatus) {
        LambdaQueryWrapper<MerchantAuth> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(MerchantAuth::getMerchantId, merchantId);
        MerchantAuth merchantAuth = merchantAuthService.getOne(queryWrapper);
        if (merchantAuth == null) {
            return Result.error("merchant auth record not found");
        }

        merchantAuth.setAuthStatus(authStatus);
        merchantAuth.setUpdatedAt(LocalDateTime.now());
        boolean updated = merchantAuthService.updateById(merchantAuth);
        if (!updated) {
            return Result.error("failed to update merchant auth status");
        }

        String action = authStatus == STATUS_APPROVED ? "approved" : "updated";
        return Result.success("merchant auth " + action, true);
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "List merchant auth by status", description = "List merchant auth records by auth status")
    public Result<List<MerchantAuthDTO>> listAuthByStatus(
            @RequestParam("authStatus")
            @Parameter(description = "Auth status")
            @NotNull(message = "auth status is required") Integer authStatus) {
        LambdaQueryWrapper<MerchantAuth> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(MerchantAuth::getAuthStatus, authStatus);
        List<MerchantAuthDTO> result = merchantAuthService.list(queryWrapper).stream()
                .map(merchantAuthConverter::toDTO)
                .toList();
        return Result.success(result);
    }

    @PostMapping("/review/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Batch review merchant auth", description = "Batch review merchant auth records")
    public Result<Boolean> reviewAuthBatch(
            @RequestBody
            @Parameter(description = "Merchant IDs")
            @NotNull(message = "merchant ids are required") List<Long> merchantIds,
            @RequestParam("authStatus")
            @Parameter(description = "Auth status")
            @NotNull(message = "auth status is required") Integer authStatus) {
        if (merchantIds.isEmpty()) {
            return Result.badRequest("merchant ids cannot be empty");
        }
        if (merchantIds.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }

        int successCount = 0;
        for (Long merchantId : merchantIds) {
            try {
                LambdaQueryWrapper<MerchantAuth> queryWrapper = Wrappers.lambdaQuery();
                queryWrapper.eq(MerchantAuth::getMerchantId, merchantId);
                MerchantAuth merchantAuth = merchantAuthService.getOne(queryWrapper);
                if (merchantAuth == null) {
                    continue;
                }
                merchantAuth.setAuthStatus(authStatus);
                merchantAuth.setUpdatedAt(LocalDateTime.now());
                if (merchantAuthService.updateById(merchantAuth)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Failed to review merchant auth, merchantId={}", merchantId, e);
            }
        }

        String message = String.format("batch review completed: %d/%d", successCount, merchantIds.size());
        return Result.success(message, true);
    }
}
