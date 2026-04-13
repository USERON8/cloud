package com.cloud.order.controller;

import com.cloud.common.exception.BizException;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/cart")
@RequiredArgsConstructor
@Validated
@Tag(name = "Cart API", description = "Current user cart APIs")
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "Invalid request or cart state"),
  @ApiResponse(responseCode = "401", description = "Authentication required"),
  @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
  @ApiResponse(responseCode = "500", description = "Internal server error")
})
public class CartController {

  private final CartService cartService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get current user active cart")
  public Result<CartDTO> getCurrentCart(Authentication authentication) {
    return Result.success(cartService.getCurrentCart(requireCurrentUserId(authentication)));
  }

  @PostMapping("/sync")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Synchronize current user cart")
  public Result<CartDTO> syncCart(
      @Valid @RequestBody CartSyncRequest request, Authentication authentication) {
    return Result.success(cartService.syncCart(requireCurrentUserId(authentication), request));
  }

  private Long requireCurrentUserId(Authentication authentication) {
    String userId = SecurityPermissionUtils.getCurrentUserId(authentication);
    if (userId == null || userId.isBlank()) {
      throw new BizException("current user not found in token");
    }
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException ex) {
      throw new BizException("invalid user_id in token");
    }
  }
}
