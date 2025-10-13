package com.cloud.payment.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PaymentMessageProducer 消息生产者测试
 * 测试支付成功事件的发送
 *
 * @author what's up
 */
@SpringBootTest(properties = {
        "spring.cloud.stream.default-binder=test-binder"
})
@Import(TestChannelBinderConfiguration.class)
@ActiveProfiles("test")
@DisplayName("支付消息生产者测试")
class PaymentMessageProducerTest {

    @Autowired
    private PaymentMessageProducer paymentMessageProducer;

    @Autowired
    private OutputDestination outputDestination;

    @Test
    @DisplayName("测试发送支付成功事件 - 支付宝支付")
    void testSendPaymentSuccessEvent_Alipay() {
        // Given - 准备测试数据
        Long paymentId = 5001L;
        Long orderId = 10001L;
        String orderNo = "ORD202501150001";
        Long userId = 2001L;
        BigDecimal amount = new BigDecimal("299.99");
        String paymentMethod = "ALIPAY";
        String transactionNo = "TXN" + System.currentTimeMillis();

        // When - 发送消息
        boolean result = paymentMessageProducer.sendPaymentSuccessEvent(
                paymentId, orderId, orderNo, userId, amount, paymentMethod, transactionNo
        );

        // Then - 验证结果
        assertThat(result).isTrue();

        // 从测试输出目的地读取消息
        Message<byte[]> receivedMessage = outputDestination.receive(5000, "payment-success");
        assertThat(receivedMessage).isNotNull();

        // 验证消息头
        assertThat(receivedMessage.getHeaders())
                .containsKey("eventId")
                .containsKey("eventType")
                .containsEntry("eventType", "PAYMENT_SUCCESS");

        System.out.println("✅ 支付成功事件发送成功 - 支付宝");
        System.out.println("   订单号: " + orderNo);
        System.out.println("   支付金额: " + amount);
        System.out.println("   支付方式: " + paymentMethod);
        System.out.println("   交易流水号: " + transactionNo);
    }

    @Test
    @DisplayName("测试发送支付成功事件 - 微信支付")
    void testSendPaymentSuccessEvent_Wechat() {
        // Given
        Long paymentId = 5002L;
        Long orderId = 10002L;
        String orderNo = "ORD202501150002";
        Long userId = 2002L;
        BigDecimal amount = new BigDecimal("199.99");
        String paymentMethod = "WECHAT";
        String transactionNo = "WX" + System.currentTimeMillis();

        // When
        boolean result = paymentMessageProducer.sendPaymentSuccessEvent(
                paymentId, orderId, orderNo, userId, amount, paymentMethod, transactionNo
        );

        // Then
        assertThat(result).isTrue();

        Message<byte[]> receivedMessage = outputDestination.receive(5000, "payment-success");
        assertThat(receivedMessage).isNotNull();

        System.out.println("✅ 支付成功事件发送成功 - 微信支付");
        System.out.println("   订单号: " + orderNo);
        System.out.println("   支付金额: " + amount);
        System.out.println("   支付方式: " + paymentMethod);
    }

    @Test
    @DisplayName("测试发送支付成功事件 - 银联支付")
    void testSendPaymentSuccessEvent_UnionPay() {
        // Given
        Long paymentId = 5003L;
        Long orderId = 10003L;
        String orderNo = "ORD202501150003";
        Long userId = 2003L;
        BigDecimal amount = new BigDecimal("999.99");
        String paymentMethod = "UNIONPAY";
        String transactionNo = "UP" + System.currentTimeMillis();

        // When
        boolean result = paymentMessageProducer.sendPaymentSuccessEvent(
                paymentId, orderId, orderNo, userId, amount, paymentMethod, transactionNo
        );

        // Then
        assertThat(result).isTrue();

        Message<byte[]> receivedMessage = outputDestination.receive(5000, "payment-success");
        assertThat(receivedMessage).isNotNull();

        System.out.println("✅ 支付成功事件发送成功 - 银联支付");
        System.out.println("   订单号: " + orderNo);
        System.out.println("   支付金额: " + amount);
    }

    @Test
    @DisplayName("测试发送小额支付成功事件")
    void testSendPaymentSuccessEvent_SmallAmount() {
        // Given - 小额支付（0.01元）
        Long paymentId = 5004L;
        Long orderId = 10004L;
        String orderNo = "ORD202501150004";
        Long userId = 2004L;
        BigDecimal amount = new BigDecimal("0.01");
        String paymentMethod = "ALIPAY";
        String transactionNo = "TXN" + System.currentTimeMillis();

        // When
        boolean result = paymentMessageProducer.sendPaymentSuccessEvent(
                paymentId, orderId, orderNo, userId, amount, paymentMethod, transactionNo
        );

        // Then
        assertThat(result).isTrue();

        Message<byte[]> receivedMessage = outputDestination.receive(5000, "payment-success");
        assertThat(receivedMessage).isNotNull();

        System.out.println("✅ 小额支付成功事件发送成功");
        System.out.println("   订单号: " + orderNo);
        System.out.println("   支付金额: " + amount + " 元");
    }

