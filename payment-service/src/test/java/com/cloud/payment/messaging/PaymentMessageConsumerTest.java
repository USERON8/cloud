package com.cloud.payment.messaging;

import com.cloud.common.messaging.event.OrderCreatedEvent;
import com.cloud.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PaymentMessageConsumer 消息消费者测试
 * 测试订单创建事件的消费和支付流程
 *
 * @author what's up
 */
@SpringBootTest(properties = {
        "spring.cloud.stream.default-binder=test-binder"
})
@Import(TestChannelBinderConfiguration.class)
@ActiveProfiles("test")
@DisplayName("支付消息消费者测试")
class PaymentMessageConsumerTest {

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private PaymentMessageProducer paymentMessageProducer;

    @Test
    @DisplayName("测试消费订单创建事件 - 完整支付流程")
    void testConsumeOrderCreatedEvent_CompleteFlow() throws Exception {
        // Given - 准备订单创建事件
        Map<Long, Integer> productQuantityMap = new HashMap<>();
        productQuantityMap.put(1001L, 2);
        productQuantityMap.put(1002L, 1);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(10001L)
                .orderNo("ORD202501150001")
                .userId(2001L)
                .totalAmount(new BigDecimal("299.99"))
                .productQuantityMap(productQuantityMap)
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock支付服务
        when(paymentService.isPaymentRecordExists(anyLong())).thenReturn(false);
        when(paymentService.createPayment(any())).thenReturn(5001L);
        when(paymentService.processPaymentSuccess(anyLong())).thenReturn(true);
        when(paymentMessageProducer.sendPaymentSuccessEvent(
                anyLong(), anyLong(), anyString(), anyLong(),
                any(BigDecimal.class), anyString(), anyString()
        )).thenReturn(true);

        // When - 发送消息到输入目的地
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "order-created");

        // Then - 等待消息处理
        TimeUnit.MILLISECONDS.sleep(1500);

        // 验证支付记录存在性检查被调用
        verify(paymentService, times(1)).isPaymentRecordExists(eq(event.getOrderId()));

        // 验证创建支付被调用
        verify(paymentService, times(1)).createPayment(argThat(dto ->
                dto.getOrderId().equals(event.getOrderId()) &&
                        dto.getOrderNo().equals(event.getOrderNo()) &&
                        dto.getUserId().equals(event.getUserId()) &&
                        dto.getAmount().equals(event.getTotalAmount())
        ));

        // 验证处理支付成功被调用
        verify(paymentService, times(1)).processPaymentSuccess(eq(5001L));

        // 验证发送支付成功事件被调用
        verify(paymentMessageProducer, times(1)).sendPaymentSuccessEvent(
                eq(5001L),
                eq(event.getOrderId()),
                eq(event.getOrderNo()),
                eq(event.getUserId()),
                eq(event.getTotalAmount()),
                eq("ALIPAY"),
                anyString()
        );

