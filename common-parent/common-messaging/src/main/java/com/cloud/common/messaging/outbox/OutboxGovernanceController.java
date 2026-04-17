package com.cloud.common.messaging.outbox;

import com.cloud.common.domain.dto.governance.OutboxBatchRequeueRequestDTO;
import com.cloud.common.result.Result;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/outbox/governance")
public class OutboxGovernanceController {

  private final OutboxGovernanceService outboxGovernanceService;

  public OutboxGovernanceController(OutboxGovernanceService outboxGovernanceService) {
    this.outboxGovernanceService = outboxGovernanceService;
  }

  @GetMapping("/stats")
  public Result<Map<String, Object>> stats() {
    return Result.success(outboxGovernanceService.getStats());
  }

  @GetMapping("/pending")
  public Result<List<OutboxEvent>> pending(
      @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
    return Result.success(outboxGovernanceService.listPending(limit == null ? 20 : limit));
  }

  @GetMapping("/dead")
  public Result<List<OutboxEvent>> dead(
      @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
    return Result.success(outboxGovernanceService.listDead(limit == null ? 20 : limit));
  }

  @PostMapping("/requeue")
  @ResponseStatus(HttpStatus.OK)
  public Result<Boolean> requeue(@RequestParam("id") Long id) {
    return Result.success(outboxGovernanceService.requeue(id));
  }

  @PostMapping("/requeue-batch")
  @ResponseStatus(HttpStatus.OK)
  public Result<Integer> requeueBatch(@RequestBody @Valid OutboxBatchRequeueRequestDTO requestDTO) {
    return Result.success(outboxGovernanceService.requeueBatch(requestDTO.getIds()));
  }
}
