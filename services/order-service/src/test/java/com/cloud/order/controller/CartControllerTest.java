package com.cloud.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.order.dto.CartDTO;
import com.cloud.order.dto.CartSyncRequest;
import com.cloud.order.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

  @Mock private CartService cartService;

  @InjectMocks private CartController cartController;

  @Test
  void getCurrentCartShouldUseCurrentUserId() {
    CartDTO cart = new CartDTO();
    cart.setId(10L);
    when(cartService.getCurrentCart(101L)).thenReturn(cart);

    var result = cartController.getCurrentCart(authentication("101", "ROLE_USER"));

    assertThat(result.getData()).isSameAs(cart);
    verify(cartService).getCurrentCart(101L);
  }

  @Test
  void syncCartShouldUseCurrentUserId() {
    CartSyncRequest request = new CartSyncRequest();
    CartDTO cart = new CartDTO();
    cart.setId(11L);
    when(cartService.syncCart(eq(101L), same(request))).thenReturn(cart);

    var result = cartController.syncCart(request, authentication("101", "ROLE_USER"));

    assertThat(result.getData()).isSameAs(cart);
    verify(cartService).syncCart(eq(101L), same(request));
  }

  private JwtAuthenticationToken authentication(String userId, String primaryRole) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("user_id", userId)
            .claim("username", "tester")
            .build();
    return new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList(primaryRole));
  }
}
