package com.cloud.user.controller.merchant;

import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantAuthFileUploadDTO;
import com.cloud.common.domain.dto.user.MerchantAuthRequestDTO;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.service.MerchantAuthService;
import com.cloud.user.service.MerchantService;
import com.cloud.user.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/merchant/auth")
@RequiredArgsConstructor
@Tag(name = "Merchant Auth", description = "Merchant authentication APIs")
public class MerchantAuthController {

  private static final int STATUS_PENDING = 0;
  private static final int STATUS_APPROVED = 1;
  private static final int STATUS_REJECTED = 2;

  private final MerchantAuthService merchantAuthService;
  private final MerchantService merchantService;
  private final MinioService minioService;

  @Value("${user.auth.list.max-size:200}")
  private Integer authListMaxSize;

  @Value("${minio.cert-bucket-name:cloud-shop-certs}")
  private String certBucketName;

  @PostMapping("/apply/{merchantId}")
  @PreAuthorize(
      "hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#merchantId, authentication)")
  @Operation(
      summary = "Apply merchant auth",
      description = "Create or update merchant auth application")
  public Result<MerchantAuthDTO> applyForAuth(
      @PathVariable("merchantId")
          @Parameter(description = "Merchant ID")
          @NotNull(message = "merchant id is required")
          Long merchantId,
      @RequestBody
          @Parameter(description = "Merchant auth request body")
          @Valid
          @NotNull(message = "merchant auth request is required")
          MerchantAuthRequestDTO merchantAuthRequestDTO) {
    if (!SecurityPermissionUtils.isAdminOrMerchantOwner(merchantId)) {
      return Result.forbidden("no permission to apply merchant auth");
    }
    if (merchantService.getById(merchantId) == null) {
      return Result.notFound("merchant not found");
    }

    MerchantAuthDTO existingAuth =
        merchantAuthService.getMerchantAuthByMerchantIdWithCache(merchantId);

    String normalizedLicenseUrl =
        normalizeBusinessLicenseUrl(
            merchantAuthRequestDTO.getBusinessLicenseUrl(),
            existingAuth == null ? null : existingAuth.getBusinessLicenseUrl());
    if (normalizedLicenseUrl == null || normalizedLicenseUrl.isBlank()) {
      return Result.badRequest("invalid business license url");
    }

    MerchantAuthDTO savedAuth =
        merchantAuthService.applyForAuth(
            merchantId, merchantAuthRequestDTO, STATUS_PENDING, normalizedLicenseUrl);
    if (savedAuth == null) {
      return Result.error(
          existingAuth != null
              ? "failed to update merchant auth application"
              : "failed to submit merchant auth application");
    }

    try {
      merchantService.updateMerchantAuditStatus(merchantId, STATUS_PENDING);
    } catch (Exception e) {
      log.error("Failed to set merchant status pending, merchantId={}", merchantId, e);
      return Result.error("merchant auth submitted but failed to update merchant status");
    }

    return Result.success("merchant auth application submitted", savedAuth);
  }

  @PostMapping("/upload/license/{merchantId}")
  @PreAuthorize(
      "hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#merchantId, authentication)")
  @Operation(
      summary = "Upload business license",
      description = "Upload business license for merchant auth")
  public Result<MerchantAuthFileUploadDTO> uploadBusinessLicense(
      @PathVariable("merchantId")
          @Parameter(description = "Merchant ID")
          @NotNull(message = "merchant id is required")
          Long merchantId,
      @RequestPart("file") MultipartFile file) {
    if (!SecurityPermissionUtils.isAdminOrMerchantOwner(merchantId)) {
      return Result.forbidden("no permission to upload business license");
    }
    if (merchantService.getById(merchantId) == null) {
      return Result.notFound("merchant not found");
    }

    try {
      String objectName = minioService.uploadBusinessLicense(merchantId, file);
      merchantAuthService.updateBusinessLicenseUrlIfExists(merchantId, objectName);
      String previewUrl = minioService.getBusinessLicensePresignedUrl(objectName);
      MerchantAuthFileUploadDTO response = new MerchantAuthFileUploadDTO();
      response.setFileKey(objectName);
      response.setPreviewUrl(previewUrl);
      return Result.success("business license uploaded", response);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid business license upload, merchantId={}", merchantId, e);
      return Result.badRequest("invalid business license upload request");
    } catch (Exception e) {
      log.error("Failed to upload business license, merchantId={}", merchantId, e);
      return Result.error("failed to upload business license");
    }
  }

