package com.cloud.auth.controller;

import com.cloud.auth.service.AuthUserAuthorityCacheService;
import com.cloud.auth.service.OAuth2TokenManagementService;
import com.cloud.auth.service.support.AuthIdentityService;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.auth.RegisterResponseDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.ValidationException;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/auth")
@Tag(name = "Authentication API", description = "Authentication, login and token management APIs")
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "Invalid authentication request"),
  @ApiResponse(responseCode = "401", description = "Authentication required or token invalid"),
  @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
  @ApiResponse(responseCode = "404", description = "Authentication target resource not found"),
  @ApiResponse(responseCode = "409", description = "Authentication state conflict"),
  @ApiResponse(responseCode = "500", description = "Internal authentication service error")
})
public class AuthController {

  private final OAuth2TokenManagementService tokenManagementService;
  private final AuthIdentityService authIdentityService;
  private final JwtDecoder jwtDecoder;
  private final AuthUserAuthorityCacheService authorityCacheService;

  @PostMapping("/users/register")
  @Operation(summary = "Register user")
  public Result<RegisterResponseDTO> register(
      @RequestBody @Valid @NotNull(message = "Register request cannot be null")
          RegisterRequestDTO registerRequestDTO) {
    UserDTO registeredUser = authIdentityService.register(registerRequestDTO);
    if (registeredUser == null) {
      throw new BizException(ResultCode.USER_ALREADY_EXISTS);
    }

    RegisterResponseDTO registerResponse =
        new RegisterResponseDTO(
            registeredUser.getId(),
            registeredUser.getUsername(),
            registeredUser.getPhone(),
            registeredUser.getNickname(),
            registeredUser.getRoles());
    return Result.success(
        "Registration successful. Continue with /oauth2/authorize to sign in.", registerResponse);
  }

  @DeleteMapping("/sessions")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Logout current session")
  public Result<Void> logout(jakarta.servlet.http.HttpServletRequest request) {
    String authorizationHeader = request.getHeader("Authorization");
    boolean logoutSuccess = false;
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      String accessToken = authorizationHeader.substring(7);
      logoutSuccess = tokenManagementService.logout(accessToken, null);
      authorityCacheService.evictByAccessToken(accessToken, jwtDecoder);
    }

    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
      SecurityContextHolder.clearContext();
      logoutSuccess = true;
    }

    if (!logoutSuccess) {
      throw new BizException(ResultCode.UNAUTHORIZED);
    }

    return Result.success("Logout successful", null);
  }

  @DeleteMapping("/users/{username}/sessions")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Logout all user sessions")
  public Result<String> logoutAllSessions(
      @PathVariable
          @Parameter(description = "Username", required = true)
          @NotBlank(message = "Username cannot be blank")
          String username) {
    int revokedCount = tokenManagementService.logoutAllSessions(username);
    AuthPrincipalDTO principal = authIdentityService.findByUsername(username);
    if (principal != null && principal.getId() != null) {
      authorityCacheService.evict(principal.getId());
    }
    String message =
        String.format("Revoked %d active sessions for user %s", revokedCount, username);
    return Result.success(message);
  }

  @GetMapping("/tokens/validate")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Validate access token")
  public Result<String> validateToken(jakarta.servlet.http.HttpServletRequest request) {
    String authorizationHeader = request.getHeader("Authorization");
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new ValidationException(
          "Authorization header",
          authorizationHeader,
          "Missing valid Bearer token in Authorization header");
    }

    String accessToken = authorizationHeader.substring(7);
    if (!tokenManagementService.isTokenValid(accessToken)) {
      throw new BizException(ResultCode.UNAUTHORIZED);
    }

    OAuth2Authorization authorization = tokenManagementService.findByToken(accessToken);
    if (authorization == null) {
      throw new BizException(ResultCode.UNAUTHORIZED);
    }

    String message =
        String.format(
            "Token is valid, principal=%s, scopes=%s",
            authorization.getPrincipalName(),
            String.join(", ", authorization.getAuthorizedScopes()));
    return Result.success(message);
  }
}
