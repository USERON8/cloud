package com.cloud.auth.handler;

import com.cloud.auth.service.GitHubUserInfoService;
import com.cloud.auth.util.OAuth2ResponseUtil;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2认证成功处理器
 * 处理OAuth2登录成功后的用户信息获取、JWT生成和重定向
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler, ApplicationContextAware {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final JwtEncoder jwtEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // 通过ApplicationContext动态获取GitHubUserInfoService，解决循环依赖问题
    private ApplicationContext applicationContext;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        log.info("OAuth2认证成功，开始处理用户信息");

        try {
            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                String registrationId = oauthToken.getAuthorizedClientRegistrationId();

                log.info("OAuth2登录提供者: {}", registrationId);

                // 获取授权客户端
                OAuth2AuthorizedClient authorizedClient = authorizedClientService
                        .loadAuthorizedClient(registrationId, authentication.getName());

                if (authorizedClient == null) {
                    log.error("无法获取OAuth2授权客户端: {}", registrationId);
                    handleError(response, "无法获取授权信息");
                    return;
                }

                UserDTO userDTO = null;

                // 根据不同的OAuth2提供者处理用户信息
                if (registrationId.equals("github")) {
                    GitHubUserInfoService gitHubUserInfoService = applicationContext.getBean(GitHubUserInfoService.class);
                    userDTO = gitHubUserInfoService.getOrCreateUser(authorizedClient);
                } else {
                    log.error("不支持的OAuth2提供者: {}", registrationId);
                    handleError(response, "不支持的登录提供者");
                    return;
                }

                if (userDTO != null) {
                    // 生成JWT响应
                    LoginResponseDTO loginResponse = OAuth2ResponseUtil.buildSimpleLoginResponse(userDTO, jwtEncoder);

                    log.info("OAuth2登录成功，用户: {}, 生成JWT令牌", userDTO.getUsername());

                    // 重定向到前端，携带登录信息
                    handleSuccess(response, loginResponse);
                } else {
                    log.error("获取用户信息失败");
                    handleError(response, "获取用户信息失败");
                }

            } else {
                log.error("无效的认证类型: {}", authentication.getClass().getSimpleName());
                handleError(response, "无效的认证类型");
            }

        } catch (Exception e) {
            log.error("处理OAuth2登录成功回调时发生异常", e);
            handleError(response, "登录处理失败");
        }
    }

    /**
     * 处理登录成功
     *
     * @param response      响应对象
     * @param loginResponse 登录响应数据
     */
    private void handleSuccess(HttpServletResponse response, LoginResponseDTO loginResponse) throws IOException {
        try {
            // 将登录响应转换为JSON字符串
            String responseJson = objectMapper.writeValueAsString(loginResponse);
            String encodedResponse = URLEncoder.encode(responseJson, StandardCharsets.UTF_8);

            // 重定向到前端成功页面，携带登录信息
            String redirectUrl = String.format("http://localhost:3000/auth/success?data=%s", encodedResponse);

            log.info("OAuth2登录成功，重定向到: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("生成登录成功重定向URL时发生异常", e);
            handleError(response, "登录成功但重定向失败");
        }
    }

    /**
     * 处理登录失败
     *
     * @param response     响应对象
     * @param errorMessage 错误信息
     */
    private void handleError(HttpServletResponse response, String errorMessage) throws IOException {
        try {
            String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            String redirectUrl = String.format("http://localhost:3000/auth/error?message=%s", encodedError);

            log.warn("OAuth2登录失败，重定向到: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("生成登录失败重定向URL时发生异常", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("登录失败");
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
