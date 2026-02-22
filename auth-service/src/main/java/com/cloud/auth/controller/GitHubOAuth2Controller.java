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
import org.springframework.web.bind.annotation.RequestParam;
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
        

        boolean isAuthenticated = gitHubUserInfoService.checkAuthStatus(
                principal, authorizedClientService);

        return Result.success(isAuthenticated);
    }

    







    @GetMapping("/callback")
    public Result<String> handleCallback(@RequestParam("code") String code,
                                         @RequestParam(value = "state", required = false) String state) {
        

        
        
        String message = "GitHub OAuth2鍥炶皟鎺ユ敹鎴愬姛锛岃浣跨敤 /oauth2/github/user-info 鑾峰彇鐢ㄦ埛淇℃伅";

        return Result.success(message);
    }

    





    @GetMapping("/login-url")
    public Result<String> getGitHubLoginUrl() {
        

        
        String loginUrl = "/oauth2/authorization/github";
        

        return Result.success(loginUrl);
    }
}