        System.out.println("✅ 订单创建事件消费成功 - 完整支付流程");
        System.out.println("   订单号: " + event.getOrderNo());
        System.out.println("   支付金额: " + event.getTotalAmount());
        System.out.println("   流程: 创建支付 → 完成支付 → 发送成功事件");
    }

    @Test
    @DisplayName("测试消费订单创建事件 - 支付记录已存在")
    void testConsumeOrderCreatedEvent_PaymentExists() throws Exception {
        // Given - 准备订单创建事件
        Map<Long, Integer> productQuantityMap = new HashMap<>();
        productQuantityMap.put(1001L, 1);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(10002L)
                .orderNo("ORD202501150002")
                .userId(2002L)
                .totalAmount(new BigDecimal("199.99"))
                .productQuantityMap(productQuantityMap)
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock支付记录已存在
        when(paymentService.isPaymentRecordExists(eq(event.getOrderId()))).thenReturn(true);

        // When - 发送消息
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "order-created");

        // Then - 等待消息处理
        TimeUnit.MILLISECONDS.sleep(1000);

        // 验证只检查了支付记录存在性，其他操作未执行
        verify(paymentService, times(1)).isPaymentRecordExists(eq(event.getOrderId()));
        verify(paymentService, never()).createPayment(any());
        verify(paymentService, never()).processPaymentSuccess(anyLong());
        verify(paymentMessageProducer, never()).sendPaymentSuccessEvent(
                anyLong(), anyLong(), anyString(), anyLong(),
                any(BigDecimal.class), anyString(), anyString()
        );

        System.out.println("✅ 订单创建事件消费成功 - 幂等性检查");
        System.out.println("   订单号: " + event.getOrderNo());
        System.out.println("   支付记录已存在，跳过处理");
    }

    @Test
    @DisplayName("测试消费订单创建事件 - 创建支付失败")
    void testConsumeOrderCreatedEvent_CreatePaymentFailed() throws Exception {
        // Given - 准备订单创建事件
        Map<Long, Integer> productQuantityMap = new HashMap<>();
        productQuantityMap.put(1001L, 1);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(10003L)
                .orderNo("ORD202501150003")
                .userId(2003L)
                .totalAmount(new BigDecimal("99.99"))
                .productQuantityMap(productQuantityMap)
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock创建支付失败（返回null）
        when(paymentService.isPaymentRecordExists(anyLong())).thenReturn(false);
        when(paymentService.createPayment(any())).thenReturn(null);

        // When - 发送消息
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "order-created");

        // Then - 等待消息处理
        TimeUnit.MILLISECONDS.sleep(1000);

        // 验证创建支付被调用
        verify(paymentService, times(1)).createPayment(any());

        // 验证后续操作未执行
        verify(paymentService, never()).processPaymentSuccess(anyLong());
        verify(paymentMessageProducer, never()).sendPaymentSuccessEvent(
                anyLong(), anyLong(), anyString(), anyLong(),
                any(BigDecimal.class), anyString(), anyString()
        );

        System.out.println("✅ 订单创建事件消费 - 创建支付失败场景");
        System.out.println("   订单号: " + event.getOrderNo());
        System.out.println("   创建支付失败，流程中断");
    }

    @Test
    @DisplayName("测试消费订单创建事件 - 处理支付成功失败")
    void testConsumeOrderCreatedEvent_ProcessPaymentFailed() throws Exception {
        // Given - 准备订单创建事件
        Map<Long, Integer> productQuantityMap = new HashMap<>();
        productQuantityMap.put(1001L, 1);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(10004L)
                .orderNo("ORD202501150004")
                .userId(2004L)
                .totalAmount(new BigDecimal("149.99"))
                .productQuantityMap(productQuantityMap)
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock处理支付成功失败
        when(paymentService.isPaymentRecordExists(anyLong())).thenReturn(false);
        when(paymentService.createPayment(any())).thenReturn(5002L);
        when(paymentService.processPaymentSuccess(anyLong())).thenReturn(false);

        // When - 发送消息
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "order-created");

        // Then - 等待消息处理
        TimeUnit.MILLISECONDS.sleep(1000);

        // 验证创建支付成功
        verify(paymentService, times(1)).createPayment(any());

        // 验证处理支付成功被调用但返回false
        verify(paymentService, times(1)).processPaymentSuccess(eq(5002L));

        // 验证支付成功事件未发送
        verify(paymentMessageProducer, never()).sendPaymentSuccessEvent(
                anyLong(), anyLong(), anyString(), anyLong(),
                any(BigDecimal.class), anyString(), anyString()
        );

        System.out.println("✅ 订单创建事件消费 - 处理支付成功失败场景");
        System.out.println("   订单号: " + event.getOrderNo());
        System.out.println("   支付创建成功，但处理失败");
    }

    @Test
    @DisplayName("测试消费订单创建事件 - 小额订单")
    void testConsumeOrderCreatedEvent_SmallAmount() throws Exception {
        // Given - 小额订单（0.01元）
        Map<Long, Integer> productQuantityMap = new HashMap<>();
        productQuantityMap.put(1001L, 1);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(10005L)
                .orderNo("ORD202501150005")
                .userId(2005L)
                .totalAmount(new BigDecimal("0.01"))
                .productQuantityMap(productQuantityMap)
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock支付服务
        when(paymentService.isPaymentRecordExists(anyLong())).thenReturn(false);
        when(paymentService.createPayment(any())).thenReturn(5003L);
        when(paymentService.processPaymentSuccess(anyLong())).thenReturn(true);
        when(paymentMessageProducer.sendPaymentSuccessEvent(
                anyLong(), anyLong(), anyString(), anyLong(),
                any(BigDecimal.class), anyString(), anyString()
        )).thenReturn(true);

        // When - 发送消息
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "order-created");

        // Then - 等待消息处理
        TimeUnit.MILLISECONDS.sleep(1500);

        // 验证完整流程
        verify(paymentService, times(1)).createPayment(any());
        verify(paymentService, times(1)).processPaymentSuccess(anyLong());
        verify(paymentMessageProducer, times(1)).sendPaymentSuccessEvent(
                anyLong(), anyLong(), anyString(), anyLong(),
                eq(new BigDecimal("0.01")), anyString(), anyString()
        );

        System.out.println("✅ 小额订单支付流程测试成功");
        System.out.println("   订单号: " + event.getOrderNo());
        System.out.println("   支付金额: " + event.getTotalAmount() + " 元");
    }

    @Test
    @DisplayName("测试批量消费订单创建事件")
    void testConsumeMultipleOrderCreatedEvents() throws Exception {
        // Given - 准备多个订单
        int eventCount = 3;

        // Mock支付服务
        when(paymentService.isPaymentRecordExists(anyLong())).thenReturn(false);
        when(paymentService.createPayment(any())).thenAnswer(invocation -> {
            // 根据订单ID生成不同的支付ID
            return 5000L + invocation.hashCode() % 1000;
        });
        when(paymentService.processPaymentSuccess(anyLong())).thenReturn(true);
        when(paymentMessageProducer.sendPaymentSuccessEvent(
                anyLong(), anyLong(), anyString(), anyLong(),
                any(BigDecimal.class), anyString(), anyString()
        )).thenReturn(true);

        // When - 发送多个消息
        for (int i = 1; i <= eventCount; i++) {
            Map<Long, Integer> productQuantityMap = new HashMap<>();
            productQuantityMap.put(1000L + i, i);

            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(10000L + i)
                    .orderNo("ORD20250115000" + i)
                    .userId(2000L + i)
                    .totalAmount(new BigDecimal(99.99 * i))
                    .productQuantityMap(productQuantityMap)
                    .timestamp(System.currentTimeMillis())
                    .eventId(UUID.randomUUID().toString())
                    .build();

            byte[] payload = objectMapper.writeValueAsBytes(event);
            Message<byte[]> message = MessageBuilder.withPayload(payload)
                    .setHeader("contentType", "application/json")
                    .build();

            inputDestination.send(message, "order-created");
        }

        // Then - 等待所有消息处理完成
        TimeUnit.SECONDS.sleep(3);

        // 验证支付服务方法被调用了eventCount次
        verify(paymentService, times(eventCount)).isPaymentRecordExists(anyLong());
        verify(paymentService, times(eventCount)).createPayment(any());
        verify(paymentService, times(eventCount)).processPaymentSuccess(anyLong());
        verify(paymentMessageProducer, times(eventCount)).sendPaymentSuccessEvent(
                anyLong(), anyLong(), anyString(), anyLong(),
                any(BigDecimal.class), anyString(), anyString()
        );

        System.out.println("✅ 批量消费 " + eventCount + " 个订单创建事件成功");
        System.out.println("   每个订单都完成了完整的支付流程");
    }
}
