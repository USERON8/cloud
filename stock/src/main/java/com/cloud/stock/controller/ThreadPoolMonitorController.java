package com.cloud.stock.controller;

import com.cloud.common.domain.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池监控控制器
 */
@RestController
@RequestMapping("/stock/monitor")
@RequiredArgsConstructor
@Slf4j
public class ThreadPoolMonitorController {

    private final ThreadPoolTaskExecutor stockQueryExecutor;

    /**
     * 获取线程池状态
     */
    @GetMapping("/thread-pool")
    public Result<Map<String, Object>> getThreadPoolStatus() {
        ThreadPoolExecutor executor = stockQueryExecutor.getThreadPoolExecutor();

        Map<String, Object> status = new HashMap<>();
        status.put("corePoolSize", executor.getCorePoolSize());
        status.put("maximumPoolSize", executor.getMaximumPoolSize());
        status.put("activeCount", executor.getActiveCount());
        status.put("poolSize", executor.getPoolSize());
        status.put("queueSize", executor.getQueue().size());
        status.put("completedTaskCount", executor.getCompletedTaskCount());
        status.put("taskCount", executor.getTaskCount());

        log.info("线程池状态查询: {}", status);

        return Result.success(status);
    }
}