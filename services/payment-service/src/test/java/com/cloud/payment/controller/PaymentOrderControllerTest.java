package com.cloud.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.vo.payment.PaymentOrderVO;
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
    var result = controller.getPaymentOrderByNo("PAY-2", authentication("99", "ROLE_USER"));

    assertThat(result.getCode()).isEqualTo(403);
    assertThat(result.getData()).isNull();
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
}
