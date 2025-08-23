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
    private final ThreadPoolTaskExecutor stockOperationExecutor;
    private final ThreadPoolTaskExecutor stockCommonAsyncExecutor;
    private final ThreadPoolTaskExecutor stockNewExecutor;

    /**
     * 获取所有线程池状态
     */
    @GetMapping("/thread-pool")
    public Result<Map<String, Object>> getThreadPoolStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 查询线程池状态
        status.put("stockQueryExecutor", getThreadPoolInfo(stockQueryExecutor, "stockQueryExecutor"));
        
        // 操作线程池状态
        status.put("stockOperationExecutor", getThreadPoolInfo(stockOperationExecutor, "stockOperationExecutor"));
        
        // 通用异步线程池状态
        status.put("stockCommonAsyncExecutor", getThreadPoolInfo(stockCommonAsyncExecutor, "stockCommonAsyncExecutor"));
        
        // 新线程池状态
        status.put("stockNewExecutor", getThreadPoolInfo(stockNewExecutor, "stockNewExecutor"));

        log.info("线程池状态查询: {}", status);

        return Result.success(status);
    }
    
    /**
     * 获取单个线程池信息
     * 
     * @param executor 线程池执行器
     * @param name 线程池名称
     * @return 线程池信息
     */
    private Map<String, Object> getThreadPoolInfo(ThreadPoolTaskExecutor executor, String name) {
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
        
        Map<String, Object> info = new HashMap<>();
        info.put("name", name);
        info.put("corePoolSize", threadPoolExecutor.getCorePoolSize());
        info.put("maximumPoolSize", threadPoolExecutor.getMaximumPoolSize());
        info.put("activeCount", threadPoolExecutor.getActiveCount());
        info.put("poolSize", threadPoolExecutor.getPoolSize());
        info.put("queueSize", threadPoolExecutor.getQueue().size());
        info.put("completedTaskCount", threadPoolExecutor.getCompletedTaskCount());
        info.put("taskCount", threadPoolExecutor.getTaskCount());
        
        return info;
    }
}