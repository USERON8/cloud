package com.cloud.log.service;

import com.cloud.log.domain.document.StockEventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 库存事件服务接口
 * 负责库存事件的存储和查询
 *
 * @author cloud
 * @date 2024-01-20
 */
public interface StockEventService {

    /**
     * 保存库存事件
     */
    void save(StockEventDocument document);

    /**
     * 检查事件是否已存在（幂等性检查）
     */
    boolean existsByEventId(String eventId);

    /**
     * 根据ID查询库存事件
     */
    Optional<StockEventDocument> findById(String id);

    /**
     * 根据商品ID查询所有事件
     */
    List<StockEventDocument> findByProductId(Long productId);

    /**
     * 根据仓库ID查询事件
     */
    Page<StockEventDocument> findByWarehouseId(Long warehouseId, Pageable pageable);

    /**
     * 根据事件类型查询
     */
    Page<StockEventDocument> findByEventType(String eventType, Pageable pageable);

    /**
     * 根据时间范围查询
     */
    Page<StockEventDocument> findByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据订单ID查询
     */
    Page<StockEventDocument> findByOrderId(Long orderId, Pageable pageable);

    /**
     * 根据变更类型查询
     */
    Page<StockEventDocument> findByChangeType(Integer changeType, Pageable pageable);

    /**
     * 根据业务类型查询
     */
    Page<StockEventDocument> findByBusinessType(Integer businessType, Pageable pageable);

    /**
     * 根据操作人查询
     */
    Page<StockEventDocument> findByOperatorId(Long operatorId, Pageable pageable);

    /**
     * 根据TraceId查询
     */
    Optional<StockEventDocument> findByTraceId(String traceId);

    /**
     * 统计指定时间范围内的事件数量
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
     * 删除过期的事件记录
     */
    void deleteExpiredEvents(LocalDateTime expiredTime);
}
