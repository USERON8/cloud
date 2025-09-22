package com.cloud.log.service;

import com.cloud.common.domain.event.LogCollectionEvent;

/**
 * 日志收集服务接口
 *
 * @author cloud
 * @date 2025/1/15
 */
public interface LogCollectionService {

    /**
     * 保存日志事件到Elasticsearch
     *
     * @param event 日志事件
     * @return 保存是否成功
     */
    boolean saveLogEvent(LogCollectionEvent event);

    /**
     * 检查日志是否已处理（幂等性检查）
     *
     * @param traceId 追踪ID
     * @return 是否已处理
     */
    boolean isLogProcessed(String traceId);

    /**
     * 标记日志已处理
     *
     * @param traceId 追踪ID
     */
    void markLogProcessed(String traceId);
}
