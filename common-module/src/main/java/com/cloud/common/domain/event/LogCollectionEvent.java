package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 日志收集事件
 * 用于各微服务向日志服务发送业务日志
 *
 * @author cloud
 * @date 2025/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogCollectionEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    private String logId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 日志级别：DEBUG, INFO, WARN, ERROR
     */
    private String logLevel;

    /**
     * 日志类型：BUSINESS, SYSTEM, SECURITY, PERFORMANCE
     */
    private String logType;

    /**
     * 业务模块
     */
    private String module;

    /**
     * 操作类型：CREATE, UPDATE, DELETE, QUERY, LOGIN, LOGOUT等
     */
    private String operation;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户类型：CUSTOMER, MERCHANT, ADMIN
     */
    private String userType;

    /**
     * 业务ID（如订单ID、商品ID等）
     */
    private String businessId;

    /**
     * 业务类型（如ORDER, PRODUCT, PAYMENT等）
     */
    private String businessType;

    /**
     * 请求IP地址
     */
    private String clientIp;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 请求URI
     */
    private String requestUri;

    /**
     * 请求方法：GET, POST, PUT, DELETE
     */
    private String requestMethod;

    /**
     * 请求参数（敏感信息已脱敏）
     */
    private String requestParams;

    /**
     * 响应状态码
     */
    private Integer responseStatus;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;

    /**
     * 异常信息
     */
    private String exceptionMessage;

    /**
     * 异常堆栈
     */
    private String exceptionStack;

    /**
     * 扩展字段
     */
    private Map<String, Object> extendedFields;

    /**
     * 日志内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 跟踪ID，用于链路追踪
     */
    private String traceId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 操作结果：SUCCESS, FAILURE
     */
    private String result;

    /**
     * 操作耗时（毫秒）
     */
    private Long duration;

    /**
     * 数据变更前的值（JSON格式）
     */
    private String beforeData;

    /**
     * 数据变更后的值（JSON格式）
     */
    private String afterData;

    /**
     * 备注信息
     */
    private String remark;
}
