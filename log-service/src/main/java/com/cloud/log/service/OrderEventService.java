package com.cloud.log.service;

import com.cloud.log.domain.document.OrderEventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单事件服务接口
 * 负责订单事件的存储和查询
 * 基于阿里巴巴官方示例标准设计
 *
 * @author cloud
 * @date 2025/1/15
 */
public interface OrderEventService {

    /**
     * 保存订单事件
     */
    void saveOrderEvent(OrderEventDocument document);

    /**
     * 检查订单事件是否已存在（幂等性检查）
     */
    boolean existsByOrderIdAndEventType(Long orderId, String eventType, String traceId);

    /**
     * 根据ID查询订单事件
     */
    Optional<OrderEventDocument> findById(String id);

    /**
     * 根据订单ID查询所有事件
     */
    List<OrderEventDocument> findByOrderId(Long orderId);

    /**
     * 根据用户ID查询订单事件
     */
    Page<OrderEventDocument> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据事件类型查询
     */
    Page<OrderEventDocument> findByEventType(String eventType, Pageable pageable);

    /**
     * 根据时间范围查询
     */
    Page<OrderEventDocument> findByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据订单ID和事件类型查询
     */
    List<OrderEventDocument> findByOrderIdAndEventType(Long orderId, String eventType);

    /**
     * 根据TraceId查询
     */
    Optional<OrderEventDocument> findByTraceId(String traceId);

    /**
     * 统计指定时间范围内的事件数量
     */
    long countByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定用户的事件数量
     */
    long countByUserId(Long userId);

    /**
     * 删除过期的事件记录
     */
    void deleteExpiredEvents(LocalDateTime expiredTime);
}
