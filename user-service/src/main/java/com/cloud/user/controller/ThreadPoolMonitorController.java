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

/**
 * 线程池监控控制器
 * 提供线程池状态监控接口
 */
@Slf4j
@RestController
@RequestMapping("/thread-pool")
@Tag(name = "线程池监控")
@RequiredArgsConstructor
public class ThreadPoolMonitorController {


    private final ApplicationContext applicationContext;

    /**
     * 获取所有线程池信息
     *
     * @return 线程池信息列表
     */
    @GetMapping("/info")
    @Operation(summary = "获取所有线程池信息")
    public Result<List<Map<String, Object>>> getAllThreadPoolInfo() {
        log.info("获取所有线程池信息");

        List<Map<String, Object>> threadPoolInfoList = new ArrayList<>();

        try {
            // 获取所有ThreadPoolTaskExecutor类型的Bean
            Map<String, ThreadPoolTaskExecutor> threadPoolBeans =
                    applicationContext.getBeansOfType(ThreadPoolTaskExecutor.class);

            for (Map.Entry<String, ThreadPoolTaskExecutor> entry : threadPoolBeans.entrySet()) {
                String beanName = entry.getKey();
                ThreadPoolTaskExecutor executor = entry.getValue();

                Map<String, Object> info = buildThreadPoolInfo(beanName, executor);
                threadPoolInfoList.add(info);
            }
        } catch (Exception e) {
            log.error("获取线程池信息时发生异常", e);
            return Result.error("获取线程池信息失败: " + e.getMessage());
        }

        log.info("成功获取{}个线程池信息", threadPoolInfoList.size());
        return Result.success(threadPoolInfoList);
    }

    /**
     * 根据名称获取特定线程池信息
     *
     * @param name 线程池名称
     * @return 线程池信息
     */
    @GetMapping("/info/detail")
    @Operation(summary = "根据名称获取特定线程池信息")
    public Result<Map<String, Object>> getThreadPoolInfoByName(
            @Parameter(description = "线程池名称") @RequestParam String name) {
        log.info("获取线程池信息，名称: {}", name);

        try {
            ThreadPoolTaskExecutor executor = applicationContext.getBean(name, ThreadPoolTaskExecutor.class);
            Map<String, Object> info = buildThreadPoolInfo(name, executor);

            log.info("成功获取线程池{}的信息", name);
            return Result.success(info);
        } catch (NoSuchBeanDefinitionException e) {
            log.warn("未找到名称为{}的线程池", name);
            return Result.error("未找到名称为" + name + "的线程池");
        } catch (Exception e) {
            log.error("获取线程池信息时发生异常", e);
            return Result.error("获取线程池信息失败: " + e.getMessage());
        }
    }

    /**
     * 构建线程池信息Map
     *
     * @param name     线程池名称
     * @param executor 线程池执行器
     * @return 线程池信息Map
     */
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