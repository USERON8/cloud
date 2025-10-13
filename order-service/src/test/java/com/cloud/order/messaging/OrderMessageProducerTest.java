package com.cloud.order.messaging;

import com.cloud.common.messaging.event.OrderCreatedEvent;
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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderMessageProducer 消息生产者测试
 * 测试订单创建事件和订单取消事件的发送
 *
 * @author what's up
 */
@SpringBootTest(properties = {
        "spring.cloud.stream.default-binder=test-binder"
})
@Import(TestChannelBinderConfiguration.class)
@ActiveProfiles("test")
@DisplayName("订单消息生产者测试")
class OrderMessageProducerTest {

    @Autowired
    private OrderMessageProducer orderMessageProducer;

    @Autowired
    private OutputDestination outputDestination;

    @Test
    @DisplayName("测试发送订单创建事件")
    void testSendOrderCreatedEvent() {
        // Given - 准备测试数据
        Map<Long, Integer> productQuantityMap = new HashMap<>();
        productQuantityMap.put(1001L, 2);  // 商品ID 1001, 数量 2
        productQuantityMap.put(1002L, 1);  // 商品ID 1002, 数量 1

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(10001L)
                .orderNo("ORD202501150001")
                .userId(2001L)
                .totalAmount(new BigDecimal("299.99"))
                .productQuantityMap(productQuantityMap)
                .remark("测试订单")
                .build();

        // When - 发送消息
        boolean result = orderMessageProducer.sendOrderCreatedEvent(event);

        // Then - 验证结果
        assertThat(result).isTrue();

        // 从测试输出目的地读取消息
        Message<byte[]> receivedMessage = outputDestination.receive(5000, "order-created");
        assertThat(receivedMessage).isNotNull();

        // 验证消息头
        assertThat(receivedMessage.getHeaders())
                .containsKey("eventId")
                .containsKey("eventType");

        System.out.println("✅ 订单创建事件发送成功");
        System.out.println("   订单号: " + event.getOrderNo());
        System.out.println("   订单ID: " + event.getOrderId());
        System.out.println("   金额: " + event.getTotalAmount());
    }

    @Test
    @DisplayName("测试发送订单取消事件")
    void testSendOrderCancelledEvent() {
        // Given - 准备测试数据
        Long orderId = 10002L;
        String orderNo = "ORD202501150002";
        String reason = "用户主动取消";

        // When - 发送消息
        boolean result = orderMessageProducer.sendOrderCancelledEvent(orderId, orderNo, reason);

        // Then - 验证结果
        assertThat(result).isTrue();

        // 从测试输出目的地读取消息
        Message<byte[]> receivedMessage = outputDestination.receive(5000, "order-cancelled");
        assertThat(receivedMessage).isNotNull();

        // 验证消息头
        assertThat(receivedMessage.getHeaders())
                .containsKey("eventId")
                .containsEntry("eventType", "ORDER_CANCELLED");

        System.out.println("✅ 订单取消事件发送成功");
        System.out.println("   订单号: " + orderNo);
        System.out.println("   取消原因: " + reason);
    }

    @Test
    @DisplayName("测试发送空商品列表的订单创建事件")
    void testSendOrderCreatedEventWithEmptyProducts() {
        // Given - 准备测试数据（空商品列表）
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(10003L)
                .orderNo("ORD202501150003")
                .userId(2001L)
                .totalAmount(BigDecimal.ZERO)
                .productQuantityMap(new HashMap<>())
                .build();

        // When - 发送消息
        boolean result = orderMessageProducer.sendOrderCreatedEvent(event);

        // Then - 验证结果（即使商品列表为空，消息也应该发送成功）
        assertThat(result).isTrue();

        // 验证消息已发送
        Message<byte[]> receivedMessage = outputDestination.receive(5000, "order-created");
        assertThat(receivedMessage).isNotNull();

        System.out.println("✅ 空商品列表订单创建事件发送成功");
    }

    @Test
    @DisplayName("测试批量发送订单创建事件")
    void testSendMultipleOrderCreatedEvents() {
        // Given - 准备多个订单
        int orderCount = 5;

        // When - 批量发送
        for (int i = 1; i <= orderCount; i++) {
            Map<Long, Integer> productQuantityMap = new HashMap<>();
            productQuantityMap.put(1000L + i, i);

            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(10000L + i)
                    .orderNo("ORD20250115000" + i)
                    .userId(2001L)
                    .totalAmount(new BigDecimal(99.99 * i))
                    .productQuantityMap(productQuantityMap)
                    .build();

            boolean result = orderMessageProducer.sendOrderCreatedEvent(event);
            assertThat(result).isTrue();
        }

        // Then - 验证所有消息都已发送
        for (int i = 1; i <= orderCount; i++) {
            Message<byte[]> receivedMessage = outputDestination.receive(5000, "order-created");
            assertThat(receivedMessage).isNotNull();
        }

        System.out.println("✅ 批量发送 " + orderCount + " 个订单创建事件成功");
    }
}
