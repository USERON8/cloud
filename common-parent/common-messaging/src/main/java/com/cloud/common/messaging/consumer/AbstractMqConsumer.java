package com.cloud.common.messaging.consumer;

import com.cloud.common.exception.BizException;
import com.cloud.common.exception.RemoteException;
import com.cloud.common.exception.SystemException;
import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.deadletter.DeadLetterReason;
import com.cloud.common.messaging.deadletter.DeadLetterService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class AbstractMqConsumer<T> implements RocketMQListener<MessageExt> {

  @Autowired protected DeadLetterService deadLetterService;

  @Autowired protected MessageIdempotencyService messageIdempotencyService;

  @Autowired(required = false)
  protected MeterRegistry meterRegistry;

  @Override
  public final void onMessage(MessageExt msgExt) {
    String topic = resolveTopic(msgExt);
    String msgId = msgExt == null ? null : msgExt.getMsgId();
    int reconsumeTimes = resolveReconsumeTimes(msgExt);
    T payload;

    try {
      payload = deserialize(msgExt == null ? null : msgExt.getBody());
    } catch (Exception ex) {
      log.error(
          "[MQ][{}] Deserialize failed, skip msgId={} body={}",
          topic,
          msgId,
          toBodyText(msgExt),
          ex);
      deadLetterService.record(
          topic,
          msgId,
          msgExt == null ? null : msgExt.getBody(),
          DeadLetterReason.DESERIALIZE_FAIL,
          ex);
      increment("mq.consume.deserialize_fail", "topic", topic);
      onDeserializeFail(msgExt, ex);
      return;
    }

    String namespace = resolveIdempotentNamespace(topic, msgExt, payload);
    String idempotentKey = buildIdempotentKey(topic, msgId, payload, msgExt);
    if (!messageIdempotencyService.tryAcquire(namespace, idempotentKey)) {
      log.info("[MQ][{}] Duplicate message, skip msgId={} key={}", topic, msgId, idempotentKey);
      increment("mq.consume.duplicate", "topic", topic);
      onDuplicate(msgExt, payload, idempotentKey);
      return;
    }

    if (reconsumeTimes >= getMaxReconsumeTimes()) {
      log.error(
          "[MQ][{}] Exceed max reconsume, reconsumeTimes={} msgId={} payload={}",
          topic,
          reconsumeTimes,
          msgId,
          payload);
      deadLetterService.record(topic, msgId, payload, DeadLetterReason.MAX_RECONSUME, null);
      increment("mq.consume.exhausted", "topic", topic);
      messageIdempotencyService.markSuccess(namespace, idempotentKey);
      onMaxReconsume(msgExt, payload, reconsumeTimes);
      return;
    }

    try {
      doConsume(payload, msgExt);
      messageIdempotencyService.markSuccess(namespace, idempotentKey);
      increment("mq.consume.success", "topic", topic);
      onConsumeSuccess(msgExt, payload);
    } catch (BizException ex) {
      log.warn(
          "[MQ][{}][BIZ-ACK] code={} msgId={} reconsumeTimes={} message={}",
          topic,
          ex.getCode(),
          msgId,
          reconsumeTimes,
          ex.getMessage());
      deadLetterService.record(topic, msgId, payload, DeadLetterReason.BIZ_FAIL, ex);
      increment("mq.consume.biz_fail", "topic", topic, "code", String.valueOf(ex.getCode()));
      messageIdempotencyService.markSuccess(namespace, idempotentKey);
      onBizException(msgExt, payload, ex);
    } catch (SystemException ex) {
      boolean retryable = ex.isRetryable();
      if (retryable) {
        log.error(
            "[MQ][{}][SYS-NACK] retryable system exception, reconsumeTimes={} msgId={}",
            topic,
            reconsumeTimes,
            msgId,
            ex);
        increment("mq.consume.sys_fail", "topic", topic, "retryable", String.valueOf(true));
        onSystemException(msgExt, payload, ex, true);
        messageIdempotencyService.release(namespace, idempotentKey);
        throw ex;
      }
      log.error("[MQ][{}][SYS-ACK] non-retryable system exception, msgId={}", topic, msgId, ex);
      deadLetterService.record(topic, msgId, payload, DeadLetterReason.SYS_NONRETRYABLE, ex);
      increment("mq.consume.sys_fail", "topic", topic, "retryable", String.valueOf(false));
      messageIdempotencyService.markSuccess(namespace, idempotentKey);
      onSystemException(msgExt, payload, ex, false);
    } catch (RemoteException ex) {
      log.error(
          "[MQ][{}][REMOTE-NACK] remote exception, reconsumeTimes={} msgId={}",
          topic,
          reconsumeTimes,
          msgId,
          ex);
      increment("mq.consume.sys_fail", "topic", topic, "retryable", String.valueOf(true));
      onRemoteException(msgExt, payload, ex);
      messageIdempotencyService.release(namespace, idempotentKey);
      throw ex;
    } catch (Exception ex) {
      log.error(
          "[MQ][{}][UNKNOWN-NACK] unknown exception, reconsumeTimes={} msgId={}",
          topic,
          reconsumeTimes,
          msgId,
          ex);
      increment("mq.consume.unknown_fail", "topic", topic);
      onUnknownException(msgExt, payload, ex);
      messageIdempotencyService.release(namespace, idempotentKey);
      throw ex;
    }
  }

  protected abstract void doConsume(T payload, MessageExt msgExt);

  protected abstract T deserialize(byte[] body);

  protected int getMaxReconsumeTimes() {
    return 16;
  }

  protected String resolveTopic(MessageExt msgExt) {
    if (msgExt == null || msgExt.getTopic() == null || msgExt.getTopic().isBlank()) {
      return "unknown";
    }
    return msgExt.getTopic();
  }

  protected String resolveIdempotentNamespace(String topic, MessageExt msgExt, T payload) {
    return topic;
  }

  protected String buildIdempotentKey(String topic, String msgId, T payload, MessageExt msgExt) {
    return msgId == null ? "" : msgId;
  }

  protected int resolveReconsumeTimes(MessageExt msgExt) {
    if (msgExt == null) {
      return 0;
    }
    return Math.max(0, msgExt.getReconsumeTimes());
  }

  protected void onDeserializeFail(MessageExt msgExt, Exception ex) {}

  protected void onDuplicate(MessageExt msgExt, T payload, String idempotentKey) {}

  protected void onMaxReconsume(MessageExt msgExt, T payload, int reconsumeTimes) {}

  protected void onConsumeSuccess(MessageExt msgExt, T payload) {}

  protected void onBizException(MessageExt msgExt, T payload, BizException ex) {}

  protected void onSystemException(
      MessageExt msgExt, T payload, SystemException ex, boolean retryable) {}

  protected void onRemoteException(MessageExt msgExt, T payload, RemoteException ex) {}

  protected void onUnknownException(MessageExt msgExt, T payload, Exception ex) {}

  private void increment(String name, String... tags) {
    if (meterRegistry == null) {
      return;
    }
    meterRegistry.counter(name, tags).increment();
  }

  private String toBodyText(MessageExt msgExt) {
    if (msgExt == null || msgExt.getBody() == null) {
      return "";
    }
    return new String(msgExt.getBody(), java.nio.charset.StandardCharsets.UTF_8);
  }
}
