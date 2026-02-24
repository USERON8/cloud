package com.cloud.user.controller.merchant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
@Tag(name = "Merchant Management", description = "Merchant REST APIs")
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping
    @PreAuthorize("(hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')) "
            + "or (hasRole('MERCHANT') and hasAuthority('SCOPE_merchant:read'))")
    @Operation(summary = "Get merchants", description = "Get merchants with pagination and status filter")
    public Result<PageResult<MerchantDTO>> getMerchants(
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "page must be greater than 0") Integer page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size must be greater than 0")
            @Max(value = 100, message = "size must be less than or equal to 100") Integer size,
            @Parameter(description = "Merchant status")
            @RequestParam(required = false) Integer status,
            Authentication authentication) {
        try {
            if (!SecurityPermissionUtils.isAdmin(authentication)) {
                String currentUserId = SecurityPermissionUtils.getCurrentUserId(authentication);
                if (!StringUtils.hasText(currentUserId)) {
                    return Result.unauthorized("current user is not available");
                }

                Long merchantId = Long.parseLong(currentUserId);
                MerchantDTO merchant = merchantService.getMerchantById(merchantId);
                List<MerchantDTO> records = List.of();
                if (merchant != null && (status == null || status.equals(merchant.getStatus()))) {
                    records = List.of(merchant);
                }
                PageResult<MerchantDTO> result = PageResult.of(1L, size.longValue(), Long.valueOf(records.size()), records);
                return Result.success(result);
            }

            Page<MerchantDTO> pageResult = merchantService.getMerchantsPage(page, size, status);
            PageResult<MerchantDTO> result = PageResult.of(
                    pageResult.getCurrent(),
                    pageResult.getSize(),
                    pageResult.getTotal(),
                    pageResult.getRecords()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to query merchants", e);
            return Result.error("Failed to query merchants: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("(hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')) "
            + "or (hasAuthority('SCOPE_merchant:read') "
            + "and @permissionManager.isMerchantOwner(#id, authentication))")
    @Operation(summary = "Get merchant by ID", description = "Get merchant details by merchant ID")
    public Result<MerchantDTO> getMerchantById(
            @Parameter(description = "Merchant ID")
            @PathVariable
            @NotNull(message = "merchant id is required")
            @Positive(message = "merchant id must be positive") Long id,
            Authentication authentication) {
        try {
            MerchantDTO merchant = merchantService.getMerchantById(id);
            if (merchant == null) {
                return Result.notFound("merchant not found");
            }
            return Result.success("query successful", merchant);
        } catch (Exception e) {
            log.error("Failed to query merchant, id={}", id, e);
            return Result.error("Failed to query merchant: " + e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Create merchant", description = "Create a merchant")
    public Result<MerchantDTO> createMerchant(
            @Parameter(description = "Merchant payload")
            @RequestBody
            @Valid
            @NotNull(message = "merchant payload is required") MerchantDTO merchantDTO) {
        try {
            MerchantDTO created = merchantService.createMerchant(merchantDTO);
            return Result.success("merchant created", created);
        } catch (Exception e) {
            log.error("Failed to create merchant", e);
            return Result.error("Failed to create merchant: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("(hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')) "
            + "or (hasAuthority('SCOPE_merchant:write') "
            + "and @permissionManager.isMerchantOwner(#id, authentication))")
    @Operation(summary = "Update merchant", description = "Update merchant details")
    public Result<Boolean> updateMerchant(
            @Parameter(description = "Merchant ID") @PathVariable Long id,
            @Parameter(description = "Merchant payload")
            @RequestBody
            @Valid
            @NotNull(message = "merchant payload is required") MerchantDTO merchantDTO,
            Authentication authentication) {
        merchantDTO.setId(id);
        try {
            boolean result = merchantService.updateMerchant(merchantDTO);
            return Result.success("merchant updated", result);
        } catch (Exception e) {
            log.error("Failed to update merchant, id={}", id, e);
            return Result.error("Failed to update merchant: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Delete merchant", description = "Delete merchant by ID")
    public Result<Boolean> deleteMerchant(
            @Parameter(description = "Merchant ID")
            @PathVariable
            @NotNull(message = "merchant id is required") Long id) {
        try {
            boolean result = merchantService.deleteMerchant(id);
            return Result.success("merchant deleted", result);
        } catch (Exception e) {
            log.error("Failed to delete merchant, id={}", id, e);
            return Result.error("Failed to delete merchant: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Approve merchant", description = "Approve merchant")
    public Result<Boolean> approveMerchant(
            @Parameter(description = "Merchant ID") @PathVariable Long id,
            @Parameter(description = "Review remark") @RequestParam(required = false) String remark) {
        try {
            boolean result = merchantService.approveMerchant(id, remark);
            return Result.success("merchant approved", result);
        } catch (Exception e) {
            log.error("Failed to approve merchant, id={}", id, e);
            return Result.error("Failed to approve merchant: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Reject merchant", description = "Reject merchant")
    public Result<Boolean> rejectMerchant(
            @Parameter(description = "Merchant ID") @PathVariable Long id,
            @Parameter(description = "Reject reason") @RequestParam String reason) {
        try {
            boolean result = merchantService.rejectMerchant(id, reason);
            return Result.success("merchant rejected", result);
        } catch (Exception e) {
            log.error("Failed to reject merchant, id={}", id, e);
            return Result.error("Failed to reject merchant: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Update merchant status", description = "Update merchant status")
    public Result<Boolean> updateMerchantStatus(
            @Parameter(description = "Merchant ID") @PathVariable Long id,
            @Parameter(description = "Merchant status") @RequestParam Integer status) {
        try {
            boolean result = merchantService.updateMerchantStatus(id, status);
            return Result.success("merchant status updated", result);
        } catch (Exception e) {
            log.error("Failed to update merchant status, id={}, status={}", id, status, e);
            return Result.error("Failed to update merchant status: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/statistics")
    @PreAuthorize("(hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')) "
            + "or (hasAuthority('SCOPE_merchant:read') "
            + "and @permissionManager.isMerchantOwner(#id, authentication))")
    @Operation(summary = "Get merchant statistics", description = "Get statistics for one merchant")
    public Result<Object> getMerchantStatistics(
            @Parameter(description = "Merchant ID") @PathVariable Long id,
            Authentication authentication) {
        try {
            Object statistics = merchantService.getMerchantStatistics(id);
            return Result.success("query successful", statistics);
        } catch (Exception e) {
            log.error("Failed to get merchant statistics, id={}", id, e);
            return Result.error("Failed to get merchant statistics: " + e.getMessage());
        }
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Batch delete merchants", description = "Batch delete merchants by IDs")
    public Result<Boolean> deleteMerchantsBatch(
            @Parameter(description = "Merchant IDs")
            @RequestBody
            @NotNull(message = "merchant ids are required")
            @NotEmpty(message = "merchant ids cannot be empty") List<Long> ids) {
        if (ids.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }

        try {
            boolean result = merchantService.batchDeleteMerchants(ids);
            return Result.success("batch delete completed", result);
        } catch (Exception e) {
            log.error("Failed to batch delete merchants, ids={}", ids, e);
            return Result.error("Failed to batch delete merchants: " + e.getMessage());
        }
    }

    @PatchMapping("/batch/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Batch update merchant status", description = "Batch update merchant status by IDs")
    public Result<Boolean> updateMerchantStatusBatch(
            @Parameter(description = "Merchant IDs")
            @RequestParam
            @NotNull(message = "merchant ids are required") List<Long> ids,
            @Parameter(description = "Merchant status")
            @RequestParam
            @NotNull(message = "status is required") Integer status) {
        if (ids.isEmpty()) {
            return Result.badRequest("merchant ids cannot be empty");
        }
        if (ids.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }

        try {
            int successCount = 0;
            for (Long id : ids) {
                if (merchantService.updateMerchantStatus(id, status)) {
                    successCount++;
                }
            }
            String message = String.format("batch status update completed: %d/%d", successCount, ids.size());
            return Result.success(message, true);
        } catch (Exception e) {
            log.error("Failed to batch update merchant status, ids={}, status={}", ids, status, e);
            return Result.error("Failed to batch update merchant status: " + e.getMessage());
        }
    }

    @PostMapping("/batch/approve")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Batch approve merchants", description = "Batch approve merchants by IDs")
    public Result<Boolean> approveMerchantsBatch(
            @Parameter(description = "Merchant IDs")
            @RequestBody
            @NotNull(message = "merchant ids are required")
            @NotEmpty(message = "merchant ids cannot be empty") List<Long> ids,
            @Parameter(description = "Review remark") @RequestParam(required = false) String remark) {
        if (ids.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }

        try {
            int successCount = 0;
            for (Long id : ids) {
                if (merchantService.approveMerchant(id, remark)) {
                    successCount++;
                }
            }
            String message = String.format("batch approve completed: %d/%d", successCount, ids.size());
            return Result.success(message, true);
        } catch (Exception e) {
            log.error("Failed to batch approve merchants, ids={}", ids, e);
            return Result.error("Failed to batch approve merchants: " + e.getMessage());
        }
    }
}
