package com.cloud.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeMetrics {

  private static final String METRIC_ORDER = "trade_order_total";
  private static final String METRIC_PAYMENT = "trade_payment_total";
  private static final String METRIC_REFUND = "trade_refund_total";
  private static final String METRIC_STOCK_FREEZE = "trade_stock_freeze_total";
  private static final String METRIC_ORDER_PLACEMENT = "trade_order_placement_total";
  private static final String METRIC_STOCK_RELEASE = "trade_stock_release_total";
  private static final String METRIC_PAYMENT_CALLBACK = "trade_payment_callback_total";
  private static final String METRIC_MESSAGE_CONSUME = "trade_message_consume_total";

  private final MeterRegistry meterRegistry;
  private final Map<String, Counter> counters = new ConcurrentHashMap<>();

  public void incrementOrder(String result) {
    increment(METRIC_ORDER, result, null);
  }

  public void incrementPayment(String result) {
    increment(METRIC_PAYMENT, result, null);
  }

  public void incrementRefund(String result) {
    increment(METRIC_REFUND, result, null);
  }

  public void incrementStockFreeze(String result) {
    increment(METRIC_STOCK_FREEZE, result, null);
  }

  public void incrementOrderPlacement(String result) {
    increment(METRIC_ORDER_PLACEMENT, result, null);
  }

  public void incrementStockRelease(String result) {
    increment(METRIC_STOCK_RELEASE, result, null);
  }

  public void incrementPaymentCallback(String result) {
    increment(METRIC_PAYMENT_CALLBACK, result, null);
  }

  public void incrementMessageConsume(String eventType, String result) {
    increment(METRIC_MESSAGE_CONSUME, result, eventType);
  }

  private void increment(String metric, String result, String eventType) {
    counter(metric, result, eventType).increment();
  }

  private Counter counter(String metric, String result, String eventType) {
    String safeResult = result == null ? "unknown" : result;
    String safeEvent = eventType == null ? "" : eventType;
    String key = metric + ":" + safeResult + ":" + safeEvent;
    return counters.computeIfAbsent(
        key,
        ignored -> {
          Counter.Builder builder =
              Counter.builder(metric)
                  .description("Trade chain business metrics")
                  .tag("result", safeResult);
          if (!safeEvent.isBlank()) {
            builder.tag("eventType", safeEvent);
          }
          return builder.register(meterRegistry);
        });
  }
}
