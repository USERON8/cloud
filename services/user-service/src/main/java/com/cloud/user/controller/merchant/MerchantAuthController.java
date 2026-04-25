package com.cloud.user.controller.merchant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantAuthFileUploadDTO;
import com.cloud.common.domain.dto.user.MerchantAuthRequestDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.user.service.MerchantAuthService;
import com.cloud.user.service.MerchantService;
import com.cloud.user.service.MinioService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Tag(name = "Merchant Auth", description = "Merchant authentication APIs")
@Validated
@ApiResponses({
  @ApiResponse(
      responseCode = "400",
      description = "Invalid merchant auth parameters or review state"),
  @ApiResponse(responseCode = "401", description = "Authentication required"),
  @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
  @ApiResponse(responseCode = "404", description = "Merchant or auth record not found"),
  @ApiResponse(responseCode = "500", description = "Internal merchant auth service error")
})
public class MerchantAuthController {

  private static final int STATUS_PENDING = 0;
  private static final int STATUS_APPROVED = 1;
  private static final int STATUS_REJECTED = 2;

  private final MerchantAuthService merchantAuthService;
  private final MerchantService merchantService;
  private final MinioService minioService;
  private final MerchantAuthorizationService merchantAuthorizationService;

  @Value("${user.auth.list.max-size:200}")
  private Integer authListMaxSize;

  @Value("${minio.cert-bucket-name:cloud-shop-certs}")
  private String certBucketName;

  @PutMapping("/api/merchants/{merchantId}/authentication")
  @PreAuthorize(
      "hasAuthority('admin:all') "
          + "or (hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#merchantId, authentication))")
  @Operation(
      summary = "Apply merchant auth",
      description = "Create or update merchant auth application")
  public Result<MerchantAuthDTO> applyForAuth(
      @PathVariable("merchantId")
          @Parameter(description = "Merchant ID")
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long merchantId,
      @RequestBody
          @Parameter(description = "Merchant auth request body")
          @Valid
          @NotNull(message = "merchant auth request is required")
          MerchantAuthRequestDTO merchantAuthRequestDTO,
      Authentication authentication) {
    merchantAuthorizationService.assertCanWriteMerchant(authentication, merchantId);
    if (merchantService.getById(merchantId) == null) {
      throw new BizException(ResultCode.NOT_FOUND, "merchant not found");
    }

    MerchantAuthDTO existingAuth =
        merchantAuthService.getMerchantAuthByMerchantIdWithCache(merchantId);

    String normalizedLicenseUrl =
        normalizeCertUrl(
            merchantAuthRequestDTO.getBusinessLicenseUrl(),
            existingAuth == null ? null : existingAuth.getBusinessLicenseUrl());
    if (normalizedLicenseUrl == null || normalizedLicenseUrl.isBlank()) {
      throw new BizException(ResultCode.BAD_REQUEST, "invalid business license url");
    }
    String normalizedIdCardFrontUrl =
        normalizeCertUrl(
            merchantAuthRequestDTO.getIdCardFrontUrl(),
            existingAuth == null ? null : existingAuth.getIdCardFrontUrl());
    if (normalizedIdCardFrontUrl == null || normalizedIdCardFrontUrl.isBlank()) {
      throw new BizException(ResultCode.BAD_REQUEST, "invalid id card front url");
    }
    String normalizedIdCardBackUrl =
        normalizeCertUrl(
            merchantAuthRequestDTO.getIdCardBackUrl(),
            existingAuth == null ? null : existingAuth.getIdCardBackUrl());
    if (normalizedIdCardBackUrl == null || normalizedIdCardBackUrl.isBlank()) {
      throw new BizException(ResultCode.BAD_REQUEST, "invalid id card back url");
    }

    merchantAuthRequestDTO.setIdCardFrontUrl(normalizedIdCardFrontUrl);
    merchantAuthRequestDTO.setIdCardBackUrl(normalizedIdCardBackUrl);

    MerchantAuthDTO savedAuth =
        merchantAuthService.applyForAuth(
            merchantId, merchantAuthRequestDTO, STATUS_PENDING, normalizedLicenseUrl);
    if (savedAuth == null) {
      throw new BizException(
          ResultCode.BUSINESS_ERROR,
          existingAuth != null
              ? "failed to update merchant auth application"
              : "failed to submit merchant auth application");
    }

    merchantService.updateMerchantAuditStatus(merchantId, STATUS_PENDING);

    return Result.success("merchant auth application submitted", savedAuth);
  }

