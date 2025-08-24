package com.cloud.log.event;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通用日志事件类，用于接收各种服务的日志消息
 */
@Data
public class GenericLogEvent {
    /**
     * 跟踪ID
     */
    private String traceId;

    /**
     * 服务名称 (admin, merchant, user, product, payment, stock)
     */
    private String serviceName;

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 附加数据
     */
    private Map<String, Object> extraData;
}