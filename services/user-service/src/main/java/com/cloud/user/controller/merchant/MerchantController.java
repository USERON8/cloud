package com.cloud.user.controller.merchant;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.domain.dto.user.MerchantUpsertRequestDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.service.MerchantService;
import com.cloud.user.service.support.MerchantAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
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

@RestController
@RequestMapping("/api/app/merchant")
@RequiredArgsConstructor
@Tag(name = "Merchant Management", description = "Merchant REST APIs")
@Validated
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "Invalid merchant parameters or business state"),
  @ApiResponse(responseCode = "401", description = "Authentication required"),
  @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
  @ApiResponse(responseCode = "404", description = "Merchant resource not found"),
  @ApiResponse(responseCode = "500", description = "Internal merchant service error")
})
public class MerchantController {

  private final MerchantService merchantService;
  private final MerchantAuthorizationService merchantAuthorizationService;

  @GetMapping
  @PreAuthorize("hasAuthority('admin:all') or hasAuthority('merchant:manage')")
  @Operation(
      summary = "Get merchants",
      description = "Get merchants with pagination and status filter")
  public Result<PageResult<MerchantDTO>> getMerchants(
      @Parameter(description = "Page number")
          @RequestParam(defaultValue = "1")
          @Min(value = 1, message = "page must be greater than 0")
          Integer page,
      @Parameter(description = "Page size")
          @RequestParam(defaultValue = "10")
          @Min(value = 1, message = "size must be greater than 0")
          @Max(value = 100, message = "size must be less than or equal to 100")
          Integer size,
      @Parameter(description = "Merchant status") @RequestParam(required = false) Integer status,
      @Parameter(description = "Merchant audit status") @RequestParam(required = false)
          Integer auditStatus,
      Authentication authentication) {
    if (auditStatus != null && !isValidAuditStatus(auditStatus)) {
      throw new BizException(ResultCode.BAD_REQUEST, "invalid merchant audit status");
    }
    if (!SecurityPermissionUtils.isAdmin(authentication)) {
      String currentUserId = SecurityPermissionUtils.getCurrentUserId(authentication);
      if (StrUtil.isBlank(currentUserId)) {
        throw new BizException(ResultCode.UNAUTHORIZED, "current user is not available");
      }

      Long ownerUserId = Long.parseLong(currentUserId);
      MerchantDTO merchant = merchantService.getMerchantByOwnerUserId(ownerUserId);
      List<MerchantDTO> records = List.of();
      if (merchant != null
          && (status == null || status.equals(merchant.getStatus()))
          && (auditStatus == null || auditStatus.equals(merchant.getAuditStatus()))) {
        records = List.of(merchant);
      }
      PageResult<MerchantDTO> result =
          PageResult.of(1L, size.longValue(), Long.valueOf(records.size()), records);
      return Result.success(result);
    }

    Page<MerchantDTO> pageResult =
        merchantService.getMerchantsPage(page, size, status, auditStatus);
    PageResult<MerchantDTO> result =
        PageResult.of(
            pageResult.getCurrent(),
            pageResult.getSize(),
            pageResult.getTotal(),
            pageResult.getRecords());
    return Result.success(result);
  }

