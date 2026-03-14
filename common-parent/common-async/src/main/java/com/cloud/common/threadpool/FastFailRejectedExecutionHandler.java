package com.cloud.common.threadpool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class FastFailRejectedExecutionHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        int queueSize = executor.getQueue() == null ? 0 : executor.getQueue().size();
        String message = String.format(
                "Thread pool overloaded: poolSize=%d, active=%d, queued=%d, taskCount=%d",
                executor.getPoolSize(),
                executor.getActiveCount(),
                queueSize,
                executor.getTaskCount()
        );
        log.warn(message);
        throw new RejectedExecutionException(message);
    }
}
