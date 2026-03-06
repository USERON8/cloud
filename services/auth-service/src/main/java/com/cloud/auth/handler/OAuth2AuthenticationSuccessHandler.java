package com.cloud.auth.handler;

import com.cloud.auth.service.GitHubUserInfoService;
import com.cloud.auth.service.AuthorizationCodeFlowService;
import com.cloud.auth.service.AuthorizationRequestSessionService;
import com.cloud.auth.service.LocalUserAuthorityService;
import com.cloud.common.domain.dto.auth.AuthorizationRequestDTO;
import com.cloud.common.domain.dto.auth.AuthorizationSessionResponseDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
    private final GitHubUserInfoService gitHubUserInfoService;
    private final AuthorizationRequestSessionService authorizationRequestSessionService;
    private final AuthorizationCodeFlowService authorizationCodeFlowService;
    private final LocalUserAuthorityService localUserAuthorityService;

    @Value("${app.oauth2.github.redirect.error-url:http://127.0.0.1:3000/auth/error}")
    private String githubErrorRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            AuthorizationRequestDTO authorizationRequest = authorizationRequestSessionService.consume(request);
            if (authorizationRequest == null) {
                handleError(request, response, "Missing pending local OAuth2 authorization request");
                return;
            }

            if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
                handleError(request, response, "Invalid authentication type");
                return;
            }

            OAuth2AuthorizedClient authorizedClient = authorizedClientService
                    .loadAuthorizedClient(oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
            if (authorizedClient == null) {
                handleError(request, response, "OAuth2 authorized client not found");
                return;
            }

            UserDTO user = gitHubUserInfoService.getOrCreateUser(authorizedClient);
            AuthorizationSessionResponseDTO sessionResponse = authorizationCodeFlowService.createAuthorizationSession(
                    authorizationRequest,
                    localUserAuthorityService.createAuthenticatedPrincipal(user),
                    request,
                    response
            );
            response.sendRedirect(sessionResponse.getAuthorizationUri());
        } catch (Exception e) {
            log.error("Failed to handle OAuth2 success callback", e);
            handleError(request, response, "OAuth2 login failed");
        }
    }

    private void handleError(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException {
        authorizationRequestSessionService.clear(request);
        String message = URLEncoder.encode(
                errorMessage == null ? "OAuth2 login failed" : errorMessage,
                StandardCharsets.UTF_8
        );
        response.sendRedirect(githubErrorRedirectUrl + "?message=" + message);
    }
}
