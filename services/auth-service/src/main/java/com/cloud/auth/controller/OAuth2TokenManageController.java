package com.cloud.auth.controller;

import com.cloud.auth.service.AuthGovernanceService;
import com.cloud.common.domain.support.AuthGovernancePayloadMapper;
import com.cloud.common.domain.vo.auth.TokenBlacklistStatsVO;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "OAuth2 Token Management", description = "OAuth2 token management and monitoring APIs")
@Validated
@RequiredArgsConstructor
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "Invalid token management parameters"),
  @ApiResponse(responseCode = "401", description = "Authentication required"),
  @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
  @ApiResponse(responseCode = "404", description = "Authorization or token resource not found"),
  @ApiResponse(responseCode = "500", description = "Internal token management error")
})
public class OAuth2TokenManageController {

  private final AuthGovernanceService authGovernanceService;

  @Operation(
      summary = "Get token storage statistics",
      description = "Get token storage metrics from Redis")
  @GetMapping("/authorizations/statistics")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getTokenStats() {
    return Result.success(
        AuthGovernancePayloadMapper.toTokenStatsPayload(authGovernanceService.getTokenStats()));
  }

  @Operation(
      summary = "Get authorization details",
      description = "Get authorization details by authorization ID")
  @GetMapping("/authorizations/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getAuthorizationDetails(
      @Parameter(description = "Authorization ID")
          @PathVariable
          @NotBlank(message = "authorization id cannot be blank")
          String id) {
    return Result.success(
        AuthGovernancePayloadMapper.toAuthorizationDetailPayload(
            authGovernanceService.getAuthorizationDetails(id)));
  }

  @Operation(summary = "Revoke authorization", description = "Revoke OAuth2 authorization by ID")
  @DeleteMapping("/authorizations/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Void> revokeAuthorization(
      @Parameter(description = "Authorization ID")
          @PathVariable
          @NotBlank(message = "authorization id cannot be blank")
          String id) {
    boolean revoked = authGovernanceService.revokeAuthorization(id);
    if (!revoked) {
      throw new ResourceNotFoundException("Authorization", id);
    }
    return Result.success();
  }

  @Operation(
      summary = "Cleanup expired tokens",
      description = "Trigger cleanup for expired token data")
  @PostMapping("/cleanups/authorizations")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> cleanupExpiredTokens() {
    return Result.success(authGovernanceService.cleanupAuthorizations());
  }

  @Operation(
      summary = "Get Redis hash storage structure",
      description = "Show Redis hash structure for OAuth2 data")
  @GetMapping("/authorizations/storage-structure")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getStorageStructure() {
    return Result.success(authGovernanceService.getAuthorizationStorageStructure());
  }

  @Operation(
      summary = "Get blacklist statistics",
      description = "Get current token blacklist statistics")
  @GetMapping("/blacklist-entries/statistics")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<TokenBlacklistStatsVO> getBlacklistStats() {
    return Result.success(authGovernanceService.getBlacklistStats());
  }

  @Operation(summary = "Add token to blacklist", description = "Manually add a token to blacklist")
  @PostMapping("/blacklist-entries")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Void> addToBlacklist(
      @Parameter(description = "Token value")
          @RequestParam
          @NotBlank(message = "tokenValue cannot be blank")
          String tokenValue,
      @Parameter(description = "Revocation reason")
          @RequestParam(defaultValue = "admin_manual")
          @NotBlank(message = "reason cannot be blank")
          @Size(max = 64, message = "reason must be less than or equal to 64 characters")
          String reason) {
    authGovernanceService.addToBlacklist(tokenValue, reason);
    return Result.success();
  }

  @Operation(
      summary = "Check blacklist status",
      description = "Check whether a token is blacklisted")
  @GetMapping("/blacklist-entries/check")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> checkBlacklist(
      @Parameter(description = "Token value")
          @RequestParam
          @NotBlank(message = "tokenValue cannot be blank")
          String tokenValue) {
    return Result.success(
        AuthGovernancePayloadMapper.toBlacklistCheckPayload(
            authGovernanceService.checkBlacklist(tokenValue)));
  }

  @Operation(
      summary = "Cleanup blacklist entries",
      description = "Remove expired blacklist entries")
  @PostMapping("/cleanups/blacklist-entries")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> cleanupBlacklist() {
    int cleanedCount = authGovernanceService.cleanupBlacklist();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("cleanedCount", cleanedCount);
    result.put("message", "Blacklist cleanup completed");
    result.put("cleanupTime", Instant.now());
    return Result.success(result);
  }
}
