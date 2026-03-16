package com.cloud.auth.controller;

import com.cloud.auth.service.AuthorizationRequestSessionService;
import com.cloud.auth.service.GitHubUserInfoService;
import com.cloud.common.domain.dto.auth.AuthorizationRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth/oauth2/github")
@RequiredArgsConstructor
@Tag(name = "GitHub OAuth2 API", description = "GitHub OAuth2 login endpoints")
public class GitHubOAuth2Controller {

  private final GitHubUserInfoService gitHubUserInfoService;
  private final OAuth2AuthorizedClientService authorizedClientService;
  private final AuthorizationRequestSessionService authorizationRequestSessionService;

  @GetMapping("/user-info")
  @Operation(summary = "Get GitHub user info")
  public Result<UserDTO> getUserInfo(Principal principal) {
    UserDTO user = gitHubUserInfoService.getAuthorizedUser(principal, authorizedClientService);
    return Result.success(user);
  }

  @GetMapping("/status")
  @Operation(summary = "Check GitHub authorization status")
  public Result<Boolean> checkAuthStatus(Principal principal) {
    boolean isAuthenticated =
        gitHubUserInfoService.checkAuthStatus(principal, authorizedClientService);
    return Result.success(isAuthenticated);
  }

  @GetMapping("/callback")
  @Operation(summary = "Handle GitHub OAuth2 callback")
  public Result<String> handleCallback() {
    return Result.success("GitHub callback is handled by /login/oauth2/code/github");
  }

  @GetMapping("/login-url")
  @Operation(summary = "Get GitHub OAuth2 login URL")
  public Result<String> getGitHubLoginUrl(
      @Valid @ModelAttribute AuthorizationRequestDTO authorizationRequest,
      HttpServletRequest request) {
    authorizationRequestSessionService.store(authorizationRequest, request);
    return Result.success("/oauth2/authorization/github");
  }
}
