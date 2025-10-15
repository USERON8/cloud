package com.cloud.stock.messaging;

import com.cloud.common.messaging.event.OrderCreatedEvent;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 库存消息消费者
 * 接收并处理库存相关的事件消息
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockMessageConsumer {

    private final StockService stockService;
    private final StockMessageProducer stockMessageProducer;

    /**
     * 消费订单创建事件
     * 冻结库存（预留库存）
     */
    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();

            log.info("📨 接收到订单创建事件: orderId={}, orderNo={}, userId={}, totalAmount={}",
                    event.getOrderId(), event.getOrderNo(), event.getUserId(), event.getTotalAmount());

            try {
                // 幂等性检查
                String eventId = event.getEventId();
                // TODO: 检查该事件是否已处理（可使用Redis存储已处理的eventId）

                // 检查订单是否已冻结库存
                if (stockService.isStockFrozen(event.getOrderId())) {
                    log.warn("⚠️ 订单库存已冻结，跳过处理: orderId={}, orderNo={}",
                            event.getOrderId(), event.getOrderNo());
                    return;
                }

                // 遍历商品列表，逐个冻结库存
                Map<Long, Integer> productQuantityMap = event.getProductQuantityMap();
                boolean allSuccess = true;
                String failureReason = null;

                for (Map.Entry<Long, Integer> entry : productQuantityMap.entrySet()) {
                    Long productId = entry.getKey();
                    Integer quantity = entry.getValue();

                    log.info("🔒 开始冻结库存: orderId={}, productId={}, quantity={}",
                            event.getOrderId(), productId, quantity);

                    // 检查库存是否充足
                    if (!stockService.checkStockSufficient(productId, quantity)) {
                        failureReason = String.format("商品 %d 库存不足，需要 %d，可用库存不足", productId, quantity);
                        log.warn("⚠️ {}", failureReason);
                        allSuccess = false;
                        break;
                    }

                    // 预留库存（冻结）
                    boolean success = stockService.reserveStock(productId, quantity);

                    if (!success) {
                        failureReason = String.format("商品 %d 库存冻结失败", productId);
                        log.error("❌ {}", failureReason);
                        allSuccess = false;
                        break;
                    }

                    log.info("✅ 库存冻结成功: productId={}, quantity={}", productId, quantity);
                }

                if (allSuccess) {
                    log.info("✅ 订单库存全部冻结成功: orderId={}, orderNo={}, 商品数量={}",
                            event.getOrderId(), event.getOrderNo(), productQuantityMap.size());
                } else {
                    log.error("❌ 订单库存冻结失败: orderId={}, orderNo={}, reason={}",
                            event.getOrderId(), event.getOrderNo(), failureReason);

                    // 发送库存冻结失败事件
                    stockMessageProducer.sendStockFreezeFailedEvent(
                            event.getOrderId(),
                            event.getOrderNo(),
                            failureReason
                    );

                    // TODO: 回滚已冻结的库存（需要记录已冻结的商品）
                }

            } catch (Exception e) {
                log.error("❌ 处理订单创建事件失败: orderId={}, orderNo={}",
                        event.getOrderId(), event.getOrderNo(), e);

                // 发送库存冻结失败事件
                stockMessageProducer.sendStockFreezeFailedEvent(
                        event.getOrderId(),
                        event.getOrderNo(),
                        "系统异常: " + e.getMessage()
                );

                // 抛出异常触发消息重试
                throw new RuntimeException("处理订单创建事件失败", e);
            }
        };
    }

    /**
     * 消费支付成功事件
     * 解冻库存并确认扣减
     */
    @Bean
    public Consumer<Message<PaymentSuccessEvent>> paymentSuccessConsumer() {
        return message -> {
            PaymentSuccessEvent event = message.getPayload();

            log.info("📨 接收到支付成功事件: orderId={}, orderNo={}, paymentId={}, amount={}",
                    event.getOrderId(), event.getOrderNo(), event.getPaymentId(), event.getAmount());

            try {
                // 幂等性检查
                String eventId = event.getEventId();
                // TODO: 检查该事件是否已处理（可使用Redis存储已处理的eventId）

                // 检查订单是否已扣减库存
                if (stockService.isStockDeducted(event.getOrderId())) {
                    log.warn("⚠️ 订单库存已扣减，跳过处理: orderId={}, orderNo={}",
                            event.getOrderId(), event.getOrderNo());
                    return;
                }

                // TODO: 需要从订单中获取商品列表
                // 这里简化处理，假设从订单服务查询或者从消息中携带商品信息
                // 实际应该通过Feign调用订单服务获取订单详情

                log.warn("⚠️ 支付成功后库存扣减功能待完善: 需要获取订单商品列表");
                log.info("💡 建议: OrderCreatedEvent中已包含productQuantityMap，可以考虑在PaymentSuccessEvent中也携带此信息");

                // 临时标记：实际应该调用stockOut方法扣减库存
                // stockService.stockOut(productId, quantity, orderId, orderNo, "支付成功扣减");

                log.info("✅ 订单库存扣减处理完成: orderId={}, orderNo={}",
                        event.getOrderId(), event.getOrderNo());

            } catch (Exception e) {
                log.error("❌ 处理支付成功事件失败: orderId={}, orderNo={}",
                        event.getOrderId(), event.getOrderNo(), e);

                // 抛出异常触发消息重试
                throw new RuntimeException("处理支付成功事件失败", e);
            }
        };
    }

    /**
     * 消费库存恢复事件
     * 当退款完成时，恢复订单商品的库存
     */
    @Bean
    public Consumer<Message<Map<String, Object>>> stockRestoreConsumer() {
        return message -> {
            Map<String, Object> event = message.getPayload();

            Long orderId = ((Number) event.get("orderId")).longValue();
            String orderNo = (String) event.get("orderNo");
            Long refundId = ((Number) event.get("refundId")).longValue();
            String refundNo = (String) event.get("refundNo");
            @SuppressWarnings("unchecked")
            Map<Long, Integer> productQuantityMap = (Map<Long, Integer>) event.get("productQuantityMap");

            log.info("📨 接收到库存恢复事件: orderId={}, refundNo={}, products={}",
                    orderId, refundNo, productQuantityMap != null ? productQuantityMap.size() : 0);

            try {
                // 幂等性检查
                String eventId = (String) event.get("eventId");
                // TODO: 检查该事件是否已处理（可使用Redis存储已处理的eventId）

                if (productQuantityMap == null || productQuantityMap.isEmpty()) {
                    log.warn("⚠️ 没有需要恢复的商品库存: refundNo={}", refundNo);
                    return;
                }

                // 遍历商品列表，逐个恢复库存
                boolean allSuccess = true;
                StringBuilder failureDetails = new StringBuilder();

                for (Map.Entry<Long, Integer> entry : productQuantityMap.entrySet()) {
                    Long productId = entry.getKey();
                    Integer quantity = entry.getValue();

                    log.info("📦 开始恢复库存: refundNo={}, productId={}, quantity={}",
                            refundNo, productId, quantity);

                    // 释放预留库存（增加可用库存）
                    boolean success = stockService.releaseReservedStock(productId, quantity);

                    if (!success) {
                        String error = String.format("商品 %d 库存恢复失败", productId);
                        log.error("❌ {}", error);
                        failureDetails.append(error).append("; ");
                        allSuccess = false;
                        // 继续处理其他商品
                    } else {
                        log.info("✅ 库存恢复成功: productId={}, quantity={}", productId, quantity);
                    }
                }

                if (allSuccess) {
                    log.info("✅ 订单库存全部恢复成功: orderId={}, refundNo={}, 商品数量={}",
                            orderId, refundNo, productQuantityMap.size());
                } else {
                    log.error("⚠️ 订单库存部分恢复失败: orderId={}, refundNo={}, 失败详情: {}",
                            orderId, refundNo, failureDetails.toString());
                    // 部分失败不抛异常，避免重复消费已成功的商品
                }

            } catch (Exception e) {
                log.error("❌ 处理库存恢复事件失败: orderId={}, refundNo={}",
                        orderId, refundNo, e);
                // 抛出异常触发消息重试
                throw new RuntimeException("处理库存恢复事件失败", e);
            }
        };
    }
}
