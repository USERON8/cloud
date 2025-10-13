package com.cloud.stock.messaging;

import com.cloud.common.messaging.event.OrderCreatedEvent;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.stock.service.StockService;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StockMessageConsumer 消息消费者测试
 * 测试订单创建事件和支付成功事件的消费
 *
 * @author what's up
 */
@SpringBootTest(properties = {
        "spring.cloud.stream.default-binder=test-binder"
})
@Import(TestChannelBinderConfiguration.class)
@ActiveProfiles("test")
@DisplayName("库存消息消费者测试")
class StockMessageConsumerTest {

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StockService stockService;

    @MockBean
    private StockMessageProducer stockMessageProducer;

    @Test
    @DisplayName("测试消费订单创建事件 - 库存充足场景")
    void testConsumeOrderCreatedEvent_SufficientStock() throws Exception {
        // Given - 准备订单创建事件
        Map<Long, Integer> productQuantityMap = new HashMap<>();
        productQuantityMap.put(1001L, 2);  // 商品1001, 数量2
        productQuantityMap.put(1002L, 1);  // 商品1002, 数量1

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(10001L)
                .orderNo("ORD202501150001")
                .userId(2001L)
                .totalAmount(new BigDecimal("299.99"))
                .productQuantityMap(productQuantityMap)
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock库存检查和冻结成功
        when(stockService.isStockFrozen(anyLong())).thenReturn(false);
        when(stockService.checkStockSufficient(anyLong(), anyInt())).thenReturn(true);
        when(stockService.reserveStock(anyLong(), anyInt())).thenReturn(true);

        // When - 发送消息到输入目的地
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "order-created");

        // Then - 等待消息处理
        TimeUnit.MILLISECONDS.sleep(1000);

        // 验证库存服务方法被调用
        verify(stockService, times(1)).isStockFrozen(eq(event.getOrderId()));
        verify(stockService, times(2)).checkStockSufficient(anyLong(), anyInt());
        verify(stockService, times(2)).reserveStock(anyLong(), anyInt());

        // 验证没有发送失败事件
        verify(stockMessageProducer, never()).sendStockFreezeFailedEvent(anyLong(), anyString(), anyString());

