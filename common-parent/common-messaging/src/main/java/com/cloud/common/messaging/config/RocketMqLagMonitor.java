package com.cloud.common.messaging.config;

import com.cloud.common.config.properties.MessageProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.protocol.admin.ConsumeStats;
import org.apache.rocketmq.remoting.protocol.admin.OffsetWrapper;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

@Slf4j
public class RocketMqLagMonitor implements InitializingBean, DisposableBean {

  private final ListableBeanFactory beanFactory;
  private final MeterRegistry meterRegistry;
  private final MessageProperties messageProperties;
  private final RocketMqConsumerTopology consumerTopology;
  private final Map<String, AtomicLong> lagGaugeValues = new ConcurrentHashMap<>();
  private final Map<String, MonitorTarget> targets = new ConcurrentHashMap<>();

  @Value("${rocketmq.name-server:${spring.cloud.stream.rocketmq.binder.name-server:}}")
  private String nameServer;

  private DefaultMQAdminExt adminExt;

  public RocketMqLagMonitor(
      ListableBeanFactory beanFactory,
      MeterRegistry meterRegistry,
      MessageProperties messageProperties) {
    this.beanFactory = beanFactory;
    this.meterRegistry = meterRegistry;
    this.messageProperties = messageProperties;
    this.consumerTopology = new RocketMqConsumerTopology(beanFactory, messageProperties);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    discoverTargets();
    if (!StringUtils.hasText(nameServer) || targets.isEmpty()) {
      log.info(
          "RocketMQ lag monitor skipped: nameServerConfigured={}, targetCount={}",
          StringUtils.hasText(nameServer),
          targets.size());
      return;
    }
    this.adminExt = new DefaultMQAdminExt();
    this.adminExt.setNamesrvAddr(nameServer);
    this.adminExt.setInstanceName("mq-lag-monitor-" + System.currentTimeMillis());
    this.adminExt.start();
    log.info("RocketMQ lag monitor started: targetCount={}", targets.size());
  }

  @Scheduled(fixedDelayString = "${app.message.monitor.lag-scan-interval-ms:60000}")
  public void scanLag() {
    if (adminExt == null || targets.isEmpty()) {
      return;
    }
    for (MonitorTarget target : targets.values()) {
      long lag = queryLag(target);
      gaugeValue(target).set(lag);
      long threshold = Math.max(0L, target.lagAlertThreshold());
      if (lag >= threshold && threshold > 0) {
        log.warn(
            "RocketMQ backlog exceeded threshold: topic={}, consumerGroup={}, lag={}, threshold={}",
            target.topic(),
            target.consumerGroup(),
            lag,
            threshold);
      }
    }
  }

  @Override
  public void destroy() throws Exception {
    if (adminExt != null) {
      adminExt.shutdown();
    }
  }

  private void discoverTargets() {
    for (RocketMqConsumerTopology.ConsumerTarget consumerTarget :
        consumerTopology.discoverConsumers()) {
      MonitorTarget target =
          new MonitorTarget(
              consumerTarget.getTopic(),
              consumerTarget.getConsumerGroup(),
              consumerTarget.getLagAlertThreshold());
      targets.put(target.consumerGroup() + "@" + target.topic(), target);
    }
  }

  private long queryLag(MonitorTarget target) {
    try {
      ConsumeStats consumeStats = adminExt.examineConsumeStats(target.consumerGroup());
      if (consumeStats == null || consumeStats.getOffsetTable() == null) {
        return 0L;
      }
      long lag = 0L;
      for (Map.Entry<MessageQueue, OffsetWrapper> entry :
          consumeStats.getOffsetTable().entrySet()) {
        MessageQueue queue = entry.getKey();
        OffsetWrapper offsetWrapper = entry.getValue();
        if (queue == null || offsetWrapper == null || !target.topic().equals(queue.getTopic())) {
          continue;
        }
        lag += Math.max(0L, offsetWrapper.getBrokerOffset() - offsetWrapper.getConsumerOffset());
      }
      return lag;
    } catch (Exception ex) {
      log.warn(
          "Query RocketMQ backlog failed: topic={}, consumerGroup={}",
          target.topic(),
          target.consumerGroup(),
          ex);
      return 0L;
    }
  }

  private AtomicLong gaugeValue(MonitorTarget target) {
    String key = target.consumerGroup() + "@" + target.topic();
    return lagGaugeValues.computeIfAbsent(
        key,
        ignored -> {
          AtomicLong value = new AtomicLong();
          Gauge.builder("mq.consumer.backlog", value, AtomicLong::get)
              .tag("topic", target.topic())
              .tag("consumerGroup", target.consumerGroup())
              .register(meterRegistry);
          return value;
        });
  }

  private record MonitorTarget(String topic, String consumerGroup, long lagAlertThreshold) {}
}
