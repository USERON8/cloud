package com.cloud.auth.controller;

import com.cloud.auth.service.AuthorizationRequestSessionService;
import com.cloud.auth.service.GitHubUserInfoService;
import com.cloud.common.domain.dto.auth.AuthorizationRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth/oauth2/github")
@RequiredArgsConstructor
@Tag(name = "GitHub OAuth2 接口", description = "GitHub OAuth2 登录端点")
@Validated
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "GitHub OAuth2 请求无效"),
  @ApiResponse(responseCode = "401", description = "需要认证"),
  @ApiResponse(responseCode = "500", description = "GitHub OAuth2 内部错误")
})
public class GitHubOAuth2Controller {

  private final GitHubUserInfoService gitHubUserInfoService;
  private final OAuth2AuthorizedClientService authorizedClientService;
  private final AuthorizationRequestSessionService authorizationRequestSessionService;

  @GetMapping("/user-info")
  @Operation(summary = "获取 GitHub 用户信息")
  public Result<UserDTO> getUserInfo(@Parameter(hidden = true) Principal principal) {
    UserDTO user = gitHubUserInfoService.getAuthorizedUser(principal, authorizedClientService);
    return Result.success(user);
  }

  @GetMapping("/status")
  @Operation(summary = "检查 GitHub 授权状态")
  public Result<Boolean> checkAuthStatus(@Parameter(hidden = true) Principal principal) {
    boolean isAuthenticated =
        gitHubUserInfoService.checkAuthStatus(principal, authorizedClientService);
    return Result.success(isAuthenticated);
  }

  @GetMapping("/callback")
  @Operation(summary = "处理 GitHub OAuth2 回调")
  public Result<String> handleCallback() {
    return Result.success("GitHub callback is handled by /login/oauth2/code/github");
  }

  @GetMapping("/login-url")
  @Operation(summary = "获取 GitHub OAuth2 登录地址")
  public Result<String> getGitHubLoginUrl(
      @Valid @ModelAttribute AuthorizationRequestDTO authorizationRequest,
      @Parameter(hidden = true) HttpServletRequest request) {
    authorizationRequestSessionService.store(authorizationRequest, request);
    return Result.success("/oauth2/authorization/github");
  }
}
