package com.cloud.auth.controller;

import com.cloud.auth.service.GitHubUserInfoService;
import com.cloud.auth.util.OAuth2ResponseUtil;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * GitHub OAuth2登录控制器
 * 提供GitHub OAuth2登录相关的API接口
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/auth/oauth2/github")
@RequiredArgsConstructor
public class GitHubOAuth2Controller {

    private final GitHubUserInfoService gitHubUserInfoService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final JwtEncoder jwtEncoder;
    private final OAuth2ResponseUtil oauth2ResponseUtil;

    /**
     * 获取GitHub OAuth2登录用户信息
     * 此接口用于前端在OAuth2回调后获取用户信息和JWT令牌
     *
     * @param principal 认证主体
     * @return 用户登录信息
     */
    @GetMapping("/user-info")
    public Result<LoginResponseDTO> getUserInfo(Principal principal) {
        log.info("获取GitHub OAuth2用户信息，principal: {}", principal != null ? principal.getName() : "null");
        
        LoginResponseDTO loginResponse = gitHubUserInfoService.getUserInfoAndGenerateToken(
                principal, authorizedClientService, jwtEncoder, oauth2ResponseUtil);
        
        return Result.success(loginResponse);
    }

    /**
     * GitHub OAuth2登录状态检查
     * 检查当前用户是否已通过GitHub OAuth2认证
     *
     * @param principal 认证主体
     * @return 认证状态
     */
    @GetMapping("/status")
    public Result<Boolean> checkAuthStatus(Principal principal) {
        log.info("检查GitHub OAuth2登录状态，principal: {}", principal != null ? principal.getName() : "null");
        
        boolean isAuthenticated = gitHubUserInfoService.checkAuthStatus(
                principal, authorizedClientService);
        
        return Result.success(isAuthenticated);
    }

    /**
     * 处理GitHub OAuth2登录回调
     * 此接口主要用于直接API调用方式的OAuth2流程
     *
     * @param code  授权码
     * @param state 状态参数
     * @return 登录结果
     */
    @GetMapping("/callback")
    public Result<String> handleCallback(@RequestParam("code") String code,
                                         @RequestParam(value = "state", required = false) String state) {
        log.info("接收到GitHub OAuth2回调，code: {}, state: {}", code != null ? "****" : null, state);
        
        // 注意：实际的OAuth2授权码处理由Spring Security OAuth2 Client自动完成
        // 这个接口主要用于前端重定向或API调用场景
        String message = "GitHub OAuth2回调接收成功，请使用 /oauth2/github/user-info 获取用户信息";
        
        return Result.success(message);
    }

    /**
     * 获取GitHub OAuth2登录URL
     * 为前端提供GitHub OAuth2登录的跳转链接
     *
     * @return GitHub OAuth2登录URL
     */
    @GetMapping("/login-url")
    public Result<String> getGitHubLoginUrl() {
        log.info("获取GitHub OAuth2登录URL");
        
        // 构建GitHub OAuth2登录URL
        String loginUrl = "/oauth2/authorization/github";
        log.info("GitHub OAuth2登录URL: {}", loginUrl);
        
        return Result.success(loginUrl);
    }
}
