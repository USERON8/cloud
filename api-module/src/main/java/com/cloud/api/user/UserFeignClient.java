package com.cloud.api.user;

import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.oauth.GitHubUserDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", path = "/internal/user", contextId = "userFeignClient")
public interface UserFeignClient {

    @GetMapping("/username/{username}")
    UserDTO findByUsername(@PathVariable("username") String username);

    @GetMapping("/id/{id}")
    UserDTO findById(@PathVariable("id") Long id);

    @PostMapping("/register")
    UserDTO register(@RequestBody RegisterRequestDTO registerRequest);

    @PutMapping("/update")
    Boolean update(@RequestBody UserDTO userDTO);

    @GetMapping("/password/{username}")
    String getUserPassword(@PathVariable("username") String username);

    @GetMapping("/github-id/{githubId}")
    UserDTO findByGitHubId(@PathVariable("githubId") Long githubId);

    @GetMapping("/oauth")
    UserDTO findByOAuthProvider(@RequestParam("oauthProvider") String oauthProvider,
                                @RequestParam("oauthProviderId") String oauthProviderId);

    @PostMapping("/github/create")
    UserDTO createGitHubUser(@RequestBody GitHubUserDTO githubUserDTO);

    @PutMapping("/github/update/{userId}")
    Boolean updateGitHubUserInfo(@PathVariable("userId") Long userId,
                                 @RequestBody GitHubUserDTO githubUserDTO);
}
