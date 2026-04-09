package com.cloud.common.messaging.config;

import com.cloud.common.config.properties.MessageProperties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.StringUtils;

public class RocketMqConsumerTopology {

  private final ListableBeanFactory beanFactory;
  private final MessageProperties messageProperties;

  public RocketMqConsumerTopology(
      ListableBeanFactory beanFactory, MessageProperties messageProperties) {
    this.beanFactory = beanFactory;
    this.messageProperties = messageProperties;
  }

  public List<ConsumerTarget> discoverConsumers() {
    String[] beanNames = beanFactory.getBeanNamesForAnnotation(RocketMQMessageListener.class);
    List<ConsumerTarget> targets = new ArrayList<>();
    for (String beanName : beanNames) {
      Class<?> beanType = beanFactory.getType(beanName);
      if (beanType == null) {
        continue;
      }
      RocketMQMessageListener listener = beanType.getAnnotation(RocketMQMessageListener.class);
      if (listener == null
          || !StringUtils.hasText(listener.topic())
          || !StringUtils.hasText(listener.consumerGroup())) {
        continue;
      }
      MessageProperties.TargetConfig targetConfig =
          findConfig(listener.topic(), listener.consumerGroup());
      targets.add(
          new ConsumerTarget(
              listener.topic(),
              listener.consumerGroup(),
              beanName,
              beanType.getName(),
              resolveLagThreshold(targetConfig),
              resolveMaxReconsumeTimes(listener, targetConfig)));
    }
    targets.sort(
        Comparator.comparing(ConsumerTarget::getTopic)
            .thenComparing(ConsumerTarget::getConsumerGroup)
            .thenComparing(ConsumerTarget::getBeanName));
    return targets;
  }

  private long resolveLagThreshold(MessageProperties.TargetConfig targetConfig) {
    if (targetConfig != null && targetConfig.getLagAlertThreshold() >= 0) {
      return targetConfig.getLagAlertThreshold();
    }
    return messageProperties.getMonitor().getLagAlertThreshold();
  }

  private int resolveMaxReconsumeTimes(
      RocketMQMessageListener listener, MessageProperties.TargetConfig targetConfig) {
    if (targetConfig != null && targetConfig.getMaxReconsumeTimes() >= 0) {
      return targetConfig.getMaxReconsumeTimes();
    }
    return listener.maxReconsumeTimes();
  }

  private MessageProperties.TargetConfig findConfig(String topic, String consumerGroup) {
    if (messageProperties.getMonitor().getTargets() == null) {
      return null;
    }
    for (MessageProperties.TargetConfig targetConfig :
        messageProperties.getMonitor().getTargets()) {
      if (targetConfig == null) {
        continue;
      }
      if (topic.equals(targetConfig.getTopic())
          && consumerGroup.equals(targetConfig.getConsumerGroup())) {
        return targetConfig;
      }
    }
    return null;
  }

  @Getter
  public static class ConsumerTarget {

    private final String topic;
    private final String consumerGroup;
    private final String beanName;
    private final String beanType;
    private final long lagAlertThreshold;
    private final int maxReconsumeTimes;

    public ConsumerTarget(
        String topic,
        String consumerGroup,
        String beanName,
        String beanType,
        long lagAlertThreshold,
        int maxReconsumeTimes) {
      this.topic = topic;
      this.consumerGroup = consumerGroup;
      this.beanName = beanName;
      this.beanType = beanType;
      this.lagAlertThreshold = lagAlertThreshold;
      this.maxReconsumeTimes = maxReconsumeTimes;
    }

    public Map<String, Object> toMap(long pendingDeadLetters) {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("topic", topic);
      data.put("consumerGroup", consumerGroup);
      data.put("beanName", beanName);
      data.put("beanType", beanType);
      data.put("lagAlertThreshold", lagAlertThreshold);
      data.put("maxReconsumeTimes", maxReconsumeTimes);
      data.put("pendingDeadLetters", pendingDeadLetters);
      return data;
    }
  }
}
