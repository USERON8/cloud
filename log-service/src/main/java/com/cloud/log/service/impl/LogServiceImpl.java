package com.cloud.log.service.impl;

import com.cloud.common.result.PageResult;
import com.cloud.log.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 日志服务实现类
 * 提供统一的日志查询和管理功能
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserEventService userEventService;
    private final OrderEventService orderEventService;
    private final PaymentEventService paymentEventService;
    private final StockEventService stockEventService;

    @Override
    public PageResult<Object> getLogs(Integer page, Integer size, String level, String service,
                                      String startTime, String endTime, String keyword) {
        try {
            log.debug("获取日志列表 - 页码: {}, 大小: {}, 级别: {}, 服务: {}", page, size, level, service);

            // 创建分页对象
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "eventTime"));

            // 根据服务类型查询不同的事件
            List<Object> logs = new ArrayList<>();
            long totalElements = 0;

            if (service == null || "user".equals(service)) {
                Page<?> userEvents = userEventService.findByEventTimeBetween(
                        parseDateTime(startTime), parseDateTime(endTime), pageable);
                logs.addAll(userEvents.getContent());
                totalElements += userEvents.getTotalElements();
            }

            if (service == null || "order".equals(service)) {
                Page<?> orderEvents = orderEventService.findByEventTimeBetween(
                        parseDateTime(startTime), parseDateTime(endTime), pageable);
                logs.addAll(orderEvents.getContent());
                totalElements += orderEvents.getTotalElements();
            }

            return PageResult.of(logs, totalElements, (long) page, (long) size);

        } catch (Exception e) {
            log.error("获取日志列表失败", e);
            return PageResult.empty((long) page, (long) size);
        }
    }

    @Override
    public PageResult<Object> getApplicationLogs(Integer page, Integer size, String appName,
                                                 String level, String startTime, String endTime) {
        try {
            log.debug("获取应用日志 - 应用: {}, 级别: {}", appName, level);

            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "eventTime"));

            // 这里可以根据应用名称过滤日志
            List<Object> logs = new ArrayList<>();
            long totalElements = 0;

            // 示例：获取用户事件作为应用日志
            Page<?> events = userEventService.findByEventTimeBetween(
                    parseDateTime(startTime), parseDateTime(endTime), pageable);
            logs.addAll(events.getContent());
            totalElements = events.getTotalElements();

            return PageResult.of(logs, totalElements, (long) page, (long) size);

        } catch (Exception e) {
            log.error("获取应用日志失败", e);
            return PageResult.empty((long) page, (long) size);
        }
    }

    @Override
    public PageResult<Object> getOperationLogs(Integer page, Integer size, Long userId,
                                               String operationType, String startTime, String endTime) {
        try {
            log.debug("获取操作日志 - 用户ID: {}, 操作类型: {}", userId, operationType);

            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "eventTime"));

            List<Object> logs = new ArrayList<>();
            long totalElements = 0;

            if (userId != null) {
                List<?> userEvents = userEventService.findByUserId(userId);
                logs.addAll(userEvents);
                totalElements += userEvents.size();

                Page<?> orderEvents = orderEventService.findByUserId(userId, pageable);
                logs.addAll(orderEvents.getContent());
                totalElements += orderEvents.getTotalElements();
            } else {
                // 获取所有操作日志
                Page<?> userEvents = userEventService.findByEventTimeBetween(
                        parseDateTime(startTime), parseDateTime(endTime), pageable);
                logs.addAll(userEvents.getContent());
                totalElements = userEvents.getTotalElements();
            }

            return PageResult.of(logs, totalElements, (long) page, (long) size);

        } catch (Exception e) {
            log.error("获取操作日志失败", e);
            return PageResult.empty((long) page, (long) size);
        }
    }

    @Override
    public PageResult<Object> getErrorLogs(Integer page, Integer size, String service,
                                           String errorType, String startTime, String endTime) {
        try {
            log.debug("获取错误日志 - 服务: {}, 错误类型: {}", service, errorType);

            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "eventTime"));

            // 这里可以根据错误类型过滤日志
            List<Object> logs = new ArrayList<>();
            long totalElements = 0;

            // 示例：获取事件作为错误日志
            Page<?> events = userEventService.findByEventTimeBetween(
                    parseDateTime(startTime), parseDateTime(endTime), pageable);
            logs.addAll(events.getContent());
            totalElements = events.getTotalElements();

            return PageResult.of(logs, totalElements, (long) page, (long) size);

        } catch (Exception e) {
            log.error("获取错误日志失败", e);
            return PageResult.empty((long) page, (long) size);
        }
    }

    @Override
    public PageResult<Object> getAccessLogs(Integer page, Integer size, Long userId, String path,
                                            String method, Integer statusCode, String startTime, String endTime) {
        try {
            log.debug("获取访问日志 - 用户ID: {}, 路径: {}", userId, path);

            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "eventTime"));

            List<Object> logs = new ArrayList<>();
            long totalElements = 0;

            if (userId != null) {
                List<?> userEvents = userEventService.findByUserId(userId);
                logs.addAll(userEvents);
                totalElements = userEvents.size();
            } else {
                Page<?> events = userEventService.findByEventTimeBetween(
                        parseDateTime(startTime), parseDateTime(endTime), pageable);
                logs.addAll(events.getContent());
                totalElements = events.getTotalElements();
            }

            return PageResult.of(logs, totalElements, (long) page, (long) size);

        } catch (Exception e) {
            log.error("获取访问日志失败", e);
            return PageResult.empty((long) page, (long) size);
        }
    }

    @Override
    public PageResult<Object> getAuditLogs(Integer page, Integer size, Long userId, String resourceType,
                                           String action, String startTime, String endTime) {
        try {
            log.debug("获取审计日志 - 用户ID: {}, 资源类型: {}, 操作: {}", userId, resourceType, action);

            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "eventTime"));

            List<Object> logs = new ArrayList<>();
            long totalElements = 0;

            // 审计日志主要来自用户事件
            if (userId != null) {
                List<?> userEvents = userEventService.findByUserId(userId);
                logs.addAll(userEvents);
                totalElements = userEvents.size();
            } else {
                Page<?> events = userEventService.findByEventTimeBetween(
                        parseDateTime(startTime), parseDateTime(endTime), pageable);
                logs.addAll(events.getContent());
                totalElements = events.getTotalElements();
            }

            return PageResult.of(logs, totalElements, (long) page, (long) size);

        } catch (Exception e) {
            log.error("获取审计日志失败", e);
            return PageResult.empty((long) page, (long) size);
        }
    }

    @Override
    @Cacheable(value = "logStats", key = "#dimension + ':' + #startTime + ':' + #endTime", unless = "#result == null || #result.empty")
    public Map<String, Object> getLogStatistics(String dimension, String startTime, String endTime) {
        try {
            log.debug("获取日志统计 - 维度: {}", dimension);

            Map<String, Object> statistics = new HashMap<>();

            LocalDateTime start = parseDateTime(startTime);
            LocalDateTime end = parseDateTime(endTime);

            // 统计各类事件数量
            long userEventCount = userEventService.countByEventTimeBetween(start, end);
            long orderEventCount = orderEventService.countByEventTimeBetween(start, end);
            long paymentEventCount = paymentEventService.countByEventTimeBetween(start, end);
            long stockEventCount = stockEventService.countByEventTimeBetween(start, end);

            statistics.put("userEventCount", userEventCount);
            statistics.put("orderEventCount", orderEventCount);
            statistics.put("paymentEventCount", paymentEventCount);
            statistics.put("stockEventCount", stockEventCount);
            statistics.put("totalCount", userEventCount + orderEventCount + paymentEventCount + stockEventCount);

            return statistics;

        } catch (Exception e) {
            log.error("获取日志统计失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public String exportLogs(Map<String, Object> exportRequest) {
        try {
            log.info("导出日志 - 条件: {}", exportRequest);

            // 生成导出任务ID
            String exportId = "export_" + System.currentTimeMillis();

            // 这里可以实现异步导出逻辑
            // 暂时返回任务ID
            return exportId;

        } catch (Exception e) {
            log.error("导出日志失败", e);
            throw new RuntimeException("导出日志失败: " + e.getMessage());
        }
    }

    @Override
    public boolean cleanupLogs(Integer retentionDays, String logType) {
        try {
            log.info("清理日志 - 保留天数: {}, 类型: {}", retentionDays, logType);

            LocalDateTime expiredTime = LocalDateTime.now().minusDays(retentionDays);

            // 清理各类过期事件
            if (logType == null || "user".equals(logType)) {
                userEventService.deleteExpiredEvents(expiredTime);
            }

            if (logType == null || "order".equals(logType)) {
                orderEventService.deleteExpiredEvents(expiredTime);
            }

            if (logType == null || "payment".equals(logType)) {
                paymentEventService.deleteExpiredEvents(expiredTime);
            }

            if (logType == null || "stock".equals(logType)) {
                stockEventService.deleteExpiredEvents(expiredTime);
            }

            return true;

        } catch (Exception e) {
            log.error("清理日志失败", e);
            return false;
        }
    }

    @Override
    public Object getLogById(String id) {
        try {
            log.debug("获取日志详情 - ID: {}", id);

            // 尝试从各个事件服务中查找
            Optional<?> userEvent = userEventService.findById(id);
            if (userEvent.isPresent()) {
                return userEvent.get();
            }

            Optional<?> orderEvent = orderEventService.findById(id);
            if (orderEvent.isPresent()) {
                return orderEvent.get();
            }

            Optional<?> paymentEvent = paymentEventService.findById(id);
            if (paymentEvent.isPresent()) {
                return paymentEvent.get();
            }

            Optional<?> stockEvent = stockEventService.findById(id);
            if (stockEvent.isPresent()) {
                return stockEvent.get();
            }

            return null;

        } catch (Exception e) {
            log.error("获取日志详情失败 - ID: {}", id, e);
            return null;
        }
    }

    @Override
    public PageResult<Object> searchLogs(String keyword, Integer page, Integer size, String logType,
                                         String startTime, String endTime) {
        try {
            log.debug("搜索日志 - 关键词: {}", keyword);

            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "eventTime"));

            List<Object> logs = new ArrayList<>();
            long totalElements = 0;

            // 根据日志类型搜索
            if (logType == null || "user".equals(logType)) {
                Page<?> userEvents = userEventService.findByEventTimeBetween(
                        parseDateTime(startTime), parseDateTime(endTime), pageable);
                logs.addAll(userEvents.getContent());
                totalElements += userEvents.getTotalElements();
            }

            if (logType == null || "order".equals(logType)) {
                Page<?> orderEvents = orderEventService.findByEventTimeBetween(
                        parseDateTime(startTime), parseDateTime(endTime), pageable);
                logs.addAll(orderEvents.getContent());
                totalElements += orderEvents.getTotalElements();
            }

            return PageResult.of(logs, totalElements, (long) page, (long) size);

        } catch (Exception e) {
            log.error("搜索日志失败", e);
            return PageResult.empty((long) page, (long) size);
        }
    }

    /**
     * 解析时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return LocalDateTime.now().minusDays(7); // 默认7天前
        }

        try {
            return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("时间格式解析失败: {}, 使用默认时间", dateTimeStr);
            return LocalDateTime.now().minusDays(7);
        }
    }
}
