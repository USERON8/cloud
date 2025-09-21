package com.cloud.log.service;

import com.cloud.log.domain.document.PaymentEventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付事件服务接口
 * 负责支付事件的存储和查询
 * 基于阿里巴巴官方示例标准设计
 *
 * @author cloud
 * @date 2025/1/15
 */
public interface PaymentEventService {

    /**
     * 保存支付事件
     */
    void savePaymentEvent(PaymentEventDocument document);

    /**
     * 检查支付事件是否已存在（幂等性检查）
     */
    boolean existsByPaymentIdAndEventType(String paymentId, String eventType, String traceId);

    /**
     * 根据ID查询支付事件
     */
    Optional<PaymentEventDocument> findById(String id);

    /**
     * 根据支付ID查询所有事件
     */
    List<PaymentEventDocument> findByPaymentId(String paymentId);

    /**
     * 根据订单ID查询支付事件
     */
    List<PaymentEventDocument> findByOrderId(Long orderId);

    /**
     * 根据用户ID查询支付事件
     */
    Page<PaymentEventDocument> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据事件类型查询
     */
    Page<PaymentEventDocument> findByEventType(String eventType, Pageable pageable);

    /**
     * 根据时间范围查询
     */
    Page<PaymentEventDocument> findByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据支付方式查询
     */
    Page<PaymentEventDocument> findByPaymentMethod(Integer paymentMethod, Pageable pageable);

    /**
     * 根据支付状态查询
     */
    Page<PaymentEventDocument> findByPaymentStatus(Integer paymentStatus, Pageable pageable);

    /**
     * 根据TraceId查询
     */
    Optional<PaymentEventDocument> findByTraceId(String traceId);

    /**
     * 统计指定时间范围内的事件数量
     */
    long countByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定用户的事件数量
     */
    long countByUserId(Long userId);

    /**
     * 统计指定支付方式的事件数量
     */
    long countByPaymentMethod(Integer paymentMethod);

    /**
     * 统计指定支付状态的事件数量
     */
    long countByPaymentStatus(Integer paymentStatus);

    /**
     * 删除过期的事件记录
     */
    void deleteExpiredEvents(LocalDateTime expiredTime);
}
