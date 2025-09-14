package com.cloud.user.controller.user;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.vo.UserVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.user.constants.OAuth2Permissions;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user/query")
@RequiredArgsConstructor
@Tag(name = "用户查询", description = "用户信息查询相关操作")
public class UserQueryController {
    private final UserService userService;

    @RequestMapping("/findByUsername")
    @Operation(summary = "根据用户名查询用户", description = "根据用户名查询用户信息")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<UserDTO> findByUsername(@RequestParam
                                          @Parameter(description = "用户名")
                                          @NotBlank(message = "用户名不能为空") String username) {
        return Result.success("查询成功", userService.findByUsername(username));
    }

    @RequestMapping("/page")
    @Operation(summary = "分页查询用户", description = "分页查询用户信息")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<PageResult<UserVO>> page(@RequestBody
                                           @Parameter(description = "分页查询条件")
                                           @Valid @NotNull(message = "分页查询条件不能为空") UserPageDTO userPageDTO) {
        return Result.success(userService.pageQuery(userPageDTO));
    }
}