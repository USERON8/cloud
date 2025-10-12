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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;

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
    // 安全随机数生成器，用于生成安全密码
    private static final java.security.SecureRandom secureRandom = new java.security.SecureRandom();
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
            registerRequest.setPassword(generateSecurePassword()); // 生成安全随机密码
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
     * 获取GitHub用户信息并生成Token
     * Controller层调用的标准方法
     *
     * @param principal               认证主体
     * @param authorizedClientService OAuth2客户端服务
     * @param jwtEncoder              JWT编码器
     * @param oauth2ResponseUtil      OAuth2响应工具
     * @return 登录响应DTO
     * @throws OAuth2Exception 当认证失败或授权信息不存在时抛出
     * @throws SystemException 当系统处理失败时抛出
     */
    public LoginResponseDTO getUserInfoAndGenerateToken(
            Principal principal,
            OAuth2AuthorizedClientService authorizedClientService,
            JwtEncoder jwtEncoder,
            OAuth2ResponseUtil oauth2ResponseUtil) {

        // 1. 验证principal
        if (principal == null) {
            log.warn("未找到认证信息");
            throw new OAuth2Exception(ResultCode.UNAUTHORIZED, "未认证，请先登录");
        }

        // 2. 获取OAuth2授权客户端
        OAuth2AuthorizedClient authorizedClient = authorizedClientService
                .loadAuthorizedClient("github", principal.getName());

        if (authorizedClient == null) {
            log.warn("未找到GitHub OAuth2授权客户端，用户: {}", principal.getName());
            throw new OAuth2Exception(ResultCode.UNAUTHORIZED, "GitHub授权信息不存在");
        }

        // 3. 获取或创建用户
        UserDTO userDTO = getOrCreateUser(authorizedClient);

        if (userDTO == null) {
            log.error("获取GitHub用户信息失败");
            throw new SystemException("获取用户信息失败");
        }

        // 4. 生成JWT响应
        LoginResponseDTO loginResponse = oauth2ResponseUtil.buildSimpleLoginResponse(userDTO, jwtEncoder);
        log.info("成功获取GitHub用户信息: {}", userDTO.getUsername());

        return loginResponse;
    }

    /**
     * 检查GitHub OAuth2认证状态
     * Controller层调用的标准方法
     *
     * @param principal               认证主体
     * @param authorizedClientService OAuth2客户端服务
     * @return 是否已认证
     */
    public boolean checkAuthStatus(
            Principal principal,
            OAuth2AuthorizedClientService authorizedClientService) {

        if (principal == null) {
            return false;
        }

        OAuth2AuthorizedClient authorizedClient = authorizedClientService
                .loadAuthorizedClient("github", principal.getName());

        boolean isAuthenticated = authorizedClient != null;
        log.info("GitHub OAuth2认证状态: {}", isAuthenticated);

        return isAuthenticated;
    }

    /**
     * 生成安全的随机密码
     * 用于GitHub OAuth2用户，这些用户不会使用密码登录，但需要符合安全要求
     *
     * @return 安全的随机密码
     */
    private String generateSecurePassword() {
        // 生成32字节的随机数据
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        // 使用Base64编码，并添加前缀标识这是OAuth2用户
        String randomPassword = java.util.Base64.getEncoder().encodeToString(randomBytes);
        String securePassword = "oauth2_github_" + randomPassword;

        log.debug("为GitHub OAuth2用户生成安全随机密码，长度: {}", securePassword.length());
        return securePassword;
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
