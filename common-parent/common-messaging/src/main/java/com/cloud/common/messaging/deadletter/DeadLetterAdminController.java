package com.cloud.common.messaging.deadletter;

import com.cloud.common.config.properties.MessageProperties;
import com.cloud.common.result.Result;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/mq/dead-letters")
public class DeadLetterAdminController {

  private final DeadLetterOpsService deadLetterOpsService;
  private final MessageProperties messageProperties;

  public DeadLetterAdminController(
      DeadLetterOpsService deadLetterOpsService, MessageProperties messageProperties) {
    this.deadLetterOpsService = deadLetterOpsService;
    this.messageProperties = messageProperties;
  }

  @GetMapping("/pending")
  public Result<List<DeadLetterRecord>> pending(
      @RequestParam(value = "limit", required = false) Integer limit) {
    int defaultLimit = Math.max(1, messageProperties.getMonitor().getDeadLetterQueryLimit());
    int safeLimit = limit == null ? defaultLimit : Math.min(Math.max(1, limit), defaultLimit);
    return Result.success(deadLetterOpsService.listPending(safeLimit));
  }

  @PostMapping("/handle")
  @ResponseStatus(HttpStatus.OK)
  public Result<Boolean> handle(
      @RequestParam("topic") String topic, @RequestParam("msgId") String msgId) {
    return Result.success(deadLetterOpsService.markHandled(topic, msgId));
  }
}