  @PostMapping("/api/merchants/{merchantId}/authentication/license-files")
  @PreAuthorize(
      "hasAuthority('admin:all') "
          + "or (hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#merchantId, authentication))")
  @Operation(
      summary = "Upload business license",
      description = "Upload business license for merchant auth")
  public Result<MerchantAuthFileUploadDTO> uploadBusinessLicense(
      @PathVariable("merchantId")
          @Parameter(description = "Merchant ID")
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long merchantId,
      @RequestPart("file") @NotNull(message = "file is required") MultipartFile file,
      Authentication authentication) {
    merchantAuthorizationService.assertCanWriteMerchant(authentication, merchantId);
    if (merchantService.getById(merchantId) == null) {
      throw new BizException(ResultCode.NOT_FOUND, "merchant not found");
    }
    if (file.isEmpty()) {
      throw new BizException(ResultCode.BAD_REQUEST, "file cannot be empty");
    }

    String objectName = minioService.uploadBusinessLicense(merchantId, file);
    merchantAuthService.updateBusinessLicenseUrlIfExists(merchantId, objectName);
    String previewUrl = minioService.getCertPresignedUrl(objectName);
    MerchantAuthFileUploadDTO response = new MerchantAuthFileUploadDTO();
    response.setFileKey(objectName);
    response.setPreviewUrl(previewUrl);
    return Result.success("business license uploaded", response);
  }

  @PostMapping("/api/merchants/{merchantId}/authentication/id-card-front-files")
  @PreAuthorize(
      "hasAuthority('admin:all') "
          + "or (hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#merchantId, authentication))")
  @Operation(
      summary = "Upload ID card front",
      description = "Upload ID card front image for merchant auth")
  public Result<MerchantAuthFileUploadDTO> uploadIdCardFront(
      @PathVariable("merchantId")
          @Parameter(description = "Merchant ID")
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long merchantId,
      @RequestPart("file") @NotNull(message = "file is required") MultipartFile file,
      Authentication authentication) {
    return uploadMerchantAuthFile(
        merchantId,
        file,
        authentication,
        () -> minioService.uploadIdCardFront(merchantId, file),
        "id card front uploaded",
        objectName -> merchantAuthService.updateIdCardFrontUrlIfExists(merchantId, objectName));
  }

  @PostMapping("/api/merchants/{merchantId}/authentication/id-card-back-files")
  @PreAuthorize(
      "hasAuthority('admin:all') "
          + "or (hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#merchantId, authentication))")
  @Operation(
      summary = "Upload ID card back",
      description = "Upload ID card back image for merchant auth")
  public Result<MerchantAuthFileUploadDTO> uploadIdCardBack(
      @PathVariable("merchantId")
          @Parameter(description = "Merchant ID")
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long merchantId,
      @RequestPart("file") @NotNull(message = "file is required") MultipartFile file,
      Authentication authentication) {
    return uploadMerchantAuthFile(
        merchantId,
        file,
        authentication,
        () -> minioService.uploadIdCardBack(merchantId, file),
        "id card back uploaded",
        objectName -> merchantAuthService.updateIdCardBackUrlIfExists(merchantId, objectName));
  }

  @GetMapping("/api/merchants/{merchantId}/authentication")
  @PreAuthorize(
      "hasAuthority('admin:all') "
          + "or (hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#merchantId, authentication))")
  @Operation(
      summary = "Get merchant auth",
      description = "Get merchant auth information by merchant ID")
  public Result<MerchantAuthDTO> getAuthInfo(
      @PathVariable("merchantId")
          @Parameter(description = "Merchant ID")
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long merchantId,
      Authentication authentication) {
    merchantAuthorizationService.assertCanReadMerchant(authentication, merchantId);

    MerchantAuthDTO merchantAuth =
        merchantAuthService.getMerchantAuthByMerchantIdWithCache(merchantId);
    if (merchantAuth == null) {
      return Result.success("merchant auth not found", null);
    }
    return Result.success(enrichBusinessLicenseUrl(merchantAuth));
  }

