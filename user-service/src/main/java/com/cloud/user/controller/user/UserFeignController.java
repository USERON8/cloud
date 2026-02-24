package com.cloud.user.controller.user;

import com.cloud.api.user.UserFeignClient;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.oauth.GitHubUserDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;




@Slf4j
@RestController
@RequestMapping("/internal/user")
@RequiredArgsConstructor
public class UserFeignController implements UserFeignClient {

    private final UserService userService;

    @Override
    @GetMapping("/username/{username}")
    public UserDTO findByUsername(@PathVariable("username") String username) {
        return userService.findByUsername(username);
    }

    @Override
    @GetMapping("/id/{id}")
    public UserDTO findById(@PathVariable("id") Long id) {
        return userService.getUserById(id);
    }

    @Override
    @PostMapping("/register")
    public UserDTO register(@RequestBody RegisterRequestDTO registerRequest) {
        return userService.registerUser(registerRequest);
    }

    @Override
    @PutMapping("/update")
    public Boolean update(@RequestBody UserDTO userDTO) {
        return userService.updateUser(userDTO);
    }

    @Override
    @GetMapping("/password/{username}")
    public String getUserPassword(@PathVariable("username") String username) {
        return userService.getUserPassword(username);
    }

    @Override
    @GetMapping("/github-id/{githubId}")
    public UserDTO findByGitHubId(@PathVariable("githubId") Long githubId) {
        return userService.findByGitHubId(githubId);
    }

    @Override
    @GetMapping("/oauth")
    public UserDTO findByOAuthProvider(@RequestParam("oauthProvider") String oauthProvider,
                                       @RequestParam("oauthProviderId") String oauthProviderId) {
        return userService.findByOAuthProvider(oauthProvider, oauthProviderId);
    }

    @Override
    @PostMapping("/github/create")
    public UserDTO createGitHubUser(@RequestBody GitHubUserDTO githubUserDTO) {
        return userService.createGitHubUser(githubUserDTO);
    }

    @Override
    @PutMapping("/github/update/{userId}")
    public Boolean updateGitHubUserInfo(@PathVariable("userId") Long userId,
                                        @RequestBody GitHubUserDTO githubUserDTO) {
        return userService.updateGitHubUserInfo(userId, githubUserDTO);
    }
}
