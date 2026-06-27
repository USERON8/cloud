package com.cloud.user.controller;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.Result;
import com.cloud.common.threadpool.ThreadPoolInfo;
import com.cloud.common.threadpool.ThreadPoolMonitor;
import com.cloud.common.threadpool.ThreadPoolResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/thread-pools")
@Tag(name = "线程池监控", description = "线程池监控接口")
@RequiredArgsConstructor
public class ThreadPoolMonitorController {

  private final ThreadPoolMonitor threadPoolMonitor;

  @GetMapping
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "获取全部线程池指标")
  public Result<List<Map<String, Object>>> getAllThreadPoolInfo() {
    List<Map<String, Object>> threadPoolInfoList = new ArrayList<>();
    for (ThreadPoolInfo info : threadPoolMonitor.getAllThreadPoolInfo().values()) {
      Map<String, Object> item = ThreadPoolResponseMapper.toResponse(info);
      threadPoolInfoList.add(item);
    }
    return Result.success(threadPoolInfoList);
  }

  @GetMapping("/{name}")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "按 Bean 名称获取线程池指标")
  public Result<Map<String, Object>> getThreadPoolInfoByName(
      @Parameter(description = "线程池 Bean 名称") @PathVariable String name) {
    ThreadPoolInfo info = threadPoolMonitor.getThreadPoolInfo(name);
    if (info == null) {
      throw new BizException(ResultCode.NOT_FOUND, "Thread pool bean not found: " + name);
    }
    return Result.success(ThreadPoolResponseMapper.toResponse(info));
  }
}