  @DeleteMapping("/api/merchants/{merchantId}/authentication")
  @PreAuthorize(
      "hasAuthority('admin:all') "
          + "or (hasAuthority('merchant:manage') "
          + "and @permissionManager.isMerchantOwner(#merchantId, authentication))")
  @Operation(
      summary = "Revoke merchant auth",
      description = "Delete merchant auth application by merchant ID")
  public Result<Boolean> revokeAuth(
      @PathVariable("merchantId")
          @Parameter(description = "Merchant ID")
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long merchantId,
      Authentication authentication) {
    merchantAuthorizationService.assertCanWriteMerchant(authentication, merchantId);
    boolean removed = merchantAuthService.removeByMerchantId(merchantId);
    if (!removed) {
      return Result.success("merchant auth not found", false);
    }

    merchantService.updateMerchantAuditStatus(merchantId, STATUS_PENDING);

    return Result.success("merchant auth revoked", true);
  }

  @PostMapping("/api/merchants/{merchantId}/authentication/reviews")
  @PreAuthorize("hasAuthority('admin:all') or hasAuthority('merchant:audit')")
  @Operation(
      summary = "Review merchant auth",
      description = "Review merchant auth application by merchant ID")
  public Result<Boolean> reviewAuth(
      @PathVariable("merchantId")
          @Parameter(description = "Merchant ID")
          @NotNull(message = "merchant id is required")
          @Positive(message = "merchant id must be positive")
          Long merchantId,
      @RequestParam("authStatus")
          @Parameter(description = "Auth status")
          @NotNull(message = "auth status is required")
          @Min(value = 1, message = "auth status must be 1 or 2")
          @Max(value = 2, message = "auth status must be 1 or 2")
          Integer authStatus,
      @RequestParam(value = "remark", required = false) @Parameter(description = "Review remark")
          String remark) {
    if (!isReviewStatus(authStatus)) {
      throw new BizException(
          ResultCode.BAD_REQUEST, "auth status must be 1(approved) or 2(rejected)");
    }
    if (merchantService.getById(merchantId) == null) {
      throw new BizException(ResultCode.NOT_FOUND, "merchant not found");
    }

    MerchantAuthDTO merchantAuth =
        merchantAuthService.getMerchantAuthByMerchantIdWithCache(merchantId);
    if (merchantAuth == null) {
      throw new BizException(ResultCode.NOT_FOUND, "merchant auth record not found");
    }

    boolean updated = merchantAuthService.updateAuthStatus(merchantId, authStatus, remark);
    if (!updated) {
      throw new BizException(ResultCode.BUSINESS_ERROR, "failed to update merchant auth status");
    }

    merchantService.updateMerchantAuditStatus(merchantId, authStatus);

    String action = authStatus == STATUS_APPROVED ? "approved" : "rejected";
    return Result.success("merchant auth " + action, true);
  }

  @GetMapping("/api/merchant-authentications")
  @PreAuthorize("hasAuthority('admin:all') or hasAuthority('merchant:audit')")
  @Operation(
      summary = "List merchant auth by status",
      description = "List merchant auth records by auth status")
  public Result<PageResult<MerchantAuthDTO>> listAuthByStatus(
      @RequestParam("authStatus")
          @Parameter(description = "Auth status")
          @NotNull(message = "auth status is required")
          @Min(value = 0, message = "auth status must be between 0 and 2")
          @Max(value = 2, message = "auth status must be between 0 and 2")
          Integer authStatus,
      @RequestParam(defaultValue = "1")
          @Parameter(description = "Page number")
          @Min(value = 1, message = "page must be greater than 0")
          Integer page,
      @RequestParam(defaultValue = "20")
          @Parameter(description = "Page size")
          @Min(value = 1, message = "size must be greater than 0")
          @Max(value = 200, message = "size must be less than or equal to 200")
          Integer size) {
    if (!isValidAuthStatus(authStatus)) {
      throw new BizException(ResultCode.BAD_REQUEST, "invalid auth status");
    }

    int maxPageSize = (authListMaxSize == null || authListMaxSize <= 0) ? 200 : authListMaxSize;
    int safeSize = Math.min(size, maxPageSize);
    Page<MerchantAuthDTO> pageResult =
        merchantAuthService.getMerchantAuthPage(authStatus, page, safeSize);
    List<MerchantAuthDTO> records =
        pageResult.getRecords().stream().map(this::enrichBusinessLicenseUrl).toList();
    PageResult<MerchantAuthDTO> result =
        PageResult.of(
            pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal(), records);
    return Result.success(result);
  }

