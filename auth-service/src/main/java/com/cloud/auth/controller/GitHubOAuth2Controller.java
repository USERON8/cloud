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
@RequestMapping("/oauth2/github")
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

        try {
            if (principal == null) {
                log.warn("未找到认证信息");
                return Result.error(ResultCode.UNAUTHORIZED.getCode(), "未认证，请先登录");
            }

            // 获取OAuth2授权客户端
            OAuth2AuthorizedClient authorizedClient = authorizedClientService
                    .loadAuthorizedClient("github", principal.getName());

            if (authorizedClient == null) {
                log.warn("未找到GitHub OAuth2授权客户端，用户: {}", principal.getName());
                return Result.error(ResultCode.UNAUTHORIZED.getCode(), "GitHub授权信息不存在");
            }

            // 获取或创建用户
            UserDTO userDTO = gitHubUserInfoService.getOrCreateUser(authorizedClient);

            if (userDTO != null) {
                // 生成JWT响应
                LoginResponseDTO loginResponse = oauth2ResponseUtil.buildSimpleLoginResponse(userDTO, jwtEncoder);
                log.info("成功获取GitHub用户信息: {}", userDTO.getUsername());
                return Result.success(loginResponse);
            } else {
                log.error("获取GitHub用户信息失败");
                return Result.error(ResultCode.USER_NOT_FOUND.getCode(), "获取用户信息失败");
            }

        } catch (Exception e) {
            log.error("获取GitHub OAuth2用户信息时发生异常", e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "获取用户信息异常");
        }
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

        try {
            if (principal == null) {
                return Result.success(false);
            }

            OAuth2AuthorizedClient authorizedClient = authorizedClientService
                    .loadAuthorizedClient("github", principal.getName());

            boolean isAuthenticated = authorizedClient != null;
            log.info("GitHub OAuth2认证状态: {}", isAuthenticated);

            return Result.success(isAuthenticated);

        } catch (Exception e) {
            log.error("检查GitHub OAuth2认证状态时发生异常", e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "检查认证状态失败");
        }
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

        try {
            // 注意：实际的OAuth2授权码处理由Spring Security OAuth2 Client自动完成
            // 这个接口主要用于前端重定向或API调用场景

            return Result.success("GitHub OAuth2回调接收成功，请使用 /oauth2/github/user-info 获取用户信息");

        } catch (Exception e) {
            log.error("处理GitHub OAuth2回调时发生异常", e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "处理OAuth2回调失败");
        }
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

        try {
            // 构建GitHub OAuth2登录URL
            String loginUrl = "/oauth2/authorization/github";
            log.info("GitHub OAuth2登录URL: {}", loginUrl);

            return Result.success(loginUrl);

        } catch (Exception e) {
            log.error("获取GitHub OAuth2登录URL时发生异常", e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "获取登录URL失败");
        }
    }
}
