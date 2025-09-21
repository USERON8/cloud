package com.cloud.log.service.impl;

import com.cloud.log.domain.document.PaymentEventDocument;
import com.cloud.log.repository.PaymentEventRepository;
import com.cloud.log.service.PaymentEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付事件服务实现类
 * 负责支付事件的存储和查询
 * 基于阿里巴巴官方示例标准实现
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventServiceImpl implements PaymentEventService {

    private final PaymentEventRepository paymentEventRepository;

    @Override
    public void savePaymentEvent(PaymentEventDocument document) {
        try {
            paymentEventRepository.save(document);
            log.debug("支付事件保存成功 - 支付ID: {}, 事件类型: {}, 文档ID: {}",
                    document.getPaymentId(), document.getEventType(), document.getId());
        } catch (Exception e) {
            log.error("保存支付事件失败 - 支付ID: {}, 事件类型: {}, 错误: {}",
                    document.getPaymentId(), document.getEventType(), e.getMessage(), e);
            throw new RuntimeException("保存支付事件失败", e);
        }
    }

    @Override
    public boolean existsByPaymentIdAndEventType(String paymentId, String eventType, String traceId) {
        try {
            // 优先使用traceId检查，因为它更唯一
            if (traceId != null) {
                return paymentEventRepository.existsByTraceId(traceId);
            }
            // 备用检查方案
            return paymentEventRepository.existsByPaymentIdAndEventType(paymentId, eventType);
        } catch (Exception e) {
            log.error("检查支付事件是否存在时发生异常 - 支付ID: {}, 事件类型: {}, TraceId: {}, 错误: {}",
                    paymentId, eventType, traceId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Optional<PaymentEventDocument> findById(String id) {
        try {
            return paymentEventRepository.findById(id);
        } catch (Exception e) {
            log.error("根据ID查询支付事件失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public List<PaymentEventDocument> findByPaymentId(String paymentId) {
        try {
            return paymentEventRepository.findByPaymentIdOrderByEventTimeDesc(paymentId);
        } catch (Exception e) {
            log.error("根据支付ID查询事件失败 - 支付ID: {}, 错误: {}", paymentId, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<PaymentEventDocument> findByOrderId(Long orderId) {
        try {
            return paymentEventRepository.findByOrderIdOrderByEventTimeDesc(orderId);
        } catch (Exception e) {
            log.error("根据订单ID查询支付事件失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public Page<PaymentEventDocument> findByUserId(Long userId, Pageable pageable) {
        try {
            return paymentEventRepository.findByUserIdOrderByEventTimeDesc(userId, pageable);
        } catch (Exception e) {
            log.error("根据用户ID查询支付事件失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<PaymentEventDocument> findByEventType(String eventType, Pageable pageable) {
        try {
            return paymentEventRepository.findByEventTypeOrderByEventTimeDesc(eventType, pageable);
        } catch (Exception e) {
            log.error("根据事件类型查询失败 - 事件类型: {}, 错误: {}", eventType, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<PaymentEventDocument> findByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        try {
            return paymentEventRepository.findByEventTimeBetweenOrderByEventTimeDesc(startTime, endTime, pageable);
        } catch (Exception e) {
            log.error("根据时间范围查询支付事件失败 - 开始时间: {}, 结束时间: {}, 错误: {}",
                    startTime, endTime, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<PaymentEventDocument> findByPaymentMethod(Integer paymentMethod, Pageable pageable) {
        try {
            return paymentEventRepository.findByPaymentMethodOrderByEventTimeDesc(paymentMethod, pageable);
        } catch (Exception e) {
            log.error("根据支付方式查询事件失败 - 支付方式: {}, 错误: {}", paymentMethod, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<PaymentEventDocument> findByPaymentStatus(Integer paymentStatus, Pageable pageable) {
        try {
            return paymentEventRepository.findByPaymentStatusOrderByEventTimeDesc(paymentStatus, pageable);
        } catch (Exception e) {
            log.error("根据支付状态查询事件失败 - 支付状态: {}, 错误: {}", paymentStatus, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Optional<PaymentEventDocument> findByTraceId(String traceId) {
        try {
            return paymentEventRepository.findByTraceId(traceId);
        } catch (Exception e) {
            log.error("根据TraceId查询支付事件失败 - TraceId: {}, 错误: {}", traceId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public long countByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return paymentEventRepository.countByEventTimeBetween(startTime, endTime);
        } catch (Exception e) {
            log.error("统计时间范围内支付事件数量失败 - 开始时间: {}, 结束时间: {}, 错误: {}",
                    startTime, endTime, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByUserId(Long userId) {
        try {
            return paymentEventRepository.countByUserId(userId);
        } catch (Exception e) {
            log.error("统计用户支付事件数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByPaymentMethod(Integer paymentMethod) {
        try {
            return paymentEventRepository.countByPaymentMethod(paymentMethod);
        } catch (Exception e) {
            log.error("统计支付方式事件数量失败 - 支付方式: {}, 错误: {}", paymentMethod, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByPaymentStatus(Integer paymentStatus) {
        try {
            return paymentEventRepository.countByPaymentStatus(paymentStatus);
        } catch (Exception e) {
            log.error("统计支付状态事件数量失败 - 支付状态: {}, 错误: {}", paymentStatus, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public void deleteExpiredEvents(LocalDateTime expiredTime) {
        try {
            long count = paymentEventRepository.deleteByEventTimeBefore(expiredTime);
            log.info("删除过期支付事件完成 - 删除数量: {}, 过期时间: {}", count, expiredTime);
        } catch (Exception e) {
            log.error("删除过期支付事件失败 - 过期时间: {}, 错误: {}", expiredTime, e.getMessage(), e);
            throw new RuntimeException("删除过期支付事件失败", e);
        }
    }
}