  private MerchantAuthDTO enrichBusinessLicenseUrl(MerchantAuthDTO merchantAuthDTO) {
    if (merchantAuthDTO == null) {
      return null;
    }
    merchantAuthDTO.setBusinessLicenseUrl(enrichCertUrl(merchantAuthDTO.getBusinessLicenseUrl()));
    merchantAuthDTO.setIdCardFrontUrl(enrichCertUrl(merchantAuthDTO.getIdCardFrontUrl()));
    merchantAuthDTO.setIdCardBackUrl(enrichCertUrl(merchantAuthDTO.getIdCardBackUrl()));
    return merchantAuthDTO;
  }

  private String enrichCertUrl(String value) {
    if (value == null || value.isBlank() || isHttpUrl(value)) {
      return value;
    }
    return minioService.getCertPresignedUrl(value);
  }

  private String normalizeCertUrl(String certUrl, String existingObjectName) {
    if (certUrl == null || certUrl.isBlank()) {
      return existingObjectName;
    }
    if (!isHttpUrl(certUrl)) {
      return certUrl;
    }
    if (existingObjectName != null
        && !existingObjectName.isBlank()
        && !isHttpUrl(existingObjectName)) {
      return existingObjectName;
    }
    String objectName = extractObjectNameFromUrl(certUrl);
    if (objectName != null && !objectName.isBlank()) {
      return objectName;
    }
    return certUrl;
  }

  private Result<MerchantAuthFileUploadDTO> uploadMerchantAuthFile(
      Long merchantId,
      MultipartFile file,
      Authentication authentication,
      java.util.function.Supplier<String> uploader,
      String successMessage,
      java.util.function.Consumer<String> objectNameConsumer) {
    merchantAuthorizationService.assertCanWriteMerchant(authentication, merchantId);
    if (merchantService.getById(merchantId) == null) {
      throw new BizException(ResultCode.NOT_FOUND, "merchant not found");
    }
    if (file.isEmpty()) {
      throw new BizException(ResultCode.BAD_REQUEST, "file cannot be empty");
    }

    String objectName = uploader.get();
    objectNameConsumer.accept(objectName);
    String previewUrl = minioService.getCertPresignedUrl(objectName);
    MerchantAuthFileUploadDTO response = new MerchantAuthFileUploadDTO();
    response.setFileKey(objectName);
    response.setPreviewUrl(previewUrl);
    return Result.success(successMessage, response);
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

  @PostMapping("/api/merchant-authentications/bulk/reviews")
  @PreAuthorize("hasAuthority('admin:all') or hasAuthority('merchant:audit')")
  @Operation(
      summary = "Batch review merchant auth",
      description = "Batch review merchant auth records")
  public Result<Boolean> reviewAuthBatch(
      @RequestBody
          @Parameter(description = "Merchant IDs")
          @NotNull(message = "merchant ids are required")
          @NotEmpty(message = "merchant ids cannot be empty")
          List<
                  @NotNull(message = "merchant id cannot be null")
                  @Positive(message = "merchant id must be positive") Long>
              merchantIds,
      @RequestParam("authStatus")
          @Parameter(description = "Auth status")
          @NotNull(message = "auth status is required")
          @Min(value = 1, message = "auth status must be 1 or 2")
          @Max(value = 2, message = "auth status must be 1 or 2")
          Integer authStatus,
      @RequestParam(value = "remark", required = false) @Parameter(description = "Review remark")
          String remark) {
    if (merchantIds.isEmpty()) {
      throw new BizException(ResultCode.BAD_REQUEST, "merchant ids cannot be empty");
    }
    if (merchantIds.size() > 100) {
      throw new BizException(ResultCode.BAD_REQUEST, "batch size cannot exceed 100");
    }
    if (!isReviewStatus(authStatus)) {
      throw new BizException(
          ResultCode.BAD_REQUEST, "auth status must be 1(approved) or 2(rejected)");
    }

    int successCount = merchantAuthService.reviewAuthBatch(merchantIds, authStatus, remark);
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
