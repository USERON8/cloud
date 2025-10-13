package com.cloud.stock.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StockMessageProducer 消息生产者测试
 * 测试库存冻结失败事件的发送
 *
 * @author what's up
 */
@SpringBootTest(properties = {
        "spring.cloud.stream.default-binder=test-binder"
})
@Import(TestChannelBinderConfiguration.class)
@ActiveProfiles("test")
@DisplayName("库存消息生产者测试")
class StockMessageProducerTest {

    @Autowired
    private StockMessageProducer stockMessageProducer;

    @Autowired
    private OutputDestination outputDestination;

    @Test
    @DisplayName("测试发送库存冻结失败事件")
    void testSendStockFreezeFailedEvent() {
        // Given - 准备测试数据
        Long orderId = 10001L;
        String orderNo = "ORD202501150001";
        String reason = "商品库存不足";

        // When - 发送消息
        boolean result = stockMessageProducer.sendStockFreezeFailedEvent(orderId, orderNo, reason);

        // Then - 验证结果
        assertThat(result).isTrue();

        // 从测试输出目的地读取消息
        Message<byte[]> receivedMessage = outputDestination.receive(5000, "stock-freeze-failed");
        assertThat(receivedMessage).isNotNull();

        // 验证消息头
        assertThat(receivedMessage.getHeaders())
                .containsKey("eventId")
                .containsKey("eventType")
                .containsEntry("eventType", "STOCK_FREEZE_FAILED");

        System.out.println("✅ 库存冻结失败事件发送成功");
        System.out.println("   订单号: " + orderNo);
        System.out.println("   失败原因: " + reason);
    }

    @Test
    @DisplayName("测试发送库存冻结失败事件 - 库存不足")
    void testSendStockFreezeFailedEvent_InsufficientStock() {
        // Given
        Long orderId = 10002L;
        String orderNo = "ORD202501150002";
        String reason = "商品 1001 库存不足，需要 10，可用库存 5";

        // When
        boolean result = stockMessageProducer.sendStockFreezeFailedEvent(orderId, orderNo, reason);

        // Then
        assertThat(result).isTrue();

        Message<byte[]> receivedMessage = outputDestination.receive(5000, "stock-freeze-failed");
        assertThat(receivedMessage).isNotNull();

        System.out.println("✅ 库存不足事件发送成功");
        System.out.println("   订单号: " + orderNo);
        System.out.println("   详细原因: " + reason);
    }

    @Test
    @DisplayName("测试发送库存冻结失败事件 - 系统异常")
    void testSendStockFreezeFailedEvent_SystemError() {
        // Given
        Long orderId = 10003L;
        String orderNo = "ORD202501150003";
        String reason = "系统异常: 数据库连接超时";

        // When
        boolean result = stockMessageProducer.sendStockFreezeFailedEvent(orderId, orderNo, reason);

        // Then
        assertThat(result).isTrue();

        Message<byte[]> receivedMessage = outputDestination.receive(5000, "stock-freeze-failed");
        assertThat(receivedMessage).isNotNull();

        System.out.println("✅ 系统异常事件发送成功");
        System.out.println("   订单号: " + orderNo);
        System.out.println("   异常原因: " + reason);
    }

    @Test
    @DisplayName("测试批量发送库存冻结失败事件")
    void testSendMultipleStockFreezeFailedEvents() {
        // Given - 准备多个事件
        int eventCount = 5;

        // When - 批量发送
        for (int i = 1; i <= eventCount; i++) {
            Long orderId = 10000L + i;
            String orderNo = "ORD20250115000" + i;
            String reason = "商品 " + (1000 + i) + " 库存不足";

            boolean result = stockMessageProducer.sendStockFreezeFailedEvent(orderId, orderNo, reason);
            assertThat(result).isTrue();
        }

        // Then - 验证所有消息都已发送
        for (int i = 1; i <= eventCount; i++) {
            Message<byte[]> receivedMessage = outputDestination.receive(5000, "stock-freeze-failed");
            assertThat(receivedMessage).isNotNull();
        }

        System.out.println("✅ 批量发送 " + eventCount + " 个库存冻结失败事件成功");
    }

    @Test
    @DisplayName("测试发送长文本原因的库存冻结失败事件")
    void testSendStockFreezeFailedEvent_LongReason() {
        // Given - 准备长文本原因
        Long orderId = 10004L;
        String orderNo = "ORD202501150004";
        String reason = "库存冻结失败：商品 1001 需要 100 件，当前可用库存 50 件，" +
                "预留库存 30 件，总库存 80 件。建议拆分订单或联系供应商补货。" +
                "失败时间: " + System.currentTimeMillis();

        // When
        boolean result = stockMessageProducer.sendStockFreezeFailedEvent(orderId, orderNo, reason);

        // Then
        assertThat(result).isTrue();

        Message<byte[]> receivedMessage = outputDestination.receive(5000, "stock-freeze-failed");
        assertThat(receivedMessage).isNotNull();

        System.out.println("✅ 长文本原因事件发送成功");
        System.out.println("   订单号: " + orderNo);
        System.out.println("   原因长度: " + reason.length() + " 字符");
    }
}
