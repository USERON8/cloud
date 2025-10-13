package com.cloud.order.messaging;

import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.messaging.event.StockFreezeFailedEvent;
import com.cloud.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderMessageConsumer 消息消费者测试
 * 测试支付成功事件和库存冻结失败事件的消费
 *
 * @author what's up
 */
@SpringBootTest(properties = {
        "spring.cloud.stream.default-binder=test-binder"
})
@Import(TestChannelBinderConfiguration.class)
@ActiveProfiles("test")
@DisplayName("订单消息消费者测试")
class OrderMessageConsumerTest {

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("测试消费支付成功事件")
    void testConsumePaymentSuccessEvent() throws Exception {
        // Given - 准备支付成功事件
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .paymentId(5001L)
                .orderId(10001L)
                .orderNo("ORD202501150001")
                .userId(2001L)
                .amount(new BigDecimal("299.99"))
                .paymentMethod("ALIPAY")
                .transactionNo("TXN" + System.currentTimeMillis())
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock订单服务返回成功
        when(orderService.updateOrderStatusAfterPayment(anyLong(), anyLong(), anyString()))
                .thenReturn(true);

        // When - 发送消息到输入目的地
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "payment-success");

        // Then - 等待消息处理（异步）
        TimeUnit.MILLISECONDS.sleep(1000);

        // 验证订单服务方法被调用
        verify(orderService, times(1))
                .updateOrderStatusAfterPayment(
                        eq(event.getOrderId()),
                        eq(event.getPaymentId()),
                        eq(event.getTransactionNo())
                );

        System.out.println("✅ 支付成功事件消费成功");
        System.out.println("   订单号: " + event.getOrderNo());
        System.out.println("   支付金额: " + event.getAmount());
        System.out.println("   支付方式: " + event.getPaymentMethod());
    }

    @Test
    @DisplayName("测试消费库存冻结失败事件")
    void testConsumeStockFreezeFailedEvent() throws Exception {
        // Given - 准备库存冻结失败事件
        StockFreezeFailedEvent event = StockFreezeFailedEvent.builder()
                .orderId(10002L)
                .orderNo("ORD202501150002")
                .reason("商品库存不足")
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock订单服务返回成功
        when(orderService.cancelOrderDueToStockFreezeFailed(anyLong(), anyString()))
                .thenReturn(true);

        // When - 发送消息到输入目的地
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "stock-freeze-failed");

        // Then - 等待消息处理（异步）
        TimeUnit.MILLISECONDS.sleep(1000);

        // 验证订单服务方法被调用
        verify(orderService, times(1))
                .cancelOrderDueToStockFreezeFailed(
                        eq(event.getOrderId()),
                        eq(event.getReason())
                );

        System.out.println("✅ 库存冻结失败事件消费成功");
        System.out.println("   订单号: " + event.getOrderNo());
        System.out.println("   失败原因: " + event.getReason());
    }

    @Test
    @DisplayName("测试支付成功事件处理失败场景")
    void testConsumePaymentSuccessEvent_Failure() throws Exception {
        // Given - 准备支付成功事件
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .paymentId(5002L)
                .orderId(10003L)
                .orderNo("ORD202501150003")
                .userId(2001L)
                .amount(new BigDecimal("199.99"))
                .paymentMethod("WECHAT")
                .transactionNo("TXN" + System.currentTimeMillis())
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock订单服务返回失败
        when(orderService.updateOrderStatusAfterPayment(anyLong(), anyLong(), anyString()))
                .thenReturn(false);

        // When - 发送消息到输入目的地
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "payment-success");

        // Then - 等待消息处理（异步）
        TimeUnit.MILLISECONDS.sleep(1000);

        // 验证订单服务方法被调用（即使失败也应该调用）
        verify(orderService, times(1))
                .updateOrderStatusAfterPayment(anyLong(), anyLong(), anyString());

        System.out.println("✅ 支付成功事件处理失败场景测试完成");
        System.out.println("   订单状态更新失败，但消息已消费");
    }

    @Test
    @DisplayName("测试批量消费支付成功事件")
    void testConsumMultiplePaymentSuccessEvents() throws Exception {
        // Given - 准备多个支付成功事件
        int eventCount = 3;

        // Mock订单服务返回成功
        when(orderService.updateOrderStatusAfterPayment(anyLong(), anyLong(), anyString()))
                .thenReturn(true);

        // When - 发送多个消息
        for (int i = 1; i <= eventCount; i++) {
            PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                    .paymentId(5000L + i)
                    .orderId(10000L + i)
                    .orderNo("ORD20250115000" + i)
                    .userId(2001L)
                    .amount(new BigDecimal(99.99 * i))
                    .paymentMethod("ALIPAY")
                    .transactionNo("TXN" + System.currentTimeMillis() + i)
                    .timestamp(System.currentTimeMillis())
                    .eventId(UUID.randomUUID().toString())
                    .build();

            byte[] payload = objectMapper.writeValueAsBytes(event);
            Message<byte[]> message = MessageBuilder.withPayload(payload)
                    .setHeader("contentType", "application/json")
                    .build();

            inputDestination.send(message, "payment-success");
        }

        // Then - 等待所有消息处理完成
        TimeUnit.SECONDS.sleep(2);

        // 验证订单服务方法被调用了eventCount次
        verify(orderService, times(eventCount))
                .updateOrderStatusAfterPayment(anyLong(), anyLong(), anyString());

        System.out.println("✅ 批量消费 " + eventCount + " 个支付成功事件成功");
    }

    @Test
    @DisplayName("测试消费包含空值的支付成功事件")
    void testConsumePaymentSuccessEventWithNullValues() throws Exception {
        // Given - 准备包含空值的事件
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .paymentId(5003L)
                .orderId(10004L)
                .orderNo("ORD202501150004")
                .userId(2001L)
                .amount(new BigDecimal("99.99"))
                .paymentMethod(null)  // 空支付方式
                .transactionNo(null)  // 空流水号
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock订单服务返回成功
        when(orderService.updateOrderStatusAfterPayment(anyLong(), anyLong(), isNull()))
                .thenReturn(true);

        // When - 发送消息
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "payment-success");

        // Then - 等待消息处理
        TimeUnit.MILLISECONDS.sleep(1000);

        // 验证订单服务方法被调用
        verify(orderService, times(1))
                .updateOrderStatusAfterPayment(eq(event.getOrderId()), eq(event.getPaymentId()), isNull());

        System.out.println("✅ 包含空值的支付成功事件消费成功");
    }
}
