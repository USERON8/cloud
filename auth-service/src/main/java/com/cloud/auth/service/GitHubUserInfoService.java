package com.cloud.auth.service;

import com.cloud.api.user.UserFeignClient;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.UserType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * GitHub用户信息服务
 * 负责从GitHub API获取用户信息并同步到系统中
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubUserInfoService implements ApplicationContextAware {

    private static final String GITHUB_USER_API = "https://api.github.com/user";
    private static final String GITHUB_USER_EMAILS_API = "https://api.github.com/user/emails";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    // 通过ApplicationContext动态获取UserFeignClient，解决循环依赖问题
    private ApplicationContext applicationContext;

    /**
     * 从GitHub OAuth2授权客户端获取用户信息并同步到系统
     *
     * @param authorizedClient OAuth2授权客户端
     * @return 系统用户信息
     */
    public UserDTO getOrCreateUser(OAuth2AuthorizedClient authorizedClient) {
        try {
            // 从GitHub API获取用户基本信息
            GitHubUserInfo githubUserInfo = fetchGitHubUserInfo(authorizedClient);
            log.info("获取到GitHub用户信息: id={}, login={}, name={}, email={}",
                    githubUserInfo.getId(), githubUserInfo.getLogin(),
                    githubUserInfo.getName(), githubUserInfo.getEmail());

            // 检查用户是否已存在
            String username = "github_" + githubUserInfo.getLogin();
            UserFeignClient userFeignClient = applicationContext.getBean(UserFeignClient.class);
            UserDTO existingUser = userFeignClient.findByUsername(username);

            if (existingUser != null) {
                log.info("GitHub用户已存在: {}", username);
                return existingUser;
            }

            // 创建新用户
            RegisterRequestDTO registerRequest = new RegisterRequestDTO();
            registerRequest.setUsername(username);
            registerRequest.setNickname(githubUserInfo.getName() != null ?
                    githubUserInfo.getName() : githubUserInfo.getLogin());
            registerRequest.setUserType(UserType.USER.getCode());
            registerRequest.setPassword("github_oauth2_" + githubUserInfo.getId()); // OAuth2用户不使用密码登录
            registerRequest.setPhone("000-0000-0000"); // GitHub用户默认手机号

            UserDTO newUser = userFeignClient.register(registerRequest);
            if (newUser != null) {
                log.info("GitHub用户注册成功: username={}, userId={}", username, newUser.getId());
                return newUser;
            } else {
                log.error("GitHub用户注册失败: {}", username);
                throw new RuntimeException("GitHub用户注册失败");
            }

        } catch (Exception e) {
            log.error("处理GitHub用户信息时发生异常", e);
            throw new RuntimeException("处理GitHub用户信息失败", e);
        }
    }

    /**
     * 从GitHub API获取用户信息
     *
     * @param authorizedClient OAuth2授权客户端
     * @return GitHub用户信息
     */
    private GitHubUserInfo fetchGitHubUserInfo(OAuth2AuthorizedClient authorizedClient) {
        try {
            String accessToken = authorizedClient.getAccessToken().getTokenValue();

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setAccept(MediaType.parseMediaTypes("application/vnd.github.v3+json"));
            HttpEntity<?> entity = new HttpEntity<>(headers);

            // 获取用户基本信息
            ResponseEntity<String> userResponse = restTemplate.exchange(
                    GITHUB_USER_API, HttpMethod.GET, entity, String.class);

            if (userResponse.getStatusCode() == HttpStatus.OK) {
                JsonNode userNode = objectMapper.readTree(userResponse.getBody());

                GitHubUserInfo userInfo = new GitHubUserInfo();
                userInfo.setId(userNode.get("id").asLong());
                userInfo.setLogin(userNode.get("login").asText());
                userInfo.setName(userNode.has("name") && !userNode.get("name").isNull() ?
                        userNode.get("name").asText() : null);
                userInfo.setEmail(userNode.has("email") && !userNode.get("email").isNull() ?
                        userNode.get("email").asText() : null);
                userInfo.setAvatarUrl(userNode.has("avatar_url") ?
                        userNode.get("avatar_url").asText() : null);

                // 如果基本信息中没有邮箱，尝试获取邮箱列表
                if (userInfo.getEmail() == null) {
                    userInfo.setEmail(fetchPrimaryEmail(accessToken));
                }

                return userInfo;
            } else {
                throw new RuntimeException("获取GitHub用户信息失败，状态码: " + userResponse.getStatusCode());
            }

        } catch (Exception e) {
            log.error("调用GitHub API获取用户信息时发生异常", e);
            throw new RuntimeException("调用GitHub API失败", e);
        }
    }

    /**
     * 获取GitHub用户的主邮箱地址
     *
     * @param accessToken 访问令牌
     * @return 主邮箱地址
     */
    private String fetchPrimaryEmail(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setAccept(MediaType.parseMediaTypes("application/vnd.github.v3+json"));
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> emailResponse = restTemplate.exchange(
                    GITHUB_USER_EMAILS_API, HttpMethod.GET, entity, String.class);

            if (emailResponse.getStatusCode() == HttpStatus.OK) {
                JsonNode emailsNode = objectMapper.readTree(emailResponse.getBody());

                // 查找主邮箱
                for (JsonNode emailNode : emailsNode) {
                    if (emailNode.has("primary") && emailNode.get("primary").asBoolean()) {
                        return emailNode.get("email").asText();
                    }
                }

                // 如果没有找到主邮箱，返回第一个邮箱
                if (emailsNode.size() > 0) {
                    return emailsNode.get(0).get("email").asText();
                }
            }

        } catch (Exception e) {
            log.warn("获取GitHub用户邮箱时发生异常", e);
        }

        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * GitHub用户信息内部类
     */
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
