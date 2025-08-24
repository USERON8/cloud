package com.cloud.log.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.cloud.log.event.GenericLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 通用日志消费者服务
 * 用于消费来自不同服务的日志消息并记录到Elasticsearch
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenericLogConsumerService {

    private final ElasticsearchClient elasticsearchClient;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 通用日志消息消费者
     *
     * @return Consumer函数式接口
     */
    public Consumer<GenericLogEvent> genericLogConsumer() {
        return event -> {
            try {
                // 幂等性检查
                String traceId = event.getTraceId();
                if (traceId == null) {
                    log.warn("消息缺少traceId，跳过处理");
                    return;
                }

                // 转换并保存日志到Elasticsearch
                saveLogToElasticsearch(event);

                log.info("日志记录成功: {} | 服务: {} | 操作人: {}", 
                        traceId, event.getServiceName(), event.getOperator());
            } catch (Exception e) {
                log.error("日志处理失败: {}", event.getTraceId(), e);
                throw new RuntimeException("日志处理失败", e); // 触发重试
            }
        };
    }

    /**
     * 保存日志到Elasticsearch
     * @param event 日志事件
     */
    private void saveLogToElasticsearch(GenericLogEvent event) {
        try {
            // 转换为Elasticsearch文档
            Map<String, Object> document = convertToDocument(event);

            // 根据服务名称确定索引
            String indexName = event.getServiceName() + "_logs";

            // 构建索引请求
            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                    .index(indexName)
                    .id(event.getTraceId())
                    .document(document)
            );

            // 执行索引操作
            IndexResponse response = elasticsearchClient.index(request);
            log.info("日志已保存到Elasticsearch，索引: {}，文档ID: {}", indexName, response.id());
        } catch (IOException e) {
            log.error("保存日志到Elasticsearch失败", e);
        } catch (Exception e) {
            log.error("保存日志到Elasticsearch时发生未知错误", e);
        }
    }

    /**
     * 将日志事件转换为Elasticsearch文档
     *
     * @param event 日志事件
     * @return Elasticsearch文档Map
     */
    private Map<String, Object> convertToDocument(GenericLogEvent event) {
        Map<String, Object> document = new HashMap<>();
        document.put("traceId", event.getTraceId());
        document.put("serviceName", event.getServiceName());
        document.put("operation", event.getOperation());
        document.put("description", event.getDescription());
        document.put("operator", event.getOperator());
        document.put("operateTime", event.getOperateTime() != null ? 
                event.getOperateTime().format(DATE_TIME_FORMATTER) : null);
        document.put("ipAddress", event.getIpAddress());
        document.put("userAgent", event.getUserAgent());
        
        // 添加附加数据
        if (event.getExtraData() != null) {
            document.putAll(event.getExtraData());
        }
        
        return document;
    }
}