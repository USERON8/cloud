package com.cloud.order.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.service.OrderQueryService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.OrderShippingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class OrderBatchServiceImplTest {

  @Mock private OrderService orderService;

  @Mock private OrderQueryService orderQueryService;

  @Mock private OrderShippingService orderShippingService;

  private OrderBatchServiceImpl orderBatchService;

  @BeforeEach
  void setUp() {
    orderBatchService =
        new OrderBatchServiceImpl(orderService, orderQueryService, orderShippingService);
  }

  @Test
  void applyOrderActionShouldRejectDirectPay() {
    assertThatThrownBy(
            () -> orderBatchService.applyOrderAction(1L, null, OrderAction.PAY, null, null, null))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex ->
                org.assertj.core.api.Assertions.assertThat(((BizException) ex).getCode())
                    .isEqualTo(ResultCode.BAD_REQUEST.getCode()))
        .hasMessageContaining("direct pay actions are disabled");

    verifyNoInteractions(orderService, orderQueryService, orderShippingService);
  }

  @Test
  void batchApplyShouldRejectDirectPay() {
    assertThatThrownBy(
            () ->
                orderBatchService.batchApply(
                    List.of(1L, 2L), null, OrderAction.PAY, null, null, null))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex ->
                org.assertj.core.api.Assertions.assertThat(((BizException) ex).getCode())
                    .isEqualTo(ResultCode.BAD_REQUEST.getCode()))
        .hasMessageContaining("direct pay actions are disabled");

    verifyNoInteractions(orderService, orderQueryService, orderShippingService);
  }

  @Test
  void applyOrderActionShouldRejectShippingForRegularUser() {
    JwtAuthenticationToken authentication = authentication("101", "ROLE_USER", "order:query");
    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(10L);
    OrderSub subOrder = new OrderSub();
    subOrder.setId(20L);
    subOrder.setMainOrderId(10L);
    subOrder.setMerchantId(300L);

    when(orderQueryService.requireAccessibleMainOrder(10L, authentication)).thenReturn(mainOrder);
    when(orderService.listSubOrders(10L)).thenReturn(List.of(subOrder));

    assertThatThrownBy(
            () ->
                orderBatchService.applyOrderAction(
                    10L, authentication, OrderAction.SHIP, "SF", "T-10", null))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex ->
                org.assertj.core.api.Assertions.assertThat(((BizException) ex).getCode())
                    .isEqualTo(ResultCode.FORBIDDEN.getCode()))
        .hasMessageContaining("shipping requires merchant or admin privileges");

    verify(orderService).listSubOrders(10L);
    verifyNoInteractions(orderShippingService);
  }

  @Test
  void applyOrderActionShouldRejectCompletionForMerchant() {
    JwtAuthenticationToken authentication = authentication("201", "ROLE_MERCHANT", "order:query");
    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(10L);
    OrderSub subOrder = new OrderSub();
    subOrder.setId(20L);
    subOrder.setMainOrderId(10L);
    subOrder.setMerchantId(201L);

    when(orderQueryService.requireAccessibleMainOrder(10L, authentication)).thenReturn(mainOrder);
    when(orderService.listSubOrders(10L)).thenReturn(List.of(subOrder));

    assertThatThrownBy(
            () ->
                orderBatchService.applyOrderAction(
                    10L, authentication, OrderAction.DONE, null, null, null))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex ->
                org.assertj.core.api.Assertions.assertThat(((BizException) ex).getCode())
                    .isEqualTo(ResultCode.FORBIDDEN.getCode()))
        .hasMessageContaining("order completion requires the order owner or admin privileges");

    verify(orderService).listSubOrders(10L);
    verifyNoInteractions(orderShippingService);
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
