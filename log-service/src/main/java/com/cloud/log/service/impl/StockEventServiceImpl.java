package com.cloud.log.service.impl;

import com.cloud.log.domain.document.StockEventDocument;
import com.cloud.log.repository.StockEventRepository;
import com.cloud.log.service.StockEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 库存事件服务实现类
 * 负责库存事件的存储和查询
 *
 * @author cloud
 * @date 2024-01-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockEventServiceImpl implements StockEventService {

    private final StockEventRepository stockEventRepository;

    @Override
    public void save(StockEventDocument document) {
        try {
            stockEventRepository.save(document);
            log.debug("库存事件保存成功 - 商品ID: {}, 事件类型: {}, 文档ID: {}",
                    document.getProductId(), document.getEventType(), document.getId());
        } catch (Exception e) {
            log.error("保存库存事件失败 - 商品ID: {}, 事件类型: {}, 错误: {}",
                    document.getProductId(), document.getEventType(), e.getMessage(), e);
            throw new RuntimeException("保存库存事件失败", e);
        }
    }

    @Override
    public boolean existsByEventId(String eventId) {
        try {
            return stockEventRepository.existsById(eventId);
        } catch (Exception e) {
            log.error("检查库存事件是否存在时发生异常 - EventId: {}, 错误: {}",
                    eventId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Optional<StockEventDocument> findById(String id) {
        try {
            return stockEventRepository.findById(id);
        } catch (Exception e) {
            log.error("根据ID查询库存事件失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public List<StockEventDocument> findByProductId(Long productId) {
        try {
            return stockEventRepository.findByProductIdOrderByEventTimeDesc(productId);
        } catch (Exception e) {
            log.error("根据商品ID查询事件失败 - 商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public Page<StockEventDocument> findByWarehouseId(Long warehouseId, Pageable pageable) {
        try {
            return stockEventRepository.findByWarehouseIdOrderByEventTimeDesc(warehouseId, pageable);
        } catch (Exception e) {
            log.error("根据仓库ID查询事件失败 - 仓库ID: {}, 错误: {}", warehouseId, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<StockEventDocument> findByEventType(String eventType, Pageable pageable) {
        try {
            return stockEventRepository.findByEventTypeOrderByEventTimeDesc(eventType, pageable);
        } catch (Exception e) {
            log.error("根据事件类型查询失败 - 事件类型: {}, 错误: {}", eventType, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<StockEventDocument> findByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        try {
            return stockEventRepository.findByEventTimeBetweenOrderByEventTimeDesc(startTime, endTime, pageable);
        } catch (Exception e) {
            log.error("根据时间范围查询库存事件失败 - 开始时间: {}, 结束时间: {}, 错误: {}",
                    startTime, endTime, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<StockEventDocument> findByOrderId(Long orderId, Pageable pageable) {
        try {
            return stockEventRepository.findByOrderIdOrderByEventTimeDesc(orderId, pageable);
        } catch (Exception e) {
            log.error("根据订单ID查询事件失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<StockEventDocument> findByChangeType(Integer changeType, Pageable pageable) {
        try {
            return stockEventRepository.findByChangeTypeOrderByEventTimeDesc(changeType, pageable);
        } catch (Exception e) {
            log.error("根据变更类型查询事件失败 - 变更类型: {}, 错误: {}", changeType, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<StockEventDocument> findByBusinessType(Integer businessType, Pageable pageable) {
        try {
            return stockEventRepository.findByBusinessTypeOrderByEventTimeDesc(businessType, pageable);
        } catch (Exception e) {
            log.error("根据业务类型查询事件失败 - 业务类型: {}, 错误: {}", businessType, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<StockEventDocument> findByOperatorId(Long operatorId, Pageable pageable) {
        try {
            return stockEventRepository.findByOperatorIdOrderByEventTimeDesc(operatorId, pageable);
        } catch (Exception e) {
            log.error("根据操作人查询事件失败 - 操作人ID: {}, 错误: {}", operatorId, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Optional<StockEventDocument> findByTraceId(String traceId) {
        try {
            return stockEventRepository.findByTraceId(traceId);
        } catch (Exception e) {
            log.error("根据TraceId查询库存事件失败 - TraceId: {}, 错误: {}", traceId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public long countByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return stockEventRepository.countByEventTimeBetween(startTime, endTime);
        } catch (Exception e) {
            log.error("统计时间范围内库存事件数量失败 - 开始时间: {}, 结束时间: {}, 错误: {}",
                    startTime, endTime, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByProductId(Long productId) {
        try {
            return stockEventRepository.countByProductId(productId);
        } catch (Exception e) {
            log.error("统计商品事件数量失败 - 商品ID: {}, 错误: {}", productId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByEventType(String eventType) {
        try {
            return stockEventRepository.countByEventType(eventType);
        } catch (Exception e) {
            log.error("统计事件类型数量失败 - 事件类型: {}, 错误: {}", eventType, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByWarehouseId(Long warehouseId) {
        try {
            return stockEventRepository.countByWarehouseId(warehouseId);
        } catch (Exception e) {
            log.error("统计仓库事件数量失败 - 仓库ID: {}, 错误: {}", warehouseId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public void deleteExpiredEvents(LocalDateTime expiredTime) {
        try {
            stockEventRepository.deleteByEventTimeBefore(expiredTime);
            log.info("删除过期库存事件完成 - 过期时间: {}", expiredTime);
        } catch (Exception e) {
            log.error("删除过期库存事件失败 - 过期时间: {}, 错误: {}", expiredTime, e.getMessage(), e);
        }
    }
}