        System.out.println("✅ 订单创建事件消费成功 - 库存充足");
        System.out.println("   订单号: " + event.getOrderNo());
        System.out.println("   商品数量: " + productQuantityMap.size());
    }

    @Test
    @DisplayName("测试消费订单创建事件 - 库存不足场景")
    void testConsumeOrderCreatedEvent_InsufficientStock() throws Exception {
        // Given - 准备订单创建事件
        Map<Long, Integer> productQuantityMap = new HashMap<>();
        productQuantityMap.put(1001L, 10);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(10002L)
                .orderNo("ORD202501150002")
                .userId(2001L)
                .totalAmount(new BigDecimal("999.99"))
                .productQuantityMap(productQuantityMap)
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock库存不足
        when(stockService.isStockFrozen(anyLong())).thenReturn(false);
        when(stockService.checkStockSufficient(eq(1001L), eq(10))).thenReturn(false);
        when(stockMessageProducer.sendStockFreezeFailedEvent(anyLong(), anyString(), anyString())).thenReturn(true);

        // When - 发送消息
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "order-created");

        // Then - 等待消息处理
        TimeUnit.MILLISECONDS.sleep(1000);

        // 验证库存检查被调用
        verify(stockService, times(1)).checkStockSufficient(eq(1001L), eq(10));

        // 验证库存预留未被调用（因为库存不足）
        verify(stockService, never()).reserveStock(anyLong(), anyInt());

        // 验证发送了失败事件
        verify(stockMessageProducer, times(1))
                .sendStockFreezeFailedEvent(
                        eq(event.getOrderId()),
                        eq(event.getOrderNo()),
                        contains("库存不足")
                );

        System.out.println("✅ 订单创建事件消费成功 - 库存不足");
        System.out.println("   订单号: " + event.getOrderNo());
        System.out.println("   已发送库存冻结失败事件");
    }

    @Test
    @DisplayName("测试消费订单创建事件 - 已冻结库存场景")
    void testConsumeOrderCreatedEvent_AlreadyFrozen() throws Exception {
        // Given - 准备订单创建事件
        Map<Long, Integer> productQuantityMap = new HashMap<>();
        productQuantityMap.put(1001L, 2);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(10003L)
                .orderNo("ORD202501150003")
                .userId(2001L)
                .totalAmount(new BigDecimal("199.99"))
                .productQuantityMap(productQuantityMap)
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock订单已冻结库存
        when(stockService.isStockFrozen(eq(event.getOrderId()))).thenReturn(true);

        // When - 发送消息
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "order-created");

        // Then - 等待消息处理
        TimeUnit.MILLISECONDS.sleep(1000);

        // 验证只检查了是否已冻结，其他操作未执行
        verify(stockService, times(1)).isStockFrozen(eq(event.getOrderId()));
        verify(stockService, never()).checkStockSufficient(anyLong(), anyInt());
        verify(stockService, never()).reserveStock(anyLong(), anyInt());

        System.out.println("✅ 订单创建事件消费成功 - 幂等性检查");
        System.out.println("   订单号: " + event.getOrderNo());
        System.out.println("   库存已冻结，跳过处理");
    }

    @Test
    @DisplayName("测试消费支付成功事件")
    void testConsumePaymentSuccessEvent() throws Exception {
        // Given - 准备支付成功事件
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .paymentId(5001L)
                .orderId(10004L)
                .orderNo("ORD202501150004")
                .userId(2001L)
                .amount(new BigDecimal("299.99"))
                .paymentMethod("ALIPAY")
                .transactionNo("TXN" + System.currentTimeMillis())
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock库存未扣减
        when(stockService.isStockDeducted(anyLong())).thenReturn(false);

        // When - 发送消息
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "payment-success");

        // Then - 等待消息处理
        TimeUnit.MILLISECONDS.sleep(1000);

        // 验证库存扣减检查被调用
        verify(stockService, times(1)).isStockDeducted(eq(event.getOrderId()));

        System.out.println("✅ 支付成功事件消费成功");
        System.out.println("   订单号: " + event.getOrderNo());
        System.out.println("   支付金额: " + event.getAmount());
        System.out.println("   ⚠️  注意: 库存扣减功能待完善（需要获取商品列表）");
    }

    @Test
    @DisplayName("测试消费支付成功事件 - 已扣减库存场景")
    void testConsumePaymentSuccessEvent_AlreadyDeducted() throws Exception {
        // Given - 准备支付成功事件
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .paymentId(5002L)
                .orderId(10005L)
                .orderNo("ORD202501150005")
                .userId(2001L)
                .amount(new BigDecimal("199.99"))
                .paymentMethod("WECHAT")
                .transactionNo("TXN" + System.currentTimeMillis())
                .timestamp(System.currentTimeMillis())
                .eventId(UUID.randomUUID().toString())
                .build();

        // Mock库存已扣减
        when(stockService.isStockDeducted(eq(event.getOrderId()))).thenReturn(true);

        // When - 发送消息
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message<byte[]> message = MessageBuilder.withPayload(payload)
                .setHeader("contentType", "application/json")
                .build();

        inputDestination.send(message, "payment-success");

        // Then - 等待消息处理
        TimeUnit.MILLISECONDS.sleep(1000);

        // 验证幂等性检查
        verify(stockService, times(1)).isStockDeducted(eq(event.getOrderId()));

        System.out.println("✅ 支付成功事件消费成功 - 幂等性检查");
        System.out.println("   订单号: " + event.getOrderNo());
        System.out.println("   库存已扣减，跳过处理");
    }

    @Test
    @DisplayName("测试批量消费订单创建事件")
    void testConsumeMultipleOrderCreatedEvents() throws Exception {
        // Given - 准备多个事件
        int eventCount = 3;

        // Mock库存服务
        when(stockService.isStockFrozen(anyLong())).thenReturn(false);
        when(stockService.checkStockSufficient(anyLong(), anyInt())).thenReturn(true);
        when(stockService.reserveStock(anyLong(), anyInt())).thenReturn(true);

        // When - 发送多个消息
        for (int i = 1; i <= eventCount; i++) {
            Map<Long, Integer> productQuantityMap = new HashMap<>();
            productQuantityMap.put(1000L + i, i);

            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(10000L + i)
                    .orderNo("ORD20250115000" + i)
                    .userId(2001L)
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
        TimeUnit.SECONDS.sleep(2);

        // 验证库存服务方法被调用了eventCount次
        verify(stockService, times(eventCount)).isStockFrozen(anyLong());
        verify(stockService, times(eventCount)).checkStockSufficient(anyLong(), anyInt());
        verify(stockService, times(eventCount)).reserveStock(anyLong(), anyInt());

        System.out.println("✅ 批量消费 " + eventCount + " 个订单创建事件成功");
    }
}
