package com.cloud.auth.controller;

import com.cloud.auth.service.GitHubUserInfoService;
import com.cloud.auth.util.OAuth2ResponseUtil;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/auth/oauth2/github")
@RequiredArgsConstructor
public class GitHubOAuth2Controller {

    private final GitHubUserInfoService gitHubUserInfoService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final JwtEncoder jwtEncoder;
    private final OAuth2ResponseUtil oauth2ResponseUtil;

    @GetMapping("/user-info")
    public Result<LoginResponseDTO> getUserInfo(Principal principal) {
        LoginResponseDTO loginResponse = gitHubUserInfoService.getUserInfoAndGenerateToken(
                principal, authorizedClientService, jwtEncoder, oauth2ResponseUtil);
        return Result.success(loginResponse);
    }

    @GetMapping("/status")
    public Result<Boolean> checkAuthStatus(Principal principal) {
        boolean isAuthenticated = gitHubUserInfoService.checkAuthStatus(principal, authorizedClientService);
        return Result.success(isAuthenticated);
    }

    @GetMapping("/callback")
    public Result<String> handleCallback() {
        return Result.success("GitHub callback is handled by /login/oauth2/code/github");
    }

    @GetMapping("/login-url")
    public Result<String> getGitHubLoginUrl() {
        return Result.success("/oauth2/authorization/github");
    }
}
