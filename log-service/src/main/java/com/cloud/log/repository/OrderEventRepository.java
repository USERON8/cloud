package com.cloud.log.repository;

import com.cloud.log.domain.document.OrderEventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单事件Repository接口
 * 基于Spring Data Elasticsearch标准实现
 * 按照阿里巴巴官方示例标准设计
 *
 * @author cloud
 * @date 2025/1/15
 */
@Repository
public interface OrderEventRepository extends ElasticsearchRepository<OrderEventDocument, String> {

    /**
     * 根据订单ID查询所有事件（按事件时间倒序）
     */
    List<OrderEventDocument> findByOrderIdOrderByEventTimeDesc(Long orderId);

    /**
     * 根据用户ID查询事件（分页，按事件时间倒序）
     */
    Page<OrderEventDocument> findByUserIdOrderByEventTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据事件类型查询（分页，按事件时间倒序）
     */
    Page<OrderEventDocument> findByEventTypeOrderByEventTimeDesc(String eventType, Pageable pageable);

    /**
     * 根据时间范围查询事件（分页，按事件时间倒序）
     */
    Page<OrderEventDocument> findByEventTimeBetweenOrderByEventTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据订单ID和事件类型查询（按事件时间倒序）
     */
    List<OrderEventDocument> findByOrderIdAndEventTypeOrderByEventTimeDesc(Long orderId, String eventType);

    /**
     * 根据TraceId查询事件
     */
    Optional<OrderEventDocument> findByTraceId(String traceId);

    /**
     * 检查TraceId是否存在
     */
    boolean existsByTraceId(String traceId);

    /**
     * 检查订单ID和事件类型的组合是否存在
     */
    boolean existsByOrderIdAndEventType(Long orderId, String eventType);

    /**
     * 统计时间范围内的事件数量
     */
    long countByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定用户的事件数量
     */
    long countByUserId(Long userId);

    /**
     * 统计指定订单的事件数量
     */
    long countByOrderId(Long orderId);

    /**
     * 统计指定事件类型的数量
     */
    long countByEventType(String eventType);

    /**
     * 删除指定时间之前的事件
     */
    long deleteByEventTimeBefore(LocalDateTime expiredTime);

    /**
     * 根据订单状态查询事件
     */
    Page<OrderEventDocument> findByOrderStatusOrderByEventTimeDesc(Integer orderStatus, Pageable pageable);

    /**
     * 根据支付方式查询事件
     */
    Page<OrderEventDocument> findByPaymentMethodOrderByEventTimeDesc(Integer paymentMethod, Pageable pageable);

    /**
     * 根据订单来源查询事件
     */
    Page<OrderEventDocument> findByOrderSourceOrderByEventTimeDesc(Integer orderSource, Pageable pageable);

    /**
     * 根据操作人ID查询事件
     */
    Page<OrderEventDocument> findByOperatorIdOrderByEventTimeDesc(Long operatorId, Pageable pageable);
}
