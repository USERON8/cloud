package com.cloud.api.user;

import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.oauth.GitHubUserDTO;
import com.cloud.common.domain.dto.user.UserDTO;

public interface UserFeignClient {

    UserDTO findByUsername(String username);

    UserDTO findById(Long id);

    UserDTO register(RegisterRequestDTO registerRequest);

    Boolean update(UserDTO userDTO);

    String getUserPassword(String username);

    UserDTO findByGitHubId(Long githubId);

    UserDTO findByOAuthProvider(String oauthProvider, String oauthProviderId);

    UserDTO createGitHubUser(GitHubUserDTO githubUserDTO);

    Boolean updateGitHubUserInfo(Long userId, GitHubUserDTO githubUserDTO);
}
