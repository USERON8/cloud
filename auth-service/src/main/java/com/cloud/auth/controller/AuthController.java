package com.cloud.auth.controller;

import com.cloud.api.user.UserFeignClient;
import com.cloud.auth.service.OAuth2TokenManagementService;
import com.cloud.auth.util.OAuth2ResponseUtil;
import com.cloud.common.domain.dto.auth.LoginRequestDTO;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.exception.ValidationException;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/auth")
@Tag(name = "Authentication API", description = "Authentication, login and token management APIs")
public class AuthController {

    private final UserFeignClient userFeignClient;
    private final OAuth2TokenManagementService tokenManagementService;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2ResponseUtil oauth2ResponseUtil;

    @PostMapping("/users/register")
    @Operation(summary = "Register user")
    public Result<LoginResponseDTO> register(
            @RequestBody @Valid @NotNull(message = "Register request cannot be null")
            RegisterRequestDTO registerRequestDTO) {
        UserDTO registeredUser = userFeignClient.register(registerRequestDTO);
        if (registeredUser == null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        OAuth2Authorization authorization = tokenManagementService.generateTokensForUser(registeredUser, null);
        LoginResponseDTO response = oauth2ResponseUtil.buildLoginResponse(authorization, registeredUser);
        return Result.success(response);
    }

    @PostMapping("/sessions")
    @Operation(summary = "Login user")
    public Result<LoginResponseDTO> login(
            @RequestBody @Valid @NotNull(message = "Login request cannot be null")
            LoginRequestDTO loginRequestDTO) {
        validateLoginRequest(loginRequestDTO);
        String username = loginRequestDTO.getUsername();

        UserDTO user = userFeignClient.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("User", username);
        }

        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        if (loginRequestDTO.getUserType() != null
                && !loginRequestDTO.getUserType().isBlank()
                && user.getUserType() != null
                && !loginRequestDTO.getUserType().equalsIgnoreCase(user.getUserType().getCode())) {
            throw new BusinessException(ResultCode.USER_TYPE_MISMATCH);
        }

        String storedPassword = userFeignClient.getUserPassword(username);
        if (storedPassword == null) {
            throw new ResourceNotFoundException("User password", username);
        }
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), storedPassword)) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        OAuth2Authorization authorization = tokenManagementService.generateTokensForUser(user, null);
        LoginResponseDTO response = oauth2ResponseUtil.buildLoginResponse(authorization, user);
        return Result.success(response);
    }

    @DeleteMapping("/sessions")
    @Operation(summary = "Logout current session")
    public Result<Void> logout(jakarta.servlet.http.HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ValidationException(
                    "Authorization header",
                    authorizationHeader,
                    "Missing valid Bearer token in Authorization header");
        }

        String accessToken = authorizationHeader.substring(7);
        boolean logoutSuccess = tokenManagementService.logout(accessToken, null);
        if (!logoutSuccess) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        return Result.success("Logout successful", null);
    }

    @DeleteMapping("/users/{username}/sessions")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Logout all user sessions")
    public Result<String> logoutAllSessions(
            @PathVariable
            @Parameter(description = "Username", required = true)
            @NotBlank(message = "Username cannot be blank") String username) {
        int revokedCount = tokenManagementService.logoutAllSessions(username);
        String message = String.format("Revoked %d active sessions for user %s", revokedCount, username);
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
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        OAuth2Authorization authorization = tokenManagementService.findByToken(accessToken);
        if (authorization == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String message = String.format(
                "Token is valid, principal=%s, scopes=%s",
                authorization.getPrincipalName(),
                String.join(", ", authorization.getAuthorizedScopes())
        );
        return Result.success(message);
    }

    @PostMapping("/tokens/refresh")
    @Operation(summary = "Refresh access token")
    public Result<LoginResponseDTO> refreshToken(
            @RequestParam("refresh_token") String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new ValidationException("refresh_token", refreshToken, "refresh_token cannot be blank");
        }

        OAuth2Authorization existingAuth = tokenManagementService.findByToken(refreshToken);
        if (existingAuth == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String username = existingAuth.getPrincipalName();
        UserDTO user = userFeignClient.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("User", username);
        }

        tokenManagementService.revokeToken(refreshToken);
        OAuth2Authorization newAuth =
                tokenManagementService.generateTokensForUser(user, existingAuth.getAuthorizedScopes());
        LoginResponseDTO response = oauth2ResponseUtil.buildLoginResponse(newAuth, user);
        return Result.success(response);
    }

    private void validateLoginRequest(LoginRequestDTO loginRequestDTO) {
        if (loginRequestDTO.getUsername() == null || loginRequestDTO.getUsername().trim().isEmpty()) {
            throw new ValidationException("username", loginRequestDTO.getUsername(), "username cannot be blank");
        }
        if (loginRequestDTO.getPassword() == null || loginRequestDTO.getPassword().trim().isEmpty()) {
            throw new ValidationException("password", loginRequestDTO.getPassword(), "password cannot be blank");
        }
    }
}
