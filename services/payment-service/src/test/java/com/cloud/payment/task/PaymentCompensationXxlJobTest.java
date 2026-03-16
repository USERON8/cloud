package com.cloud.payment.task;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.payment.service.PaymentCompensationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentCompensationXxlJobTest {

  @Mock private PaymentCompensationService paymentCompensationService;

  @InjectMocks private PaymentCompensationXxlJob paymentCompensationXxlJob;

  @Test
  void reconcilePendingOrdersShouldDelegate() {
    when(paymentCompensationService.reconcilePendingOrders()).thenReturn(3);

    paymentCompensationXxlJob.reconcilePendingOrders();

    verify(paymentCompensationService).reconcilePendingOrders();
  }

  @Test
  void retryPendingRefundsShouldDelegate() {
    when(paymentCompensationService.retryPendingRefunds()).thenReturn(2);

    paymentCompensationXxlJob.retryPendingRefunds();

    verify(paymentCompensationService).retryPendingRefunds();
  }
}
