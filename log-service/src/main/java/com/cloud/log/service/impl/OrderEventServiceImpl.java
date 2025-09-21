package com.cloud.log.service.impl;

import com.cloud.log.domain.document.OrderEventDocument;
import com.cloud.log.repository.OrderEventRepository;
import com.cloud.log.service.OrderEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单事件服务实现类
 * 负责订单事件的存储和查询
 * 基于阿里巴巴官方示例标准实现
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventServiceImpl implements OrderEventService {

    private final OrderEventRepository orderEventRepository;

    @Override
    public void saveOrderEvent(OrderEventDocument document) {
        try {
            orderEventRepository.save(document);
            log.debug("订单事件保存成功 - 订单ID: {}, 事件类型: {}, 文档ID: {}",
                    document.getOrderId(), document.getEventType(), document.getId());
        } catch (Exception e) {
            log.error("保存订单事件失败 - 订单ID: {}, 事件类型: {}, 错误: {}",
                    document.getOrderId(), document.getEventType(), e.getMessage(), e);
            throw new RuntimeException("保存订单事件失败", e);
        }
    }

    @Override
    public boolean existsByOrderIdAndEventType(Long orderId, String eventType, String traceId) {
        try {
            // 优先使用traceId检查，因为它更唯一
            if (traceId != null) {
                return orderEventRepository.existsByTraceId(traceId);
            }
            // 备用检查方案
            return orderEventRepository.existsByOrderIdAndEventType(orderId, eventType);
        } catch (Exception e) {
            log.error("检查订单事件是否存在时发生异常 - 订单ID: {}, 事件类型: {}, TraceId: {}, 错误: {}",
                    orderId, eventType, traceId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Optional<OrderEventDocument> findById(String id) {
        try {
            return orderEventRepository.findById(id);
        } catch (Exception e) {
            log.error("根据ID查询订单事件失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public List<OrderEventDocument> findByOrderId(Long orderId) {
        try {
            return orderEventRepository.findByOrderIdOrderByEventTimeDesc(orderId);
        } catch (Exception e) {
            log.error("根据订单ID查询事件失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public Page<OrderEventDocument> findByUserId(Long userId, Pageable pageable) {
        try {
            return orderEventRepository.findByUserIdOrderByEventTimeDesc(userId, pageable);
        } catch (Exception e) {
            log.error("根据用户ID查询事件失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<OrderEventDocument> findByEventType(String eventType, Pageable pageable) {
        try {
            return orderEventRepository.findByEventTypeOrderByEventTimeDesc(eventType, pageable);
        } catch (Exception e) {
            log.error("根据事件类型查询失败 - 事件类型: {}, 错误: {}", eventType, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<OrderEventDocument> findByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        try {
            return orderEventRepository.findByEventTimeBetweenOrderByEventTimeDesc(startTime, endTime, pageable);
        } catch (Exception e) {
            log.error("根据时间范围查询事件失败 - 开始时间: {}, 结束时间: {}, 错误: {}",
                    startTime, endTime, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public List<OrderEventDocument> findByOrderIdAndEventType(Long orderId, String eventType) {
        try {
            return orderEventRepository.findByOrderIdAndEventTypeOrderByEventTimeDesc(orderId, eventType);
        } catch (Exception e) {
            log.error("根据订单ID和事件类型查询失败 - 订单ID: {}, 事件类型: {}, 错误: {}",
                    orderId, eventType, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public Optional<OrderEventDocument> findByTraceId(String traceId) {
        try {
            return orderEventRepository.findByTraceId(traceId);
        } catch (Exception e) {
            log.error("根据TraceId查询事件失败 - TraceId: {}, 错误: {}", traceId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public long countByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return orderEventRepository.countByEventTimeBetween(startTime, endTime);
        } catch (Exception e) {
            log.error("统计时间范围内事件数量失败 - 开始时间: {}, 结束时间: {}, 错误: {}",
                    startTime, endTime, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByUserId(Long userId) {
        try {
            return orderEventRepository.countByUserId(userId);
        } catch (Exception e) {
            log.error("统计用户事件数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public void deleteExpiredEvents(LocalDateTime expiredTime) {
        try {
            long count = orderEventRepository.deleteByEventTimeBefore(expiredTime);
            log.info("删除过期订单事件完成 - 删除数量: {}, 过期时间: {}", count, expiredTime);
        } catch (Exception e) {
            log.error("删除过期事件失败 - 过期时间: {}, 错误: {}", expiredTime, e.getMessage(), e);
            throw new RuntimeException("删除过期事件失败", e);
        }
    }
}
