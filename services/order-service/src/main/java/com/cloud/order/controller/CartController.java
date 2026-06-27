package com.cloud.order.controller;

import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.order.dto.CartDTO;
import com.cloud.order.dto.CartSyncRequest;
import com.cloud.order.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/cart")
@RequiredArgsConstructor
@Validated
@Tag(name = "购物车接口", description = "当前用户购物车接口")
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "请求参数或购物车状态无效"),
  @ApiResponse(responseCode = "401", description = "需要认证"),
  @ApiResponse(responseCode = "403", description = "权限不足"),
  @ApiResponse(responseCode = "500", description = "服务内部错误")
})
public class CartController {

  private final CartService cartService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "获取当前用户有效购物车")
  public Result<CartDTO> getCurrentCart(Authentication authentication) {
    return Result.success(
        cartService.getCurrentCart(
            SecurityPermissionUtils.requireCurrentUserIdAsLong(authentication)));
  }

  @PutMapping("/items")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "同步当前用户购物车")
  public Result<CartDTO> syncCart(
      @Valid @RequestBody CartSyncRequest request, Authentication authentication) {
    return Result.success(
        cartService.syncCart(
            SecurityPermissionUtils.requireCurrentUserIdAsLong(authentication), request));
  }
}
