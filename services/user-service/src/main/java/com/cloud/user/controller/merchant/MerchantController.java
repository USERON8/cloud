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
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
@Tag(name = "商户管理", description = "商户 REST 接口")
@Validated
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "商户参数或业务状态无效"),
  @ApiResponse(responseCode = "401", description = "需要认证"),
  @ApiResponse(responseCode = "403", description = "权限不足"),
  @ApiResponse(responseCode = "404", description = "商户资源不存在"),
  @ApiResponse(responseCode = "500", description = "商户服务内部错误")
})
public class MerchantController {

  private final MerchantService merchantService;
  private final MerchantAuthorizationService merchantAuthorizationService;

  @GetMapping
  @PreAuthorize("hasAuthority('admin:all') or hasAuthority('merchant:manage')")
  @Operation(
      summary = "查询商户列表",
      description = "按分页和状态筛选查询商户")
  public Result<PageResult<MerchantDTO>> getMerchants(
      @Parameter(description = "页码")
          @RequestParam(defaultValue = "1")
          @Min(value = 1, message = "page must be greater than 0")
          Integer page,
      @Parameter(description = "每页数量")
          @RequestParam(defaultValue = "10")
          @Min(value = 1, message = "size must be greater than 0")
          @Max(value = 100, message = "size must be less than or equal to 100")
          Integer size,
      @Parameter(description = "商户状态") @RequestParam(required = false) Integer status,
      @Parameter(description = "商户审核状态") @RequestParam(required = false)
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
  @Operation(summary = "按 ID 查询商户", description = "按商户 ID 查询商户详情")
  public Result<MerchantDTO> getMerchantById(
      @Parameter(description = "商户 ID")
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
  @Operation(summary = "创建商户", description = "创建一个商户")
  public Result<MerchantDTO> createMerchant(
      @Parameter(description = "商户请求体")
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
  @Operation(summary = "更新商户", description = "更新商户详情")
  public Result<Boolean> updateMerchant(
      @Parameter(description = "商户 ID")
          @PathVariable
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long id,
      @Parameter(description = "商户请求体")
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
  @Operation(summary = "删除商户", description = "按 ID 删除商户")
  public Result<Boolean> deleteMerchant(
      @Parameter(description = "商户 ID")
          @PathVariable
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long id) {
    boolean result = merchantService.deleteMerchant(id);
    return Result.success("merchant deleted", result);
  }

  @PostMapping("/{id}/reviews")
  @PreAuthorize("hasAuthority('merchant:audit')")
  @Operation(summary = "审核商户", description = "审核商户")
  public Result<Boolean> reviewMerchant(
      @Parameter(description = "商户 ID")
          @PathVariable
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long id,
      @Parameter(description = "审核是否通过")
          @RequestParam
          @NotNull(message = "approved is required")
          Boolean approved,
      @Parameter(description = "审核备注") @RequestParam(required = false) String remark,
      @Parameter(description = "审核原因") @RequestParam(required = false) String reason) {
    String reviewRemark = StrUtil.isNotBlank(remark) ? remark : reason;
    if (!approved && StrUtil.isBlank(reviewRemark)) {
      throw new BizException(ResultCode.BAD_REQUEST, "reason cannot be blank");
    }
    boolean result =
        Boolean.TRUE.equals(approved)
            ? merchantService.approveMerchant(id, reviewRemark)
            : merchantService.rejectMerchant(id, reviewRemark);
    return Result.success(
        Boolean.TRUE.equals(approved) ? "merchant approved" : "merchant rejected", result);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "更新商户状态", description = "更新商户状态")
  public Result<Boolean> updateMerchantStatus(
      @Parameter(description = "商户 ID")
          @PathVariable
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long id,
      @Parameter(description = "商户状态") @RequestParam Integer status) {
    boolean result = merchantService.updateMerchantStatus(id, status);
    return Result.success("merchant status updated", result);
  }

  @GetMapping("/{id}/statistics")
  @PreAuthorize(
      "hasAuthority('admin:all') "
          + "or (hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#id, authentication))")
  @Operation(summary = "查询商户统计", description = "查询单个商户统计")
  public Result<Object> getMerchantStatistics(
      @Parameter(description = "商户 ID")
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
  @Operation(summary = "批量删除商户", description = "按 ID 批量删除商户")
  public Result<Boolean> deleteMerchantsBatch(
      @Parameter(description = "商户 ID 列表")
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
      summary = "批量更新商户状态",
      description = "按 ID 批量更新商户状态")
  public Result<Boolean> updateMerchantStatusBatch(
      @Parameter(description = "商户 ID 列表")
          @RequestParam
          @NotNull(message = "merchant ids are required")
          List<
                  @NotNull(message = "merchant id cannot be null")
                  @Positive(message = "merchant id must be positive") Long>
              ids,
      @Parameter(description = "商户状态")
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

  @PostMapping("/bulk/reviews")
  @PreAuthorize("hasAuthority('merchant:audit')")
  @Operation(summary = "批量审核商户", description = "按 ID 批量审核商户")
  public Result<Boolean> reviewMerchantsBatch(
      @Parameter(description = "商户 ID 列表")
          @RequestBody
          @NotNull(message = "merchant ids are required")
          @NotEmpty(message = "merchant ids cannot be empty")
          List<
                  @NotNull(message = "merchant id cannot be null")
                  @Positive(message = "merchant id must be positive") Long>
              ids,
      @Parameter(description = "审核是否通过")
          @RequestParam
          @NotNull(message = "approved is required")
          Boolean approved,
      @Parameter(description = "审核备注") @RequestParam(required = false) String remark,
      @Parameter(description = "审核原因") @RequestParam(required = false) String reason) {
    if (ids.size() > 100) {
      throw new BizException(ResultCode.BAD_REQUEST, "batch size cannot exceed 100");
    }
    String reviewRemark = StrUtil.isNotBlank(remark) ? remark : reason;
    if (!approved && StrUtil.isBlank(reviewRemark)) {
      throw new BizException(ResultCode.BAD_REQUEST, "reason cannot be blank");
    }

    int successCount;
    if (Boolean.TRUE.equals(approved)) {
      successCount = merchantService.approveMerchantsBatch(ids, reviewRemark);
    } else {
      successCount = 0;
      for (Long merchantId : ids) {
        if (merchantService.rejectMerchant(merchantId, reviewRemark)) {
          successCount++;
        }
      }
    }
    String message = String.format("batch review completed: %d/%d", successCount, ids.size());
    return Result.success(message, true);
  }

  private static boolean isValidAuditStatus(Integer auditStatus) {
    return auditStatus == 0 || auditStatus == 1 || auditStatus == 2;
  }
}
