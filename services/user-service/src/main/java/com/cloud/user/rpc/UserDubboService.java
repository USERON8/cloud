package com.cloud.user.rpc;

import com.cloud.api.user.UserDubboApi;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.oauth.GitHubUserDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = UserDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class UserDubboService implements UserDubboApi {

    private final UserService userService;

    @Override
    public UserDTO findByUsername(String username) {
        return userService.findByUsername(username);
    }

    @Override
    public UserDTO findById(Long id) {
        return userService.getUserById(id);
    }

    @Override
    public UserDTO register(RegisterRequestDTO registerRequest) {
        return userService.registerUser(registerRequest);
    }

    @Override
    public Boolean update(UserDTO userDTO) {
        return userService.updateUser(userDTO);
    }

    @Override
    public String getUserPassword(String username) {
        return userService.getUserPassword(username);
    }

    @Override
    public UserDTO findByGitHubId(Long githubId) {
        return userService.findByGitHubId(githubId);
    }

    @Override
    public UserDTO findByOAuthProvider(String oauthProvider, String oauthProviderId) {
        return userService.findByOAuthProvider(oauthProvider, oauthProviderId);
    }

    @Override
    public UserDTO createGitHubUser(GitHubUserDTO githubUserDTO) {
        return userService.createGitHubUser(githubUserDTO);
    }

    @Override
    public Boolean updateGitHubUserInfo(Long userId, GitHubUserDTO githubUserDTO) {
        return userService.updateGitHubUserInfo(userId, githubUserDTO);
    }
}

