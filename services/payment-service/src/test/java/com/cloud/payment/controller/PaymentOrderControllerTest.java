package com.cloud.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentCheckoutSessionVO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.payment.service.PaymentOrderService;
import com.cloud.payment.service.support.PaymentSecurityCacheService;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class PaymentOrderControllerTest {

  @Mock private PaymentOrderService paymentOrderService;

  @Mock private PaymentSecurityCacheService paymentSecurityCacheService;

  @Test
  void createPaymentOrderShouldAllowOwner() {
    PaymentOrderCommandDTO command = buildPaymentCommand();
    when(paymentOrderService.createPaymentOrder(same(command))).thenReturn(11L);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    var result = controller.createPaymentOrder(command, authentication("12", "ROLE_USER"));

    assertThat(result.getCode()).isEqualTo(200);
    assertThat(result.getData()).isEqualTo(11L);
    verify(paymentOrderService).createPaymentOrder(same(command));
  }

  @Test
  void createPaymentOrderShouldRejectOtherUser() {
    PaymentOrderCommandDTO command = buildPaymentCommand();
    command.setUserId(18L);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    BizException exception =
        assertThrows(
            BizException.class,
            () -> controller.createPaymentOrder(command, authentication("12", "ROLE_USER")));

    assertThat(exception.getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode());
    assertThat(exception.getMessage()).contains("another user");
  }

  @Test
  void createPaymentOrderShouldBackfillCurrentUserForRegularUser() {
    PaymentOrderCommandDTO command = buildPaymentCommand();
    command.setUserId(null);
    when(paymentOrderService.createPaymentOrder(same(command))).thenReturn(15L);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    var result = controller.createPaymentOrder(command, authentication("12", "ROLE_USER"));

    assertThat(result.getCode()).isEqualTo(200);
    assertThat(command.getUserId()).isEqualTo(12L);
    verify(paymentOrderService).createPaymentOrder(same(command));
  }

  @Test
  void getPaymentOrderByNoShouldAllowOwner() {
    PaymentOrderVO order = new PaymentOrderVO();
    order.setPaymentNo("PAY-1");
    order.setUserId(12L);
    when(paymentOrderService.getPaymentOrderByNo("PAY-1")).thenReturn(order);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    var result = controller.getPaymentOrderByNo("PAY-1", authentication("12", "ROLE_USER"));

    assertThat(result.getCode()).isEqualTo(200);
    assertThat(result.getData()).isSameAs(order);
  }

  @Test
  void getPaymentOrderByNoShouldForbidOtherUser() {
    PaymentOrderVO order = new PaymentOrderVO();
    order.setPaymentNo("PAY-2");
    order.setUserId(18L);
    when(paymentOrderService.getPaymentOrderByNo("PAY-2")).thenReturn(order);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    BizException exception =
        assertThrows(
            BizException.class,
            () -> controller.getPaymentOrderByNo("PAY-2", authentication("99", "ROLE_USER")));
    assertThat(exception.getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode());
  }

  @Test
  void getPaymentOrderByNoShouldAllowAdmin() {
    PaymentOrderVO order = new PaymentOrderVO();
    order.setPaymentNo("PAY-3");
    order.setUserId(18L);
    when(paymentOrderService.getPaymentOrderByNo("PAY-3")).thenReturn(order);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    var result = controller.getPaymentOrderByNo("PAY-3", authentication(null, "ROLE_ADMIN"));

    assertThat(result.getCode()).isEqualTo(200);
    assertThat(result.getData()).isSameAs(order);
  }

  @Test
  void getPaymentOrderByOrderNoShouldAllowOwner() {
    PaymentOrderVO order = new PaymentOrderVO();
    order.setPaymentNo("PAY-4");
    order.setMainOrderNo("M-1");
    order.setSubOrderNo("S-1");
    order.setUserId(12L);
    when(paymentOrderService.getPaymentOrderByOrderNo("M-1", "S-1")).thenReturn(order);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    var result =
        controller.getPaymentOrderByOrderNo("M-1", "S-1", authentication("12", "ROLE_USER"));

    assertThat(result.getCode()).isEqualTo(200);
    assertThat(result.getData()).isSameAs(order);
  }

  @Test
  void getPaymentOrderByOrderNoShouldRejectMissingOrder() {
    when(paymentOrderService.getPaymentOrderByOrderNo("M-404", "S-404")).thenReturn(null);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    BizException exception =
        assertThrows(
            BizException.class,
            () ->
                controller.getPaymentOrderByOrderNo(
                    "M-404", "S-404", authentication("12", "ROLE_USER")));

    assertThat(exception.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
    assertThat(exception.getMessage()).contains("payment order not found");
  }

  @Test
  void createCheckoutSessionShouldAllowOwner() {
    PaymentOrderVO order = new PaymentOrderVO();
    order.setPaymentNo("PAY-CHECKOUT");
    order.setUserId(12L);
    PaymentCheckoutSessionVO session = new PaymentCheckoutSessionVO();
    session.setPaymentNo("PAY-CHECKOUT");
    session.setCheckoutPath("/api/payments/checkout/ticket-1");
    session.setExpiresInSeconds(300L);
    when(paymentOrderService.getPaymentOrderByNo("PAY-CHECKOUT")).thenReturn(order);
    when(paymentOrderService.createCheckoutSession("PAY-CHECKOUT")).thenReturn(session);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    var result =
        controller.createCheckoutSession("PAY-CHECKOUT", authentication("12", "ROLE_USER"));

    assertThat(result.getCode()).isEqualTo(200);
    assertThat(result.getData()).isSameAs(session);
  }

  @Test
  void createCheckoutSessionShouldForbidOtherUser() {
    PaymentOrderVO order = new PaymentOrderVO();
    order.setPaymentNo("PAY-CHECKOUT");
    order.setUserId(18L);
    when(paymentOrderService.getPaymentOrderByNo("PAY-CHECKOUT")).thenReturn(order);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    BizException exception =
        assertThrows(
            BizException.class,
            () ->
                controller.createCheckoutSession(
                    "PAY-CHECKOUT", authentication("12", "ROLE_USER")));

    assertThat(exception.getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode());
  }

  @Test
  void renderCheckoutPageShouldFallbackToHtmlErrorPage() {
    when(paymentOrderService.renderCheckoutPage("expired-ticket"))
        .thenThrow(new com.cloud.common.exception.BusinessException("checkout expired"));

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    String html = controller.renderCheckoutPage("expired-ticket");

    assertThat(html).contains("Payment unavailable");
    assertThat(html).contains("checkout expired");
  }

  @Test
  void handleCallbackShouldUseInternalCallbackHandler() {
    PaymentCallbackCommandDTO command = new PaymentCallbackCommandDTO();
    when(paymentOrderService.handleInternalPaymentCallback(command)).thenReturn(Boolean.TRUE);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    var result = controller.handleCallback(command);

    assertThat(result.getCode()).isEqualTo(200);
    assertThat(result.getData()).isTrue();
    verify(paymentOrderService).handleInternalPaymentCallback(command);
  }

  @Test
  void getRefundByNoShouldAllowOwner() {
    PaymentRefundVO refund = new PaymentRefundVO();
    refund.setRefundNo("RF-1");
    refund.setPaymentNo("PAY-10");
    PaymentOrderVO order = new PaymentOrderVO();
    order.setPaymentNo("PAY-10");
    order.setUserId(12L);
    when(paymentOrderService.getRefundByNo("RF-1")).thenReturn(refund);
    when(paymentOrderService.getPaymentOrderByNo("PAY-10")).thenReturn(order);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    var result = controller.getRefundByNo("RF-1", authentication("12", "ROLE_USER"));

    assertThat(result.getCode()).isEqualTo(200);
    assertThat(result.getData()).isSameAs(refund);
  }

  @Test
  void getRefundByNoShouldForbidOtherUser() {
    PaymentRefundVO refund = new PaymentRefundVO();
    refund.setRefundNo("RF-2");
    refund.setPaymentNo("PAY-20");
    PaymentOrderVO order = new PaymentOrderVO();
    order.setPaymentNo("PAY-20");
    order.setUserId(18L);
    when(paymentOrderService.getRefundByNo("RF-2")).thenReturn(refund);
    when(paymentOrderService.getPaymentOrderByNo("PAY-20")).thenReturn(order);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    BizException exception =
        assertThrows(
            BizException.class,
            () -> controller.getRefundByNo("RF-2", authentication("99", "ROLE_USER")));

    assertThat(exception.getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode());
    assertThat(exception.getMessage()).contains("other user's refund");
  }

  @Test
  void getRefundByNoShouldAllowAdmin() {
    PaymentRefundVO refund = new PaymentRefundVO();
    refund.setRefundNo("RF-3");
    refund.setPaymentNo("PAY-30");
    PaymentOrderVO order = new PaymentOrderVO();
    order.setPaymentNo("PAY-30");
    order.setUserId(18L);
    when(paymentOrderService.getRefundByNo("RF-3")).thenReturn(refund);
    when(paymentOrderService.getPaymentOrderByNo("PAY-30")).thenReturn(order);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    var result = controller.getRefundByNo("RF-3", authentication(null, "ROLE_ADMIN"));

    assertThat(result.getCode()).isEqualTo(200);
    assertThat(result.getData()).isSameAs(refund);
  }

  @Test
  void getRefundByNoShouldRejectMissingRefund() {
    when(paymentOrderService.getRefundByNo("RF-404")).thenReturn(null);

    PaymentOrderController controller =
        new PaymentOrderController(paymentOrderService, paymentSecurityCacheService);
    BizException exception =
        assertThrows(
            BizException.class,
            () -> controller.getRefundByNo("RF-404", authentication("12", "ROLE_USER")));

    assertThat(exception.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
    assertThat(exception.getMessage()).contains("payment refund not found");
  }

  private Authentication authentication(String userId, String... authorities) {
    Jwt.Builder builder =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("subject")
            .claim("username", "tester");
    if (userId != null) {
      builder.claim("user_id", userId);
    }
    return new JwtAuthenticationToken(
        builder.build(),
        Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList(),
        "tester");
  }

  private PaymentOrderCommandDTO buildPaymentCommand() {
    PaymentOrderCommandDTO command = new PaymentOrderCommandDTO();
    command.setPaymentNo("PAY-CREATE");
    command.setMainOrderNo("M-1");
    command.setSubOrderNo("S-1");
    command.setUserId(12L);
    command.setChannel("ALIPAY");
    command.setIdempotencyKey("idem-create");
    command.setAmount(java.math.BigDecimal.TEN);
    return command;
  }
}
