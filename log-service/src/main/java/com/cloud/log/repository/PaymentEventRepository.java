package com.cloud.log.repository;

import com.cloud.log.domain.document.PaymentEventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付事件Repository接口
 * 基于Spring Data Elasticsearch标准实现
 * 按照阿里巴巴官方示例标准设计
 *
 * @author cloud
 * @date 2025/1/15
 */
@Repository
public interface PaymentEventRepository extends ElasticsearchRepository<PaymentEventDocument, String> {

    /**
     * 根据支付ID查询所有事件（按事件时间倒序）
     */
    List<PaymentEventDocument> findByPaymentIdOrderByEventTimeDesc(String paymentId);

    /**
     * 根据订单ID查询支付事件（按事件时间倒序）
     */
    List<PaymentEventDocument> findByOrderIdOrderByEventTimeDesc(Long orderId);

    /**
     * 根据用户ID查询支付事件（分页，按事件时间倒序）
     */
    Page<PaymentEventDocument> findByUserIdOrderByEventTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据事件类型查询（分页，按事件时间倒序）
     */
    Page<PaymentEventDocument> findByEventTypeOrderByEventTimeDesc(String eventType, Pageable pageable);

    /**
     * 根据时间范围查询事件（分页，按事件时间倒序）
     */
    Page<PaymentEventDocument> findByEventTimeBetweenOrderByEventTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据支付方式查询事件（分页，按事件时间倒序）
     */
    Page<PaymentEventDocument> findByPaymentMethodOrderByEventTimeDesc(Integer paymentMethod, Pageable pageable);

    /**
     * 根据支付状态查询事件（分页，按事件时间倒序）
     */
    Page<PaymentEventDocument> findByPaymentStatusOrderByEventTimeDesc(Integer paymentStatus, Pageable pageable);

    /**
     * 根据支付ID和事件类型查询（按事件时间倒序）
     */
    List<PaymentEventDocument> findByPaymentIdAndEventTypeOrderByEventTimeDesc(Long paymentId, String eventType);

    /**
     * 根据TraceId查询事件
     */
    Optional<PaymentEventDocument> findByTraceId(String traceId);

    /**
     * 检查TraceId是否存在
     */
    boolean existsByTraceId(String traceId);

    /**
     * 检查支付ID和事件类型的组合是否存在
     */
    boolean existsByPaymentIdAndEventType(String paymentId, String eventType);

    /**
     * 统计时间范围内的事件数量
     */
    long countByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定用户的支付事件数量
     */
    long countByUserId(Long userId);

    /**
     * 统计指定支付ID的事件数量
     */
    long countByPaymentId(Long paymentId);

    /**
     * 统计指定订单ID的支付事件数量
     */
    long countByOrderId(Long orderId);

    /**
     * 统计指定事件类型的数量
     */
    long countByEventType(String eventType);

    /**
     * 统计指定支付方式的事件数量
     */
    long countByPaymentMethod(Integer paymentMethod);

    /**
     * 统计指定支付状态的事件数量
     */
    long countByPaymentStatus(Integer paymentStatus);

    /**
     * 统计指定支付渠道的事件数量
     */
    long countByPaymentChannel(String paymentChannel);

    /**
     * 删除指定时间之前的事件
     */
    long deleteByEventTimeBefore(LocalDateTime expiredTime);

    /**
     * 根据支付渠道查询事件
     */
    Page<PaymentEventDocument> findByPaymentChannelOrderByEventTimeDesc(String paymentChannel, Pageable pageable);

    /**
     * 根据业务类型查询事件
     */
    Page<PaymentEventDocument> findByBusinessTypeOrderByEventTimeDesc(Integer businessType, Pageable pageable);

    /**
     * 根据支付来源查询事件
     */
    Page<PaymentEventDocument> findByPaymentSourceOrderByEventTimeDesc(Integer paymentSource, Pageable pageable);

    /**
     * 根据操作人ID查询事件
     */
    Page<PaymentEventDocument> findByOperatorIdOrderByEventTimeDesc(Long operatorId, Pageable pageable);
}