  @GetMapping("/get/{merchantId}")
  @PreAuthorize(
      "hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#merchantId, authentication)")
  @Operation(
      summary = "Get merchant auth",
      description = "Get merchant auth information by merchant ID")
  public Result<MerchantAuthDTO> getAuthInfo(
      @PathVariable("merchantId")
          @Parameter(description = "Merchant ID")
          @NotNull(message = "merchant id is required")
          Long merchantId) {
    if (!SecurityPermissionUtils.isAdminOrMerchantOwner(merchantId)) {
      return Result.forbidden("no permission to query merchant auth");
    }

    MerchantAuthDTO merchantAuth =
        merchantAuthService.getMerchantAuthByMerchantIdWithCache(merchantId);
    if (merchantAuth == null) {
      return Result.success("merchant auth not found", null);
    }
    return Result.success(enrichBusinessLicenseUrl(merchantAuth));
  }

  @DeleteMapping("/revoke/{merchantId}")
  @PreAuthorize(
      "hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#merchantId, authentication)")
  @Operation(
      summary = "Revoke merchant auth",
      description = "Delete merchant auth application by merchant ID")
  public Result<Boolean> revokeAuth(
      @PathVariable("merchantId")
          @Parameter(description = "Merchant ID")
          @NotNull(message = "merchant id is required")
          Long merchantId) {
    if (!SecurityPermissionUtils.isAdminOrMerchantOwner(merchantId)) {
      return Result.forbidden("no permission to revoke merchant auth");
    }
    boolean removed = merchantAuthService.removeByMerchantId(merchantId);
    if (!removed) {
      return Result.success("merchant auth not found", false);
    }

    try {
      merchantService.updateMerchantAuditStatus(merchantId, STATUS_PENDING);
    } catch (Exception e) {
      log.error("Failed to reset merchant status after revoke, merchantId={}", merchantId, e);
      return Result.error("merchant auth revoked but failed to update merchant status");
    }

    return Result.success("merchant auth revoked", true);
  }

  @PostMapping("/review/{merchantId}")
  @PreAuthorize("hasAuthority('merchant:audit')")
  @Operation(
      summary = "Review merchant auth",
      description = "Review merchant auth application by merchant ID")
  public Result<Boolean> reviewAuth(
      @PathVariable("merchantId")
          @Parameter(description = "Merchant ID")
          @NotNull(message = "merchant id is required")
          Long merchantId,
      @RequestParam("authStatus")
          @Parameter(description = "Auth status")
          @NotNull(message = "auth status is required")
          Integer authStatus,
      @RequestParam(value = "remark", required = false) @Parameter(description = "Review remark")
          String remark) {
    if (!isReviewStatus(authStatus)) {
      return Result.badRequest("auth status must be 1(approved) or 2(rejected)");
    }
    if (merchantService.getById(merchantId) == null) {
      return Result.notFound("merchant not found");
    }

    MerchantAuthDTO merchantAuth =
        merchantAuthService.getMerchantAuthByMerchantIdWithCache(merchantId);
    if (merchantAuth == null) {
      return Result.error("merchant auth record not found");
    }

    boolean updated = merchantAuthService.updateAuthStatus(merchantId, authStatus, remark);
    if (!updated) {
      return Result.error("failed to update merchant auth status");
    }

    try {
      merchantService.updateMerchantAuditStatus(merchantId, authStatus);
    } catch (Exception e) {
      log.error(
          "Failed to update merchant status after auth review, merchantId={}, authStatus={}",
          merchantId,
          authStatus,
          e);
      return Result.error("merchant auth reviewed but failed to update merchant status");
    }

    String action = authStatus == STATUS_APPROVED ? "approved" : "rejected";
    return Result.success("merchant auth " + action, true);
  }

  @GetMapping("/list")
  @PreAuthorize("hasAuthority('merchant:audit')")
  @Operation(
      summary = "List merchant auth by status",
      description = "List merchant auth records by auth status")
  public Result<List<MerchantAuthDTO>> listAuthByStatus(
      @RequestParam("authStatus")
          @Parameter(description = "Auth status")
          @NotNull(message = "auth status is required")
          Integer authStatus) {
    if (!isValidAuthStatus(authStatus)) {
      return Result.badRequest("invalid auth status");
    }

    int effectiveLimit = (authListMaxSize == null || authListMaxSize <= 0) ? 200 : authListMaxSize;
    List<MerchantAuthDTO> result =
        merchantAuthService.listByAuthStatus(authStatus, effectiveLimit).stream()
            .map(this::enrichBusinessLicenseUrl)
            .toList();
    return Result.success(result);
  }

