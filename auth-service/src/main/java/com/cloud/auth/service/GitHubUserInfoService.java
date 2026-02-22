package com.cloud.auth.service;

import com.cloud.api.user.UserFeignClient;
import com.cloud.auth.util.OAuth2ResponseUtil;
import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.enums.UserType;
import com.cloud.common.exception.OAuth2Exception;
import com.cloud.common.exception.SystemException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
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
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubUserInfoService {

    private static final String GITHUB_USER_API = "https://api.github.com/user";
    private static final String GITHUB_USER_EMAILS_API = "https://api.github.com/user/emails";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserFeignClient userFeignClient;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserDTO getOrCreateUser(OAuth2AuthorizedClient authorizedClient) {
        try {
            GitHubUserInfo githubUserInfo = fetchGitHubUserInfo(authorizedClient);
            String username = buildUsername(githubUserInfo);

            UserDTO existingUser = userFeignClient.findByUsername(username);
            if (existingUser != null) {
                return existingUser;
            }

            RegisterRequestDTO registerRequest = new RegisterRequestDTO();
            registerRequest.setUsername(username);
            registerRequest.setNickname(githubUserInfo.getName() != null ? githubUserInfo.getName() : githubUserInfo.getLogin());
            registerRequest.setUserType(UserType.USER.getCode());
            registerRequest.setPassword(generateSecurePassword());
            registerRequest.setPhone("13800000000");

            UserDTO newUser = userFeignClient.register(registerRequest);
            if (newUser == null) {
                throw new RuntimeException("Failed to register GitHub user");
            }
            return newUser;
        } catch (Exception e) {
            log.error("Failed to process GitHub user info", e);
            throw new RuntimeException("Failed to process GitHub user info", e);
        }
    }

    private GitHubUserInfo fetchGitHubUserInfo(OAuth2AuthorizedClient authorizedClient) {
        try {
            String accessToken = authorizedClient.getAccessToken().getTokenValue();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setAccept(MediaType.parseMediaTypes("application/vnd.github.v3+json"));
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> userResponse = restTemplate.exchange(
                    GITHUB_USER_API, HttpMethod.GET, entity, String.class);

            if (userResponse.getStatusCode() != HttpStatus.OK || userResponse.getBody() == null) {
                throw new RuntimeException("Failed to fetch GitHub user info: " + userResponse.getStatusCode());
            }

            JsonNode userNode = objectMapper.readTree(userResponse.getBody());
            GitHubUserInfo userInfo = new GitHubUserInfo();
            userInfo.setId(userNode.get("id").asLong());
            userInfo.setLogin(userNode.get("login").asText());
            userInfo.setName(userNode.has("name") && !userNode.get("name").isNull()
                    ? userNode.get("name").asText() : null);
            userInfo.setEmail(userNode.has("email") && !userNode.get("email").isNull()
                    ? userNode.get("email").asText() : null);
            userInfo.setAvatarUrl(userNode.has("avatar_url") ? userNode.get("avatar_url").asText() : null);

            if (userInfo.getEmail() == null) {
                userInfo.setEmail(fetchPrimaryEmail(accessToken));
            }
            return userInfo;
        } catch (Exception e) {
            log.error("Failed to fetch GitHub user info", e);
            throw new RuntimeException("Failed to call GitHub API", e);
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
            JwtEncoder jwtEncoder,
            OAuth2ResponseUtil oauth2ResponseUtil) {
        if (principal == null) {
            throw new OAuth2Exception(ResultCode.UNAUTHORIZED, "Not authenticated");
        }

        OAuth2AuthorizedClient authorizedClient =
                authorizedClientService.loadAuthorizedClient("github", principal.getName());
        if (authorizedClient == null) {
            throw new OAuth2Exception(ResultCode.UNAUTHORIZED, "GitHub authorization not found");
        }

        UserDTO userDTO = getOrCreateUser(authorizedClient);
        if (userDTO == null) {
            throw new SystemException("Failed to get user info");
        }

        return oauth2ResponseUtil.buildSimpleLoginResponse(userDTO, jwtEncoder);
    }

    public boolean checkAuthStatus(
            Principal principal,
            OAuth2AuthorizedClientService authorizedClientService) {
        if (principal == null) {
            return false;
        }
        OAuth2AuthorizedClient authorizedClient =
                authorizedClientService.loadAuthorizedClient("github", principal.getName());
        return authorizedClient != null;
    }

    private String generateSecurePassword() {
        byte[] randomBytes = new byte[12];
        SECURE_RANDOM.nextBytes(randomBytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return "Aa1" + raw.substring(0, Math.min(raw.length(), 9));
    }

    private String buildUsername(GitHubUserInfo githubUserInfo) {
        String base = githubUserInfo.getLogin() != null ? githubUserInfo.getLogin() : "user";
        String normalized = base.replaceAll("[^a-zA-Z0-9_]", "_");
        if (normalized.length() > 14) {
            normalized = normalized.substring(0, 14);
        }
        if (normalized.length() < 4) {
            normalized = normalized + "user";
        }
        return "gh_" + normalized;
    }

    @Getter
    private static class GitHubUserInfo {
        private Long id;
        private String login;
        private String name;
        private String email;
        private String avatarUrl;

        public void setId(Long id) {
            this.id = id;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }
    }
}
