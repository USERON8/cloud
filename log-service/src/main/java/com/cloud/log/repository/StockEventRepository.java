package com.cloud.log.repository;

import com.cloud.log.domain.document.StockEventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 库存事件Repository接口
 * 基于Spring Data Elasticsearch标准实现
 *
 * @author cloud
 * @date 2024-01-20
 */
@Repository
public interface StockEventRepository extends ElasticsearchRepository<StockEventDocument, String> {

    /**
     * 根据商品ID查询所有事件（按事件时间倒序）
     */
    List<StockEventDocument> findByProductIdOrderByEventTimeDesc(Long productId);

    /**
     * 根据仓库ID查询事件（分页，按事件时间倒序）
     */
    Page<StockEventDocument> findByWarehouseIdOrderByEventTimeDesc(Long warehouseId, Pageable pageable);

    /**
     * 根据事件类型查询（分页，按事件时间倒序）
     */
    Page<StockEventDocument> findByEventTypeOrderByEventTimeDesc(String eventType, Pageable pageable);

    /**
     * 根据时间范围查询事件（分页，按事件时间倒序）
     */
    Page<StockEventDocument> findByEventTimeBetweenOrderByEventTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据订单ID查询事件（分页，按事件时间倒序）
     */
    Page<StockEventDocument> findByOrderIdOrderByEventTimeDesc(Long orderId, Pageable pageable);

    /**
     * 根据变更类型查询事件（分页，按事件时间倒序）
     */
    Page<StockEventDocument> findByChangeTypeOrderByEventTimeDesc(Integer changeType, Pageable pageable);

    /**
     * 根据业务类型查询事件（分页，按事件时间倒序）
     */
    Page<StockEventDocument> findByBusinessTypeOrderByEventTimeDesc(Integer businessType, Pageable pageable);

    /**
     * 根据操作人ID查询事件（分页，按事件时间倒序）
     */
    Page<StockEventDocument> findByOperatorIdOrderByEventTimeDesc(Long operatorId, Pageable pageable);

    /**
     * 根据TraceId查询事件
     */
    Optional<StockEventDocument> findByTraceId(String traceId);

    /**
     * 检查TraceId是否存在
     */
    boolean existsByTraceId(String traceId);

    /**
     * 根据商品ID和事件类型查询（按事件时间倒序）
     */
    List<StockEventDocument> findByProductIdAndEventTypeOrderByEventTimeDesc(Long productId, String eventType);

    /**
     * 统计时间范围内的事件数量
     */
    long countByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定商品的事件数量
     */
    long countByProductId(Long productId);

    /**
     * 统计指定事件类型的数量
     */
    long countByEventType(String eventType);

    /**
     * 统计指定仓库的事件数量
     */
    long countByWarehouseId(Long warehouseId);

    /**
     * 统计指定订单的事件数量
     */
    long countByOrderId(Long orderId);

    /**
     * 统计指定变更类型的事件数量
     */
    long countByChangeType(Integer changeType);

    /**
     * 统计指定业务类型的事件数量
     */
    long countByBusinessType(Integer businessType);

    /**
     * 统计指定操作人的事件数量
     */
    long countByOperatorId(Long operatorId);

    /**
     * 删除指定时间之前的事件
     */
    long deleteByEventTimeBefore(LocalDateTime expiredTime);

    /**
     * 根据供应商ID查询事件
     */
    Page<StockEventDocument> findBySupplierIdOrderByEventTimeDesc(Long supplierId, Pageable pageable);

    /**
     * 根据客户ID查询事件
     */
    Page<StockEventDocument> findByCustomerIdOrderByEventTimeDesc(Long customerId, Pageable pageable);
}
