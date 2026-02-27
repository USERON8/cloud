package com.cloud.auth.handler;

import com.cloud.auth.service.GitHubUserInfoService;
import com.cloud.auth.service.OAuth2TokenManagementService;
import com.cloud.auth.util.OAuth2ResponseUtil;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Hidden
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final OAuth2TokenManagementService tokenManagementService;
    private final OAuth2ResponseUtil oauth2ResponseUtil;
    private final GitHubUserInfoService gitHubUserInfoService;

    @Value("${app.oauth2.github.redirect.success-url:http://127.0.0.1:3000/auth/success}")
    private String githubSuccessRedirectUrl;

    @Value("${app.oauth2.github.redirect.error-url:http://127.0.0.1:3000/auth/error}")
    private String githubErrorRedirectUrl;

    @Value("${app.security.session-cookie.refresh-token-name:shop_rt}")
    private String refreshTokenCookieName;

    @Value("${app.security.session-cookie.path:/}")
    private String sessionCookiePath;

    @Value("${app.security.session-cookie.secure:false}")
    private boolean sessionCookieSecure;

    @Value("${app.security.session-cookie.same-site:Lax}")
    private String sessionCookieSameSite;

    @Value("${app.security.session-cookie.refresh-max-age-seconds:2592000}")
    private long sessionCookieRefreshMaxAgeSeconds;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
                handleError(response, "Invalid authentication type");
                return;
            }

            OAuth2AuthorizedClient authorizedClient = authorizedClientService
                    .loadAuthorizedClient(oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
            if (authorizedClient == null) {
                handleError(response, "OAuth2 authorized client not found");
                return;
            }

            LoginResponseDTO loginResponse = gitHubUserInfoService.getUserInfoAndGenerateToken(
                    oauthToken,
                    authorizedClientService,
                    tokenManagementService,
                    oauth2ResponseUtil
            );
            handleSuccess(response, loginResponse);
        } catch (Exception e) {
            log.error("Failed to handle OAuth2 success callback", e);
            handleError(response, "OAuth2 login failed");
        }
    }

    private void handleSuccess(HttpServletResponse response, LoginResponseDTO loginResponse) throws IOException {
        writeRefreshTokenCookie(response, loginResponse.getRefresh_token());
        response.sendRedirect(githubSuccessRedirectUrl);
    }

    private void handleError(HttpServletResponse response, String errorMessage) throws IOException {
        String message = URLEncoder.encode(
                errorMessage == null ? "OAuth2 login failed" : errorMessage,
                StandardCharsets.UTF_8
        );
        response.sendRedirect(githubErrorRedirectUrl + "?message=" + message);
    }

    private void writeRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        long maxAge = Math.max(sessionCookieRefreshMaxAgeSeconds, 0);
        ResponseCookie cookie = ResponseCookie.from(refreshTokenCookieName, refreshToken)
                .httpOnly(true)
                .secure(sessionCookieSecure)
                .path(sessionCookiePath)
                .sameSite(sessionCookieSameSite)
                .maxAge(Duration.ofSeconds(maxAge))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
