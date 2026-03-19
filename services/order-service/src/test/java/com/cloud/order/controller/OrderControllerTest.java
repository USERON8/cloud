package com.cloud.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.order.converter.AfterSaleDtoConverter;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.service.OrderBatchService;
import com.cloud.order.service.OrderPlacementService;
import com.cloud.order.service.OrderQueryService;
import com.cloud.order.service.OrderService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

  @Mock private OrderService orderService;

  @Mock private OrderPlacementService orderPlacementService;

  @Mock private OrderBatchService orderBatchService;

  @Mock private OrderQueryService orderQueryService;

  @Mock private AfterSaleDtoConverter afterSaleDtoConverter;

  @InjectMocks private OrderController orderController;

  @Test
  void createMainOrderShouldFillUserIdFromTokenForRegularUser() {
    CreateMainOrderRequest request = buildDirectOrderRequest();
    request.setUserId(null);
    OrderAggregateResponse response = new OrderAggregateResponse();
    when(orderPlacementService.createOrder(same(request))).thenReturn(response);

    var result =
        orderController.createMainOrder(
            request, " key-123 ", authentication("101", "ROLE_USER", "order:create"));

    assertThat(request.getUserId()).isEqualTo(101L);
    assertThat(request.getIdempotencyKey()).isEqualTo("key-123");
    assertThat(result.getData()).isSameAs(response);
    verify(orderPlacementService).createOrder(same(request));
  }

  @Test
  void createMainOrderShouldRejectMissingUserIdForAdmin() {
    CreateMainOrderRequest request = buildDirectOrderRequest();
    request.setUserId(null);

    assertThatThrownBy(
            () ->
                orderController.createMainOrder(
                    request, "admin-key", authentication("1", "ROLE_ADMIN", "order:create")))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex -> {
              BizException bizException = (BizException) ex;
              assertThat(bizException.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
            })
        .hasMessageContaining("userId is required for admin order creation");
  }

  private CreateMainOrderRequest buildDirectOrderRequest() {
    CreateMainOrderRequest request = new CreateMainOrderRequest();
    request.setSpuId(10L);
    request.setSkuId(20L);
    request.setQuantity(1);
    request.setTotalAmount(BigDecimal.valueOf(99.99));
    request.setPayableAmount(BigDecimal.valueOf(99.99));
    request.setReceiverName("tester");
    request.setReceiverPhone("13800138000");
    request.setReceiverAddress("cloud road");
    return request;
  }

  private JwtAuthenticationToken authentication(
      String userId, String primaryRole, String... authorities) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("user_id", userId)
            .claim("username", "tester")
            .build();
    return new JwtAuthenticationToken(
        jwt, AuthorityUtils.createAuthorityList(merge(primaryRole, authorities)));
  }

  private String[] merge(String first, String[] remaining) {
    String[] merged = new String[remaining.length + 1];
    merged[0] = first;
    System.arraycopy(remaining, 0, merged, 1, remaining.length);
    return merged;
  }
}
