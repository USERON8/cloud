package com.cloud.log.messaging.processor;

import com.cloud.common.domain.event.stock.StockChangeEvent;
import com.cloud.common.utils.MessageUtils;
import com.cloud.log.domain.document.StockEventDocument;
import com.cloud.log.service.StockEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 库存事件处理器
 * 处理库存变更事件，实现幂等性和数据存储
 *
 * @author cloud
 * @date 2024-01-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventProcessor {

    private final StockEventService stockEventService;

    /**
     * 处理库存变更事件
     *
     * @param event   库存变更事件
     * @param traceId 追踪ID
     * @return 处理是否成功
     */
    public boolean processStockEvent(StockChangeEvent event, String traceId) {
        try {
            // 1. 日志记录
            MessageUtils.logMessageReceive("stock-events", event, traceId);
            log.info("📦 开始处理库存事件 - 商品ID: {}, 变更类型: {}, 追踪ID: {}",
                    event.getProductId(), event.getChangeType(), traceId);

            // 2. 幂等性检查
            if (stockEventService.existsByEventId(traceId)) {
                log.warn("⚠️ 库存事件已处理，跳过 - 追踪ID: {}", traceId);
                return true;
            }

            // 3. 构建ES文档
            StockEventDocument document = buildStockEventDocument(event, traceId);

            // 4. 存储事件
            stockEventService.save(document);

            // 5. 处理成功日志
            MessageUtils.logMessageProcessSuccess(event, traceId);
            log.info("✅ 库存事件处理完成 - 商品ID: {}, 变更类型: {}, 追踪ID: {}",
                    event.getProductId(), event.getChangeType(), traceId);

            return true;

        } catch (Exception e) {
            log.error("❌ 库存事件处理失败 - 商品ID: {}, 变更类型: {}, 追踪ID: {}, 错误: {}",
                    event.getProductId(), event.getChangeType(), traceId, e.getMessage(), e);
            MessageUtils.logMessageProcessError(event, traceId, e.getMessage());
            return false;
        }
    }

    /**
     * 构建库存事件ES文档
     *
     * @param event   库存变更事件
     * @param traceId 追踪ID
     * @return StockEventDocument
     */
    private StockEventDocument buildStockEventDocument(StockChangeEvent event, String traceId) {
        return StockEventDocument.builder()
                .id(traceId) // 使用traceId作为文档ID
                .stockId(event.getStockId())
                .productId(event.getProductId())
                .productName(event.getProductName())
                .eventType("STOCK_CHANGE")
                .traceId(traceId)
                .changeType(event.getChangeType())
                .operatorName(event.getOperator())
                .eventTime(event.getOperateTime())
                .processTime(LocalDateTime.now())
                .build();
    }

    /**
     * IP地址脱敏
     */
    private String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }
        // 保留前两段IP，后两段用*代替
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".***.***.";
        }
        return ip;
    }

    /**
     * UserAgent脱敏
     */
    private String maskUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return userAgent;
        }
        // 只保留前50个字符
        return userAgent.length() > 50 ? userAgent.substring(0, 50) + "..." : userAgent;
    }

    /**
     * 设备ID脱敏
     */
    private String maskDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return deviceId;
        }
        if (deviceId.length() > 8) {
            return deviceId.substring(0, 4) + "****" + deviceId.substring(deviceId.length() - 4);
        }
        return deviceId;
    }
}