  private MerchantAuthDTO enrichBusinessLicenseUrl(MerchantAuthDTO merchantAuthDTO) {
    if (merchantAuthDTO == null) {
      return null;
    }
    String businessLicenseUrl = merchantAuthDTO.getBusinessLicenseUrl();
    if (businessLicenseUrl == null || businessLicenseUrl.isBlank()) {
      return merchantAuthDTO;
    }
    if (isHttpUrl(businessLicenseUrl)) {
      return merchantAuthDTO;
    }
    try {
      merchantAuthDTO.setBusinessLicenseUrl(
          minioService.getBusinessLicensePresignedUrl(businessLicenseUrl));
    } catch (Exception e) {
      log.warn("Failed to sign business license url, value={}", businessLicenseUrl, e);
    }
    return merchantAuthDTO;
  }

  private String normalizeBusinessLicenseUrl(
      String businessLicenseUrl, String existingBusinessLicenseUrl) {
    if (businessLicenseUrl == null || businessLicenseUrl.isBlank()) {
      return existingBusinessLicenseUrl;
    }
    if (!isHttpUrl(businessLicenseUrl)) {
      return businessLicenseUrl;
    }
    if (existingBusinessLicenseUrl != null
        && !existingBusinessLicenseUrl.isBlank()
        && !isHttpUrl(existingBusinessLicenseUrl)) {
      return existingBusinessLicenseUrl;
    }
    String objectName = extractObjectNameFromUrl(businessLicenseUrl);
    return objectName == null || objectName.isBlank() ? null : objectName;
  }

  private String extractObjectNameFromUrl(String url) {
    if (url == null || url.isBlank() || certBucketName == null || certBucketName.isBlank()) {
      return null;
    }
    String bucketSegment = "/" + certBucketName + "/";
    int bucketIndex = url.indexOf(bucketSegment);
    if (bucketIndex < 0) {
      return null;
    }
    String objectPart = url.substring(bucketIndex + bucketSegment.length());
    int queryIndex = objectPart.indexOf('?');
    if (queryIndex >= 0) {
      objectPart = objectPart.substring(0, queryIndex);
    }
    return objectPart.isBlank() ? null : objectPart;
  }

  private boolean isHttpUrl(String value) {
    return value.startsWith("http://") || value.startsWith("https://");
  }

  @PostMapping("/review/batch")
  @PreAuthorize("hasAuthority('merchant:audit')")
  @Operation(
      summary = "Batch review merchant auth",
      description = "Batch review merchant auth records")
  public Result<Boolean> reviewAuthBatch(
      @RequestBody
          @Parameter(description = "Merchant IDs")
          @NotNull(message = "merchant ids are required")
          List<Long> merchantIds,
      @RequestParam("authStatus")
          @Parameter(description = "Auth status")
          @NotNull(message = "auth status is required")
          Integer authStatus,
      @RequestParam(value = "remark", required = false) @Parameter(description = "Review remark")
          String remark) {
    if (merchantIds.isEmpty()) {
      return Result.badRequest("merchant ids cannot be empty");
    }
    if (merchantIds.size() > 100) {
      return Result.badRequest("batch size cannot exceed 100");
    }
    if (!isReviewStatus(authStatus)) {
      return Result.badRequest("auth status must be 1(approved) or 2(rejected)");
    }

    int successCount = 0;
    for (Long merchantId : merchantIds) {
      try {
        if (merchantService.getById(merchantId) == null) {
          continue;
        }

        MerchantAuthDTO merchantAuth =
            merchantAuthService.getMerchantAuthByMerchantIdWithCache(merchantId);
        if (merchantAuth == null) {
          continue;
        }

        if (merchantAuthService.updateAuthStatus(merchantId, authStatus, remark)
            && merchantService.updateMerchantAuditStatus(merchantId, authStatus)) {
          successCount++;
        }
      } catch (Exception e) {
        log.error("Failed to review merchant auth, merchantId={}", merchantId, e);
      }
    }

    String message =
        String.format("batch review completed: %d/%d", successCount, merchantIds.size());
    return Result.success(message, true);
  }

  private static boolean isValidAuthStatus(Integer authStatus) {
    if (authStatus == null) {
      return false;
    }
    return authStatus == STATUS_PENDING
        || authStatus == STATUS_APPROVED
        || authStatus == STATUS_REJECTED;
  }

  private static boolean isReviewStatus(Integer authStatus) {
    if (authStatus == null) {
      return false;
    }
    return authStatus == STATUS_APPROVED || authStatus == STATUS_REJECTED;
  }
}
