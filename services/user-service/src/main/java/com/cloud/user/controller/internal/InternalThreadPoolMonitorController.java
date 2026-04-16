package com.cloud.user.controller.internal;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.Result;
import com.cloud.common.threadpool.ThreadPoolInfo;
import com.cloud.common.threadpool.ThreadPoolMonitor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/thread-pool/internal")
@Tag(name = "Internal Thread Pool Monitor", description = "Internal governance thread pool APIs")
@RequiredArgsConstructor
public class InternalThreadPoolMonitorController {

  private final ThreadPoolMonitor threadPoolMonitor;

  @GetMapping("/info")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get all thread pool metrics for internal governance callers")
  public Result<List<Map<String, Object>>> getAllThreadPoolInfo() {
    List<Map<String, Object>> threadPoolInfoList = new ArrayList<>();
    for (ThreadPoolInfo info : threadPoolMonitor.getAllThreadPoolInfo().values()) {
      threadPoolInfoList.add(toResponse(info));
    }
    return Result.success(threadPoolInfoList);
  }

  @GetMapping("/info/detail")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get thread pool metrics by bean name for internal governance callers")
  public Result<Map<String, Object>> getThreadPoolInfoByName(
      @Parameter(description = "Thread pool bean name") @RequestParam String name) {
    ThreadPoolInfo info = threadPoolMonitor.getThreadPoolInfo(name);
    if (info == null) {
      throw new BizException(ResultCode.NOT_FOUND, "Thread pool bean not found: " + name);
    }
    return Result.success(toResponse(info));
  }

  private Map<String, Object> toResponse(ThreadPoolInfo info) {
    Map<String, Object> response = new HashMap<>();
    response.put("name", info.getBeanName());
    response.put("corePoolSize", info.getCorePoolSize());
    response.put("maxPoolSize", info.getMaximumPoolSize());
    response.put("activeCount", info.getActiveThreadCount());
    response.put("poolSize", info.getCurrentPoolSize());
    response.put("queueSize", info.getQueueSize());
    response.put("completedTaskCount", info.getCompletedTaskCount());
    response.put("taskCount", info.getTotalTaskCount());
    int queueRemainingCapacity = Math.max(info.getQueueCapacity() - info.getQueueSize(), 0);
    response.put("queueRemainingCapacity", queueRemainingCapacity);
    return response;
  }
}
