package com.cloud.user.controller.user;

import com.cloud.common.annotation.RequireScope;
import com.cloud.common.annotation.RequireUserType;
import com.cloud.common.annotation.RequireUserType.UserType;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/query/users")
@RequiredArgsConstructor
@Tag(name = "用户查询", description = "用户信息查询相关操作")
public class UserQueryController {
    private final UserService userService;

    @GetMapping
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:read")
    @Operation(summary = "根据用户名查询用户", description = "根据用户名查询用户信息")
    public Result<UserDTO> findByUsername(@RequestParam
                                          @Parameter(description = "用户名")
                                          @NotBlank(message = "用户名不能为空") String username) {
        return Result.success("查询成功", userService.findByUsername(username));
    }

    @GetMapping("/search")
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:read")
    @Operation(summary = "分页查询用户", description = "分页查询用户信息")
    public Result<PageResult<UserVO>> search(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "20") Integer size,
                                             @RequestParam(required = false) String username,
                                             @RequestParam(required = false) String email,
                                             @RequestParam(required = false) String userType) {
        UserPageDTO userPageDTO = new UserPageDTO();
        userPageDTO.setCurrent(page.longValue());
        userPageDTO.setSize(size.longValue());
        userPageDTO.setUsername(username);
        userPageDTO.setPhone(email); // UserPageDTO没有email字段，使用phone字段
        userPageDTO.setUserType(userType);
        return Result.success(userService.pageQuery(userPageDTO));
    }

    @RequestMapping("/findByGitHubId")
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:read")
    @Operation(summary = "根据GitHub ID查询用户", description = "根据GitHub用户ID查询用户信息（OAuth专用）")
    public Result<UserDTO> findByGitHubId(@RequestParam
                                          @Parameter(description = "GitHub用户ID")
                                          @NotNull(message = "GitHub用户ID不能为空") Long githubId) {
        try {
            UserDTO userDTO = userService.findByGitHubId(githubId);
            if (userDTO != null) {
                return Result.success("查询成功", userDTO);
            } else {
                return Result.error(404, "未找到对应的GitHub用户");
            }
        } catch (Exception e) {
            log.error("根据GitHub ID查询用户失败, githubId: {}", githubId, e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    @RequestMapping("/findByGitHubUsername")
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:read")
    @Operation(summary = "根据GitHub用户名查询用户", description = "根据GitHub用户名查询用户信息（OAuth专用）")
    public Result<UserDTO> findByGitHubUsername(@RequestParam
                                                @Parameter(description = "GitHub用户名")
                                                @NotBlank(message = "GitHub用户名不能为空") String githubUsername) {
        try {
            UserDTO userDTO = userService.findByGitHubUsername(githubUsername);
            if (userDTO != null) {
                return Result.success("查询成功", userDTO);
            } else {
                return Result.error(404, "未找到对应的GitHub用户");
            }
        } catch (Exception e) {
            log.error("根据GitHub用户名查询用户失败, githubUsername: {}", githubUsername, e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    @RequestMapping("/findByOAuthProvider")
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:read")
    @Operation(summary = "根据OAuth提供商查询用户", description = "根据OAuth提供商和提供商ID查询用户信息")
    public Result<UserDTO> findByOAuthProvider(@RequestParam
                                               @Parameter(description = "OAuth提供商")
                                               @NotBlank(message = "OAuth提供商不能为空") String oauthProvider,
                                               @RequestParam
                                               @Parameter(description = "OAuth提供商用户ID")
                                               @NotBlank(message = "OAuth提供商用户ID不能为空") String oauthProviderId) {
        try {
            UserDTO userDTO = userService.findByOAuthProvider(oauthProvider, oauthProviderId);
            if (userDTO != null) {
                return Result.success("查询成功", userDTO);
            } else {
                return Result.error(404, "未找到对应的OAuth用户");
            }
        } catch (Exception e) {
            log.error("根据OAuth提供商查询用户失败, provider: {}, providerId: {}",
                    oauthProvider, oauthProviderId, e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }
}