    @Test
    @DisplayName("测试发送大额支付成功事件")
    void testSendPaymentSuccessEvent_LargeAmount() {
        // Given - 大额支付（99999.99元）
        Long paymentId = 5005L;
        Long orderId = 10005L;
        String orderNo = "ORD202501150005";
        Long userId = 2005L;
        BigDecimal amount = new BigDecimal("99999.99");
        String paymentMethod = "ALIPAY";
        String transactionNo = "TXN" + System.currentTimeMillis();

        // When
        boolean result = paymentMessageProducer.sendPaymentSuccessEvent(
                paymentId, orderId, orderNo, userId, amount, paymentMethod, transactionNo
        );

        // Then
        assertThat(result).isTrue();

        Message<byte[]> receivedMessage = outputDestination.receive(5000, "payment-success");
        assertThat(receivedMessage).isNotNull();

        System.out.println("✅ 大额支付成功事件发送成功");
        System.out.println("   订单号: " + orderNo);
        System.out.println("   支付金额: " + amount + " 元");
    }

    @Test
    @DisplayName("测试批量发送支付成功事件")
    void testSendMultiplePaymentSuccessEvents() {
        // Given - 准备多个支付事件
        int eventCount = 10;

        // When - 批量发送
        for (int i = 1; i <= eventCount; i++) {
            Long paymentId = 5000L + i;
            Long orderId = 10000L + i;
            String orderNo = "ORD20250115000" + i;
            Long userId = 2000L + i;
            BigDecimal amount = new BigDecimal(99.99 * i);
            String paymentMethod = (i % 2 == 0) ? "ALIPAY" : "WECHAT";
            String transactionNo = "TXN" + System.currentTimeMillis() + i;

            boolean result = paymentMessageProducer.sendPaymentSuccessEvent(
                    paymentId, orderId, orderNo, userId, amount, paymentMethod, transactionNo
            );

            assertThat(result).isTrue();
        }

        // Then - 验证所有消息都已发送
        for (int i = 1; i <= eventCount; i++) {
            Message<byte[]> receivedMessage = outputDestination.receive(5000, "payment-success");
            assertThat(receivedMessage).isNotNull();
        }

        System.out.println("✅ 批量发送 " + eventCount + " 个支付成功事件成功");
    }

    @Test
    @DisplayName("测试发送支付成功事件 - 验证消息头信息")
    void testSendPaymentSuccessEvent_VerifyHeaders() {
        // Given
        Long paymentId = 5006L;
        Long orderId = 10006L;
        String orderNo = "ORD202501150006";
        Long userId = 2006L;
        BigDecimal amount = new BigDecimal("299.99");
        String paymentMethod = "ALIPAY";
        String transactionNo = "TXN" + System.currentTimeMillis();

        // When
        boolean result = paymentMessageProducer.sendPaymentSuccessEvent(
                paymentId, orderId, orderNo, userId, amount, paymentMethod, transactionNo
        );

        // Then
        assertThat(result).isTrue();

        Message<byte[]> receivedMessage = outputDestination.receive(5000, "payment-success");
        assertThat(receivedMessage).isNotNull();

        // 详细验证消息头
        assertThat(receivedMessage.getHeaders())
                .containsKey("eventId")
                .containsKey("eventType")
                .containsKey("contentType");

        // 验证eventId不为空
        String eventId = (String) receivedMessage.getHeaders().get("eventId");
        assertThat(eventId).isNotNull().isNotEmpty();

        // 验证eventType正确
        String eventType = (String) receivedMessage.getHeaders().get("eventType");
        assertThat(eventType).isEqualTo("PAYMENT_SUCCESS");

        System.out.println("✅ 消息头验证成功");
        System.out.println("   eventId: " + eventId);
        System.out.println("   eventType: " + eventType);
    }

    @Test
    @DisplayName("测试发送支付成功事件 - 并发场景")
    void testSendPaymentSuccessEvent_Concurrent() throws InterruptedException {
        // Given - 准备并发测试
        int threadCount = 5;
        int eventsPerThread = 2;

        // When - 并发发送消息
        Thread[] threads = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < eventsPerThread; i++) {
                    Long paymentId = 5000L + threadId * 100 + i;
                    Long orderId = 10000L + threadId * 100 + i;
                    String orderNo = "ORD2025011500" + threadId + i;
                    Long userId = 2000L + threadId;
                    BigDecimal amount = new BigDecimal(99.99 * (i + 1));
                    String paymentMethod = "ALIPAY";
                    String transactionNo = "TXN" + System.currentTimeMillis() + threadId + i;

                    boolean result = paymentMessageProducer.sendPaymentSuccessEvent(
                            paymentId, orderId, orderNo, userId, amount, paymentMethod, transactionNo
                    );

                    assertThat(result).isTrue();
                }
            });
            threads[t].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - 验证所有消息都已发送
        int totalEvents = threadCount * eventsPerThread;
        for (int i = 0; i < totalEvents; i++) {
            Message<byte[]> receivedMessage = outputDestination.receive(5000, "payment-success");
            assertThat(receivedMessage).isNotNull();
        }

        System.out.println("✅ 并发发送支付成功事件测试完成");
        System.out.println("   线程数: " + threadCount);
        System.out.println("   每线程事件数: " + eventsPerThread);
        System.out.println("   总事件数: " + totalEvents);
    }
}
