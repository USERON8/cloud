package com.cloud.common.messaging.config;

import com.cloud.common.messaging.deadletter.DeadLetterOpsService;
import com.cloud.common.result.Result;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/mq/governance")
public class RocketMqGovernanceController {

  private final RocketMqConsumerTopology consumerTopology;
  private final DeadLetterOpsService deadLetterOpsService;

  public RocketMqGovernanceController(
      RocketMqConsumerTopology consumerTopology, DeadLetterOpsService deadLetterOpsService) {
    this.consumerTopology = consumerTopology;
    this.deadLetterOpsService = deadLetterOpsService;
  }

  @GetMapping("/consumers")
  public Result<List<Map<String, Object>>> consumers() {
    return Result.success(
        consumerTopology.discoverConsumers().stream()
            .map(
                consumer ->
                    consumer.toMap(deadLetterOpsService.countPendingByTopic(consumer.getTopic())))
            .collect(Collectors.toList()));
  }
}