  @GetMapping("/{id}")
  @PreAuthorize(
      "hasAuthority('admin:all') "
          + "or (hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#id, authentication))")
  @Operation(summary = "Get merchant by ID", description = "Get merchant details by merchant ID")
  public Result<MerchantDTO> getMerchantById(
      @Parameter(description = "Merchant ID")
          @PathVariable
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long id,
      Authentication authentication) {
    merchantAuthorizationService.assertCanReadMerchant(authentication, id);
    MerchantDTO merchant = merchantService.getMerchantById(id);
    if (merchant == null) {
      throw new BizException(ResultCode.NOT_FOUND, "merchant not found");
    }
    return Result.success("query successful", merchant);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Create merchant", description = "Create a merchant")
  public Result<MerchantDTO> createMerchant(
      @Parameter(description = "Merchant payload")
          @RequestBody
          @Valid
          @NotNull(message = "merchant payload is required")
          MerchantUpsertRequestDTO requestDTO) {
    MerchantDTO created = merchantService.createMerchant(requestDTO);
    return Result.success("merchant created", created);
  }

  @PutMapping("/{id}")
  @PreAuthorize(
      "hasAuthority('admin:all') "
          + "or (hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#id, authentication))")
  @Operation(summary = "Update merchant", description = "Update merchant details")
  public Result<Boolean> updateMerchant(
      @Parameter(description = "Merchant ID")
          @PathVariable
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long id,
      @Parameter(description = "Merchant payload")
          @RequestBody
          @Valid
          @NotNull(message = "merchant payload is required")
          MerchantUpsertRequestDTO requestDTO,
      Authentication authentication) {
    merchantAuthorizationService.assertCanWriteMerchant(authentication, id);
    boolean result = merchantService.updateMerchant(id, requestDTO);
    return Result.success("merchant updated", result);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Delete merchant", description = "Delete merchant by ID")
  public Result<Boolean> deleteMerchant(
      @Parameter(description = "Merchant ID")
          @PathVariable
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long id) {
    boolean result = merchantService.deleteMerchant(id);
    return Result.success("merchant deleted", result);
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('merchant:audit')")
  @Operation(summary = "Approve merchant", description = "Approve merchant")
  public Result<Boolean> approveMerchant(
      @Parameter(description = "Merchant ID")
          @PathVariable
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long id,
      @Parameter(description = "Review remark") @RequestParam(required = false) String remark) {
    boolean result = merchantService.approveMerchant(id, remark);
    return Result.success("merchant approved", result);
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAuthority('merchant:audit')")
  @Operation(summary = "Reject merchant", description = "Reject merchant")
  public Result<Boolean> rejectMerchant(
      @Parameter(description = "Merchant ID")
          @PathVariable
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long id,
      @Parameter(description = "Reject reason")
          @RequestParam
          @NotBlank(message = "reason cannot be blank")
          @Size(max = 255, message = "reason must be less than or equal to 255 characters")
          String reason) {
    boolean result = merchantService.rejectMerchant(id, reason);
    return Result.success("merchant rejected", result);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Update merchant status", description = "Update merchant status")
  public Result<Boolean> updateMerchantStatus(
      @Parameter(description = "Merchant ID")
          @PathVariable
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long id,
      @Parameter(description = "Merchant status") @RequestParam Integer status) {
    boolean result = merchantService.updateMerchantStatus(id, status);
    return Result.success("merchant status updated", result);
  }

  @GetMapping("/{id}/statistics")
  @PreAuthorize(
      "hasAuthority('admin:all') "
          + "or (hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#id, authentication))")
  @Operation(summary = "Get merchant statistics", description = "Get statistics for one merchant")
  public Result<Object> getMerchantStatistics(
      @Parameter(description = "Merchant ID")
          @PathVariable
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long id,
      Authentication authentication) {
    merchantAuthorizationService.assertCanReadMerchant(authentication, id);
    Object statistics = merchantService.getMerchantStatistics(id);
    return Result.success("query successful", statistics);
  }

  @DeleteMapping("/batch")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Batch delete merchants", description = "Batch delete merchants by IDs")
  public Result<Boolean> deleteMerchantsBatch(
      @Parameter(description = "Merchant IDs")
          @RequestBody
          @NotNull(message = "merchant ids are required")
          @NotEmpty(message = "merchant ids cannot be empty")
          List<
                  @NotNull(message = "merchant id cannot be null")
                  @Positive(message = "merchant id must be positive") Long>
              ids) {
    if (ids.size() > 100) {
      throw new BizException(ResultCode.BAD_REQUEST, "batch size cannot exceed 100");
    }

    boolean result = merchantService.batchDeleteMerchants(ids);
    return Result.success("batch delete completed", result);
  }

  @PatchMapping("/batch/status")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(
      summary = "Batch update merchant status",
      description = "Batch update merchant status by IDs")
  public Result<Boolean> updateMerchantStatusBatch(
      @Parameter(description = "Merchant IDs")
          @RequestParam
          @NotNull(message = "merchant ids are required")
          List<
                  @NotNull(message = "merchant id cannot be null")
                  @Positive(message = "merchant id must be positive") Long>
              ids,
      @Parameter(description = "Merchant status")
          @RequestParam
          @NotNull(message = "status is required")
          Integer status) {
    if (ids.isEmpty()) {
      throw new BizException(ResultCode.BAD_REQUEST, "merchant ids cannot be empty");
    }
    if (ids.size() > 100) {
      throw new BizException(ResultCode.BAD_REQUEST, "batch size cannot exceed 100");
    }

    int successCount = merchantService.updateMerchantStatusBatch(ids, status);
    String message =
        String.format("batch status update completed: %d/%d", successCount, ids.size());
    return Result.success(message, true);
  }

  @PostMapping("/batch/approve")
  @PreAuthorize("hasAuthority('merchant:audit')")
  @Operation(summary = "Batch approve merchants", description = "Batch approve merchants by IDs")
  public Result<Boolean> approveMerchantsBatch(
      @Parameter(description = "Merchant IDs")
          @RequestBody
          @NotNull(message = "merchant ids are required")
          @NotEmpty(message = "merchant ids cannot be empty")
          List<
                  @NotNull(message = "merchant id cannot be null")
                  @Positive(message = "merchant id must be positive") Long>
              ids,
      @Parameter(description = "Review remark") @RequestParam(required = false) String remark) {
    if (ids.size() > 100) {
      throw new BizException(ResultCode.BAD_REQUEST, "batch size cannot exceed 100");
    }

    int successCount = merchantService.approveMerchantsBatch(ids, remark);
    String message = String.format("batch approve completed: %d/%d", successCount, ids.size());
    return Result.success(message, true);
  }

  private static boolean isValidAuditStatus(Integer auditStatus) {
    return auditStatus == 0 || auditStatus == 1 || auditStatus == 2;
  }
}
