package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.PageResult;
import com.cloud.order.dto.RefundCreateDTO;
import com.cloud.order.dto.RefundPageDTO;
import com.cloud.order.enums.OrderRefundStatusEnum;
import com.cloud.order.enums.OrderStatusEnum;
import com.cloud.order.enums.RefundStatusEnum;
import com.cloud.order.mapper.RefundMapper;
import com.cloud.order.messaging.RefundMessageProducer;
import com.cloud.order.module.entity.Order;
import com.cloud.order.module.entity.Refund;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.RefundService;
import com.cloud.order.vo.RefundVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundMapper refundMapper;
    private final OrderService orderService;
    private final RefundMessageProducer refundMessageProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRefund(Long userId, RefundCreateDTO dto) {
        Order order = orderService.getById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException("Order not found");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("Current user is not the order owner");
        }

        OrderStatusEnum orderStatus = order.getStatusEnum();
        if (orderStatus != OrderStatusEnum.PAID
                && orderStatus != OrderStatusEnum.SHIPPED
                && orderStatus != OrderStatusEnum.COMPLETED) {
            throw new BusinessException("Current order status does not support refund");
        }

        Refund existingRefund = getRefundByOrderId(dto.getOrderId());
        if (existingRefund != null && !existingRefund.isCompleted()) {
            throw new BusinessException("An unfinished refund already exists for this order");
        }

        if (dto.getRefundAmount().compareTo(order.getPayAmount()) > 0) {
            throw new BusinessException("Refund amount cannot exceed paid amount");
        }

        Refund refund = new Refund();
        refund.setRefundNo(Refund.generateRefundNo());
        refund.setOrderId(dto.getOrderId());
        refund.setOrderNo(StringUtils.hasText(dto.getOrderNo()) ? dto.getOrderNo() : order.getOrderNo());
        refund.setUserId(userId);
        refund.setMerchantId(order.getShopId());
        refund.setRefundType(dto.getRefundType());
        refund.setRefundReason(dto.getRefundReason());
        refund.setRefundDescription(dto.getRefundDescription());
        refund.setRefundAmount(dto.getRefundAmount());
        refund.setRefundQuantity(dto.getRefundQuantity() != null ? dto.getRefundQuantity() : 1);
        refund.setStatus(RefundStatusEnum.PENDING_AUDIT.getCode());
        refund.setCreatedAt(LocalDateTime.now());
        refund.setUpdatedAt(LocalDateTime.now());
        refund.setIsDeleted(0);

        refundMapper.insert(refund);

        order.setRefundStatus(OrderRefundStatusEnum.REFUND_APPLYING.getCode());
        order.setUpdatedAt(LocalDateTime.now());
        orderService.updateById(order);

        refundMessageProducer.sendRefundCreatedEvent(refund);
        return refund.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean auditRefund(Long refundId, Long merchantId, Boolean approved, String auditRemark) {
        Refund refund = getRefundById(refundId);
        if (refund == null) {
            throw new BusinessException("Refund not found");
        }
        if (!refund.getMerchantId().equals(merchantId)) {
            throw new BusinessException("Current merchant has no permission for this refund");
        }
        if (!refund.isPendingAudit()) {
            throw new BusinessException("Current refund status does not allow audit");
        }

        refund.setStatus(Boolean.TRUE.equals(approved)
                ? RefundStatusEnum.AUDIT_PASSED.getCode()
                : RefundStatusEnum.AUDIT_REJECTED.getCode());
        refund.setAuditTime(LocalDateTime.now());
        refund.setAuditRemark(auditRemark);
        refund.setUpdatedAt(LocalDateTime.now());

        int rows = refundMapper.updateById(refund);
        if (rows <= 0) {
            return false;
        }

        if (Boolean.TRUE.equals(approved) && refund.isRefundOnly()) {
            processRefund(refundId);
        } else if (!Boolean.TRUE.equals(approved)) {
            Order order = orderService.getById(refund.getOrderId());
            if (order != null) {
                order.setRefundStatus(OrderRefundStatusEnum.REFUND_FAILED.getCode());
                orderService.updateById(order);
            }
        }

        refundMessageProducer.sendRefundAuditedEvent(refund, approved);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelRefund(Long refundId, Long userId) {
        Refund refund = getRefundById(refundId);
        if (refund == null) {
            throw new BusinessException("Refund not found");
        }
        if (!refund.getUserId().equals(userId)) {
            throw new BusinessException("Current user has no permission for this refund");
        }
        if (!refund.canCancel()) {
            throw new BusinessException("Current refund status does not allow cancellation");
        }

        refund.setStatus(RefundStatusEnum.CANCELLED.getCode());
        refund.setUpdatedAt(LocalDateTime.now());
        int rows = refundMapper.updateById(refund);
        if (rows <= 0) {
            return false;
        }

        Order order = orderService.getById(refund.getOrderId());
        if (order != null) {
            order.setRefundStatus(OrderRefundStatusEnum.REFUND_CLOSED.getCode());
            orderService.updateById(order);
        }

        refundMessageProducer.sendRefundCancelledEvent(refund);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean processRefund(Long refundId) {
        Refund refund = getRefundById(refundId);
        if (refund == null) {
            throw new BusinessException("Refund not found");
        }
        if (!refund.isAuditPassed() && !RefundStatusEnum.GOODS_RECEIVED.getCode().equals(refund.getStatus())) {
            throw new BusinessException("Current refund status does not allow processing");
        }

        refund.setStatus(RefundStatusEnum.REFUNDING.getCode());
        refund.setUpdatedAt(LocalDateTime.now());
        refundMapper.updateById(refund);

        Order order = orderService.getById(refund.getOrderId());
        if (order != null) {
            order.setRefundStatus(OrderRefundStatusEnum.REFUNDING.getCode());
            order.setUpdatedAt(LocalDateTime.now());
            orderService.updateById(order);
        }

        boolean sent = refundMessageProducer.sendRefundProcessEvent(refund);
        if (!sent) {
            log.error("Send refund process message failed, refundId={}", refundId);
        }
        return sent;
    }

    @Override
    public Refund getRefundById(Long refundId) {
        return refundMapper.selectById(refundId);
    }

    @Override
    public Refund getRefundByOrderId(Long orderId) {
        LambdaQueryWrapper<Refund> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Refund::getOrderId, orderId)
                .eq(Refund::getIsDeleted, 0)
                .orderByDesc(Refund::getCreatedAt)
                .last("LIMIT 1");
        return refundMapper.selectOne(wrapper);
    }

    @Override
    public PageResult<RefundVO> pageQuery(RefundPageDTO pageDTO) {
        LambdaQueryWrapper<Refund> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Refund::getIsDeleted, 0);

        if (pageDTO.getStatus() != null) {
            wrapper.eq(Refund::getStatus, pageDTO.getStatus());
        }
        if (pageDTO.getUserId() != null) {
            wrapper.eq(Refund::getUserId, pageDTO.getUserId());
        }
        if (pageDTO.getMerchantId() != null) {
            wrapper.eq(Refund::getMerchantId, pageDTO.getMerchantId());
        }
        if (StringUtils.hasText(pageDTO.getOrderNo())) {
            wrapper.eq(Refund::getOrderNo, pageDTO.getOrderNo());
        }
        if (StringUtils.hasText(pageDTO.getRefundNo())) {
            wrapper.eq(Refund::getRefundNo, pageDTO.getRefundNo());
        }
        if (pageDTO.getRefundType() != null) {
            wrapper.eq(Refund::getRefundType, pageDTO.getRefundType());
        }
        if (StringUtils.hasText(pageDTO.getStartDate())) {
            wrapper.ge(Refund::getCreatedAt, pageDTO.getStartDate() + " 00:00:00");
        }
        if (StringUtils.hasText(pageDTO.getEndDate())) {
            wrapper.le(Refund::getCreatedAt, pageDTO.getEndDate() + " 23:59:59");
        }

        if ("refund_amount".equals(pageDTO.getSortField())) {
            if ("asc".equalsIgnoreCase(pageDTO.getSortOrder())) {
                wrapper.orderByAsc(Refund::getRefundAmount);
            } else {
                wrapper.orderByDesc(Refund::getRefundAmount);
            }
        } else {
            if ("asc".equalsIgnoreCase(pageDTO.getSortOrder())) {
                wrapper.orderByAsc(Refund::getCreatedAt);
            } else {
                wrapper.orderByDesc(Refund::getCreatedAt);
            }
        }

        Page<Refund> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        Page<Refund> resultPage = refundMapper.selectPage(page, wrapper);
        List<RefundVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), voList);
    }

    private RefundVO convertToVO(Refund refund) {
        RefundVO vo = new RefundVO();
        vo.setId(refund.getId());
        vo.setRefundNo(refund.getRefundNo());
        vo.setOrderId(refund.getOrderId());
        vo.setOrderNo(refund.getOrderNo());
        vo.setUserId(refund.getUserId());
        vo.setMerchantId(refund.getMerchantId());
        vo.setRefundType(refund.getRefundType());
        vo.setRefundTypeName(refund.isRefundOnly() ? "Refund only" : "Return and refund");
        vo.setRefundReason(refund.getRefundReason());
        vo.setRefundDescription(refund.getRefundDescription());
        vo.setRefundAmount(refund.getRefundAmount());
        vo.setRefundQuantity(refund.getRefundQuantity());
        vo.setStatus(refund.getStatus());

        RefundStatusEnum statusEnum = RefundStatusEnum.fromCode(refund.getStatus());
        vo.setStatusName(statusEnum == null ? "Unknown" : statusEnum.getName());

        vo.setAuditTime(refund.getAuditTime());
        vo.setAuditRemark(refund.getAuditRemark());
        vo.setLogisticsCompany(refund.getLogisticsCompany());
        vo.setLogisticsNo(refund.getLogisticsNo());
        vo.setRefundTime(refund.getRefundTime());
        vo.setRefundChannel(refund.getRefundChannel());
        vo.setRefundTransactionNo(refund.getRefundTransactionNo());
        vo.setCreatedAt(refund.getCreatedAt());
        vo.setUpdatedAt(refund.getUpdatedAt());
        return vo;
    }
}
