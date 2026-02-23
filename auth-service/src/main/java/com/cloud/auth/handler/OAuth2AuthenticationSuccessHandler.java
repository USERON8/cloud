package com.cloud.auth.handler;

import com.cloud.auth.service.GitHubUserInfoService;
import com.cloud.auth.util.OAuth2ResponseUtil;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Hidden
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final JwtEncoder jwtEncoder;
    private final OAuth2ResponseUtil oauth2ResponseUtil;
    private final GitHubUserInfoService gitHubUserInfoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                    jwtEncoder,
                    oauth2ResponseUtil
            );
            handleSuccess(response, loginResponse);
        } catch (Exception e) {
            log.error("Failed to handle OAuth2 success callback", e);
            handleError(response, "OAuth2 login failed");
        }
    }

    private void handleSuccess(HttpServletResponse response, LoginResponseDTO loginResponse) throws IOException {
        try {
            String responseJson = objectMapper.writeValueAsString(loginResponse);
            String encodedResponse = URLEncoder.encode(responseJson, StandardCharsets.UTF_8);
            String redirectUrl = String.format("http://127.0.0.1:3000/auth/success?data=%s", encodedResponse);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("Failed to build success redirect URL", e);
            handleError(response, "Login succeeded but redirect failed");
        }
    }

    private void handleError(HttpServletResponse response, String errorMessage) throws IOException {
        try {
            String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            String redirectUrl = String.format("http://127.0.0.1:3000/auth/error?message=%s", encodedError);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("Failed to build error redirect URL", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("OAuth2 login failed");
        }
    }
}
