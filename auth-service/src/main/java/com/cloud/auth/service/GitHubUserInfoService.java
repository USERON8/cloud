package com.cloud.auth.service;

import com.cloud.api.user.UserDubboApi;
import com.cloud.auth.util.OAuth2ResponseUtil;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.oauth.GitHubUserDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.OAuth2Exception;
import com.cloud.common.exception.SystemException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubUserInfoService {

    private static final String OAUTH_PROVIDER_GITHUB = "github";
    private static final String GITHUB_USER_API = "https://api.github.com/user";
    private static final String GITHUB_USER_EMAILS_API = "https://api.github.com/user/emails";

    @DubboReference(check = false, timeout = 5000, retries = 0)
    private UserDubboApi userDubboApi;
    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserDTO getOrCreateUser(OAuth2AuthorizedClient authorizedClient) {
        try {
            GitHubUserDTO githubUser = fetchGitHubUserInfo(authorizedClient);
            String oauthProviderId = String.valueOf(githubUser.getGithubId());

            UserDTO existingUser = userDubboApi.findByGitHubId(githubUser.getGithubId());
            if (existingUser == null) {
                existingUser = userDubboApi.findByOAuthProvider(OAUTH_PROVIDER_GITHUB, oauthProviderId);
            }

            if (existingUser != null) {
                userDubboApi.updateGitHubUserInfo(existingUser.getId(), githubUser);
                UserDTO refreshedUser = userDubboApi.findById(existingUser.getId());
                return refreshedUser != null ? refreshedUser : existingUser;
            }

            UserDTO newUser = userDubboApi.createGitHubUser(githubUser);
            if (newUser == null) {
                throw new SystemException("Failed to create GitHub user");
            }
            return newUser;
        } catch (Exception e) {
            log.error("Failed to process GitHub user info", e);
            throw new SystemException(500, "Failed to process GitHub user info", e);
        }
    }

    private GitHubUserDTO fetchGitHubUserInfo(OAuth2AuthorizedClient authorizedClient) {
        try {
            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            String responseBody = webClient.get()
                    .uri(GITHUB_USER_API)
                    .headers(headers -> applyGitHubHeaders(headers, accessToken))
                    .accept(MediaType.valueOf("application/vnd.github.v3+json"))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> Mono.error(
                                    new SystemException("Failed to fetch GitHub user info: " + response.statusCode() + ", body=" + body))))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(8))
                    .block();
            if (responseBody == null || responseBody.isBlank()) {
                throw new SystemException("Failed to fetch GitHub user info: empty response");
            }
            JsonNode userNode = objectMapper.readTree(responseBody);
            JsonNode idNode = userNode.get("id");
            JsonNode loginNode = userNode.get("login");

            if (idNode == null || loginNode == null) {
                throw new SystemException("GitHub user response missing required fields");
            }

            String email = userNode.has("email") && !userNode.get("email").isNull()
                    ? userNode.get("email").asText()
                    : null;
            if (email == null) {
                email = fetchPrimaryEmail(accessToken);
            }

            return GitHubUserDTO.builder()
                    .githubId(idNode.asLong())
                    .login(loginNode.asText())
                    .name(userNode.has("name") && !userNode.get("name").isNull() ? userNode.get("name").asText() : null)
                    .email(email)
                    .avatarUrl(userNode.has("avatar_url") ? userNode.get("avatar_url").asText() : null)
                    .htmlUrl(userNode.has("html_url") ? userNode.get("html_url").asText() : null)
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch GitHub user info", e);
            throw new SystemException(500, "Failed to call GitHub API", e);
        }
    }

    private String fetchPrimaryEmail(String accessToken) {
        try {
            String responseBody = webClient.get()
                    .uri(GITHUB_USER_EMAILS_API)
                    .headers(headers -> applyGitHubHeaders(headers, accessToken))
                    .accept(MediaType.valueOf("application/vnd.github.v3+json"))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> Mono.error(
                                    new SystemException("Failed to fetch GitHub emails: " + response.statusCode() + ", body=" + body))))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(8))
                    .block();
            if (responseBody == null || responseBody.isBlank()) {
                return null;
            }
            JsonNode emailsNode = objectMapper.readTree(responseBody);
            for (JsonNode emailNode : emailsNode) {
                if (emailNode.has("primary") && emailNode.get("primary").asBoolean()) {
                    return emailNode.get("email").asText();
                }
            }
            if (!emailsNode.isEmpty()) {
                return emailsNode.get(0).get("email").asText();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch GitHub email: {}", e.getMessage());
        }
        return null;
    }

    private void applyGitHubHeaders(HttpHeaders headers, String accessToken) {
        headers.setBearerAuth(accessToken);
        headers.setAccept(MediaType.parseMediaTypes("application/vnd.github.v3+json"));
    }

    public LoginResponseDTO getUserInfoAndGenerateToken(
            Principal principal,
            OAuth2AuthorizedClientService authorizedClientService,
            OAuth2TokenManagementService tokenManagementService,
            OAuth2ResponseUtil oauth2ResponseUtil) {
        if (principal == null) {
            throw new OAuth2Exception(ResultCode.UNAUTHORIZED, "Not authenticated");
        }

        OAuth2AuthorizedClient authorizedClient =
                authorizedClientService.loadAuthorizedClient(OAUTH_PROVIDER_GITHUB, principal.getName());
        if (authorizedClient == null) {
            throw new OAuth2Exception(ResultCode.UNAUTHORIZED, "GitHub authorization not found");
        }

        UserDTO userDTO = getOrCreateUser(authorizedClient);
        if (userDTO == null) {
            throw new SystemException("Failed to get user info");
        }

        OAuth2Authorization authorization = tokenManagementService.generateTokensForUser(userDTO, null);
        return oauth2ResponseUtil.buildLoginResponse(authorization, userDTO);
    }

    public boolean checkAuthStatus(
            Principal principal,
            OAuth2AuthorizedClientService authorizedClientService) {
        if (principal == null) {
            return false;
        }
        OAuth2AuthorizedClient authorizedClient =
                authorizedClientService.loadAuthorizedClient(OAUTH_PROVIDER_GITHUB, principal.getName());
        return authorizedClient != null;
    }
}

