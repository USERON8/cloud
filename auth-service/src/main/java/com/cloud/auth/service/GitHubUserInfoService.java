package com.cloud.auth.service;

import com.cloud.api.user.UserFeignClient;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubUserInfoService {

    private static final String OAUTH_PROVIDER_GITHUB = "github";
    private static final String GITHUB_USER_API = "https://api.github.com/user";
    private static final String GITHUB_USER_EMAILS_API = "https://api.github.com/user/emails";

    private final UserFeignClient userFeignClient;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserDTO getOrCreateUser(OAuth2AuthorizedClient authorizedClient) {
        try {
            GitHubUserDTO githubUser = fetchGitHubUserInfo(authorizedClient);
            String oauthProviderId = String.valueOf(githubUser.getGithubId());

            UserDTO existingUser = userFeignClient.findByGitHubId(githubUser.getGithubId());
            if (existingUser == null) {
                existingUser = userFeignClient.findByOAuthProvider(OAUTH_PROVIDER_GITHUB, oauthProviderId);
            }

            if (existingUser != null) {
                userFeignClient.updateGitHubUserInfo(existingUser.getId(), githubUser);
                UserDTO refreshedUser = userFeignClient.findById(existingUser.getId());
                return refreshedUser != null ? refreshedUser : existingUser;
            }

            UserDTO newUser = userFeignClient.createGitHubUser(githubUser);
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

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setAccept(MediaType.parseMediaTypes("application/vnd.github.v3+json"));
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> userResponse = restTemplate.exchange(
                    GITHUB_USER_API, HttpMethod.GET, entity, String.class);

            if (userResponse.getStatusCode() != HttpStatus.OK || userResponse.getBody() == null) {
                throw new SystemException("Failed to fetch GitHub user info: " + userResponse.getStatusCode());
            }

            JsonNode userNode = objectMapper.readTree(userResponse.getBody());
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
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setAccept(MediaType.parseMediaTypes("application/vnd.github.v3+json"));
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> emailResponse = restTemplate.exchange(
                    GITHUB_USER_EMAILS_API, HttpMethod.GET, entity, String.class);

            if (emailResponse.getStatusCode() == HttpStatus.OK && emailResponse.getBody() != null) {
                JsonNode emailsNode = objectMapper.readTree(emailResponse.getBody());
                for (JsonNode emailNode : emailsNode) {
                    if (emailNode.has("primary") && emailNode.get("primary").asBoolean()) {
                        return emailNode.get("email").asText();
                    }
                }
                if (!emailsNode.isEmpty()) {
                    return emailsNode.get(0).get("email").asText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch GitHub email: {}", e.getMessage());
        }
        return null;
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
