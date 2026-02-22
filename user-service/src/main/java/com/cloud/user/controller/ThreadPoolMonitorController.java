package com.cloud.user.controller;

import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/thread-pool")
@Tag(name = "Thread Pool Monitor", description = "Thread pool monitoring APIs")
@RequiredArgsConstructor
public class ThreadPoolMonitorController {

    private final ApplicationContext applicationContext;

    @GetMapping("/info")
    @Operation(summary = "Get all thread pool metrics")
    public Result<List<Map<String, Object>>> getAllThreadPoolInfo() {
        List<Map<String, Object>> threadPoolInfoList = new ArrayList<>();
        try {
            Map<String, ThreadPoolTaskExecutor> threadPoolBeans =
                    applicationContext.getBeansOfType(ThreadPoolTaskExecutor.class);

            for (Map.Entry<String, ThreadPoolTaskExecutor> entry : threadPoolBeans.entrySet()) {
                Map<String, Object> info = buildThreadPoolInfo(entry.getKey(), entry.getValue());
                threadPoolInfoList.add(info);
            }
            return Result.success(threadPoolInfoList);
        } catch (Exception e) {
            log.error("Failed to get thread pool info", e);
            return Result.error("Failed to get thread pool info: " + e.getMessage());
        }
    }

    @GetMapping("/info/detail")
    @Operation(summary = "Get thread pool metrics by bean name")
    public Result<Map<String, Object>> getThreadPoolInfoByName(
            @Parameter(description = "Thread pool bean name")
            @RequestParam String name) {
        try {
            ThreadPoolTaskExecutor executor = applicationContext.getBean(name, ThreadPoolTaskExecutor.class);
            return Result.success(buildThreadPoolInfo(name, executor));
        } catch (NoSuchBeanDefinitionException e) {
            log.warn("Thread pool bean not found: {}", name);
            return Result.error("Thread pool bean not found: " + name);
        } catch (Exception e) {
            log.error("Failed to get thread pool info by name: {}", name, e);
            return Result.error("Failed to get thread pool info: " + e.getMessage());
        }
    }

    private Map<String, Object> buildThreadPoolInfo(String name, ThreadPoolTaskExecutor executor) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", name);
        info.put("corePoolSize", executor.getCorePoolSize());
        info.put("maxPoolSize", executor.getMaxPoolSize());
        info.put("activeCount", executor.getActiveCount());
        info.put("poolSize", executor.getPoolSize());
        info.put("queueSize", executor.getThreadPoolExecutor().getQueue().size());
        info.put("completedTaskCount", executor.getThreadPoolExecutor().getCompletedTaskCount());
        info.put("taskCount", executor.getThreadPoolExecutor().getTaskCount());
        info.put("queueRemainingCapacity", executor.getThreadPoolExecutor().getQueue().remainingCapacity());
        return info;
    }
}
