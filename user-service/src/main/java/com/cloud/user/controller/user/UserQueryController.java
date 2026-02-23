package com.cloud.user.controller.user;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.vo.user.UserVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/query/users")
@RequiredArgsConstructor
@Tag(name = "User Query", description = "User query APIs")
public class UserQueryController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "Find user by username", description = "Get one user by username")
    public Result<UserDTO> findByUsername(
            @RequestParam
            @Parameter(description = "Username")
            @NotBlank(message = "username is required") String username) {
        return Result.success("query successful", userService.findByUsername(username));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "Search users", description = "Search users with paging")
    public Result<PageResult<UserVO>> search(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String userType) {
        UserPageDTO userPageDTO = new UserPageDTO();
        userPageDTO.setCurrent(page.longValue());
        userPageDTO.setSize(size.longValue());
        userPageDTO.setUsername(username);
        userPageDTO.setEmail(email);
        userPageDTO.setUserType(userType);
        return Result.success(userService.pageQuery(userPageDTO));
    }

    @RequestMapping("/findByGitHubId")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "Find user by GitHub ID", description = "Get one user by GitHub ID")
    public Result<UserDTO> findByGitHubId(
            @RequestParam
            @Parameter(description = "GitHub ID")
            @NotNull(message = "github id is required") Long githubId) {
        UserDTO userDTO = userService.findByGitHubId(githubId);
        if (userDTO != null) {
            return Result.success("query successful", userDTO);
        }
        return Result.notFound("user not found by github id");
    }

    @RequestMapping("/findByGitHubUsername")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "Find user by GitHub username", description = "Get one user by GitHub username")
    public Result<UserDTO> findByGitHubUsername(
            @RequestParam
            @Parameter(description = "GitHub username")
            @NotBlank(message = "github username is required") String githubUsername) {
        UserDTO userDTO = userService.findByGitHubUsername(githubUsername);
        if (userDTO != null) {
            return Result.success("query successful", userDTO);
        }
        return Result.notFound("user not found by github username");
    }

    @RequestMapping("/findByOAuthProvider")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "Find user by OAuth provider", description = "Get one user by OAuth provider and provider ID")
    public Result<UserDTO> findByOAuthProvider(
            @RequestParam
            @Parameter(description = "OAuth provider")
            @NotBlank(message = "oauth provider is required") String oauthProvider,
            @RequestParam
            @Parameter(description = "OAuth provider ID")
            @NotBlank(message = "oauth provider id is required") String oauthProviderId) {
        UserDTO userDTO = userService.findByOAuthProvider(oauthProvider, oauthProviderId);
        if (userDTO != null) {
            return Result.success("query successful", userDTO);
        }
        return Result.notFound("user not found by oauth provider");
    }
}
