package com.cloud.common.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AbstractJsonMqConsumerTest {

  @Test
  void deserializeUsesPayloadClassConfiguration() {
    TestEventConsumer consumer = new TestEventConsumer();
    ReflectionTestUtils.setField(consumer, "mqObjectMapper", new ObjectMapper());

    TestEvent payload = consumer.deserializeBody("{\"eventId\":\"evt-1\",\"orderNo\":\"ord-1\"}");

    assertThat(payload.getEventId()).isEqualTo("evt-1");
    assertThat(payload.getOrderNo()).isEqualTo("ord-1");
    assertThat(consumer.resolve("PAYMENT_SUCCESS", payload.getEventId(), payload.getOrderNo()))
        .isEqualTo("evt-1");
  }

  @Test
  void deserializeMapPayloadAndFallbackHelpersWorkTogether() {
    TestMapConsumer consumer = new TestMapConsumer();
    ReflectionTestUtils.setField(consumer, "mqObjectMapper", new ObjectMapper());

    Map<String, Object> payload =
        consumer.deserializeBody("{\"refundNo\":\"rf-1\",\"userId\":12,\"approved\":true}");

    assertThat(consumer.readStringValue(payload, "refundNo")).isEqualTo("rf-1");
    assertThat(consumer.readLongValue(payload, "userId")).isEqualTo(12L);
    assertThat(consumer.readBooleanValue(payload, "approved")).isTrue();
    assertThat(consumer.resolve("REFUND_AUDITED", null, payload.get("refundNo")))
        .isEqualTo("REFUND_AUDITED:rf-1");
  }

  private static final class TestEventConsumer extends AbstractJsonMqConsumer<TestEvent> {

    @Override
    protected Class<TestEvent> payloadClass() {
      return TestEvent.class;
    }

    @Override
    protected String payloadDescription() {
      return "TestEvent";
    }

    @Override
    protected void doConsume(
        TestEvent payload, org.apache.rocketmq.common.message.MessageExt msgExt) {}

    private TestEvent deserializeBody(String body) {
      return deserialize(body.getBytes(StandardCharsets.UTF_8));
    }

    private String resolve(String eventType, String eventId, Object identifier) {
      return resolveEventId(eventType, eventId, identifier);
    }
  }

  private static final class TestMapConsumer extends AbstractJsonMapMqConsumer {

    @Override
    protected String payloadDescription() {
      return "TestMapEvent";
    }

    @Override
    protected void doConsume(
        Map<String, Object> payload, org.apache.rocketmq.common.message.MessageExt msgExt) {}

    private Map<String, Object> deserializeBody(String body) {
      return deserialize(body.getBytes(StandardCharsets.UTF_8));
    }

    private String readStringValue(Map<String, Object> payload, String key) {
      return readString(payload, key);
    }

    private Long readLongValue(Map<String, Object> payload, String key) {
      return readLong(payload, key);
    }

    private Boolean readBooleanValue(Map<String, Object> payload, String key) {
      return readBoolean(payload, key);
    }

    private String resolve(String eventType, String eventId, Object identifier) {
      return resolveEventId(eventType, eventId, identifier);
    }
  }

  public static class TestEvent {

    private String eventId;
    private String orderNo;

    public String getEventId() {
      return eventId;
    }

    public void setEventId(String eventId) {
      this.eventId = eventId;
    }

    public String getOrderNo() {
      return orderNo;
    }

    public void setOrderNo(String orderNo) {
      this.orderNo = orderNo;
    }
  }
}
