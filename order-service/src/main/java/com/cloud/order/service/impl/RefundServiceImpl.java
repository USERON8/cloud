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

/**
 * 退款服务实现
 *
 * @author CloudDevAgent
 * @since 2025-01-15
 */
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
        log.info("📝 开始创建退款申请: userId={}, orderId={}, orderNo={}",
                userId, dto.getOrderId(), dto.getOrderNo());

        // 1. 验证订单
        Order order = orderService.getById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        // 2. 检查订单状态 - 只有已支付、已发货、已完成的订单可以退款
        OrderStatusEnum orderStatus = order.getStatusEnum();
        if (orderStatus != OrderStatusEnum.PAID &&
                orderStatus != OrderStatusEnum.SHIPPED &&
                orderStatus != OrderStatusEnum.COMPLETED) {
            throw new BusinessException("订单状态不允许退款,当前状态:" + orderStatus.getName());
        }

        // 3. 检查是否已有退款申请
        Refund existingRefund = getRefundByOrderId(dto.getOrderId());
        if (existingRefund != null && !existingRefund.isCompleted()) {
            throw new BusinessException("该订单已有退款申请在处理中");
        }

        // 4. 验证退款金额
        if (dto.getRefundAmount().compareTo(order.getPayAmount()) > 0) {
            throw new BusinessException("退款金额不能超过实付金额");
        }

        // 5. 创建退款单
        Refund refund = new Refund();
        refund.setRefundNo(Refund.generateRefundNo());
        refund.setOrderId(dto.getOrderId());
        refund.setOrderNo(dto.getOrderNo());
        refund.setUserId(userId);
        refund.setMerchantId(order.getShopId()); // 假设shopId就是merchantId
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

        log.info("✅ 退款申请创建成功: refundId={}, refundNo={}, amount={}",
                refund.getId(), refund.getRefundNo(), refund.getRefundAmount());

        // 6. 更新订单退款状态为"退款申请中"
        order.setRefundStatus(OrderRefundStatusEnum.REFUND_APPLYING.getCode());
        order.setUpdatedAt(LocalDateTime.now());
        orderService.updateById(order);

        // 7. 发送退款创建事件(通知商家)
        refundMessageProducer.sendRefundCreatedEvent(refund);

        return refund.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean auditRefund(Long refundId, Long merchantId, Boolean approved, String auditRemark) {
        log.info("🔍 开始审核退款: refundId={}, merchantId={}, approved={}",
                refundId, merchantId, approved);

        // 1. 查询退款单
        Refund refund = getRefundById(refundId);
        if (refund == null) {
            throw new BusinessException("退款单不存在");
        }

        // 2. 验证商家权限
        if (!refund.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权审核此退款单");
        }

        // 3. 验证退款状态
        if (!refund.isPendingAudit()) {
            throw new BusinessException("退款单状态不是待审核,无法审核");
        }

        // 4. 更新退款单状态
        refund.setStatus(approved ? RefundStatusEnum.AUDIT_PASSED.getCode() :
                RefundStatusEnum.AUDIT_REJECTED.getCode());
        refund.setAuditTime(LocalDateTime.now());
        refund.setAuditRemark(auditRemark);
        refund.setUpdatedAt(LocalDateTime.now());

        int rows = refundMapper.updateById(refund);

        if (rows > 0) {
            log.info("✅ 退款审核完成: refundId={}, approved={}", refundId, approved);

            // 5. 如果审核通过且是仅退款类型,直接处理退款
            if (approved && refund.isRefundOnly()) {
                processRefund(refundId);
            }

            // 6. 发送审核结果事件
            refundMessageProducer.sendRefundAuditedEvent(refund, approved);

            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelRefund(Long refundId, Long userId) {
        log.info("❌ 用户取消退款: refundId={}, userId={}", refundId, userId);

        // 1. 查询退款单
        Refund refund = getRefundById(refundId);
        if (refund == null) {
            throw new BusinessException("退款单不存在");
        }

        // 2. 验证用户权限
        if (!refund.getUserId().equals(userId)) {
            throw new BusinessException("无权取消此退款单");
        }

        // 3. 验证是否可以取消
        if (!refund.canCancel()) {
            throw new BusinessException("退款单当前状态不允许取消");
        }

        // 4. 更新状态
        refund.setStatus(RefundStatusEnum.CANCELLED.getCode());
        refund.setUpdatedAt(LocalDateTime.now());

        int rows = refundMapper.updateById(refund);

        if (rows > 0) {
            log.info("✅ 退款已取消: refundId={}", refundId);

            // 发送取消事件
            refundMessageProducer.sendRefundCancelledEvent(refund);

            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean processRefund(Long refundId) {
        log.info("💰 开始处理退款: refundId={}", refundId);

        // 1. 查询退款单
        Refund refund = getRefundById(refundId);
        if (refund == null) {
            throw new BusinessException("退款单不存在");
        }

        // 2. 验证状态 - 只有审核通过或已收货的退款可以处理
        if (!refund.isAuditPassed() && refund.getStatus() != RefundStatusEnum.GOODS_RECEIVED.getCode()) {
            throw new BusinessException("退款单状态不允许处理退款");
        }

        // 3. 更新退款单状态为退款中
        refund.setStatus(RefundStatusEnum.REFUNDING.getCode());
        refund.setUpdatedAt(LocalDateTime.now());
        refundMapper.updateById(refund);

        // 4. 更新订单退款状态为"退款中"
        Order order = orderService.getById(refund.getOrderId());
        if (order != null) {
            order.setRefundStatus(OrderRefundStatusEnum.REFUNDING.getCode());
            order.setUpdatedAt(LocalDateTime.now());
            orderService.updateById(order);
            log.info("✅ 订单退款状态已更新为退款中: orderId={}", order.getId());
        }

        // 5. 发送退款处理事件给payment-service
        boolean sent = refundMessageProducer.sendRefundProcessEvent(refund);

        if (sent) {
            log.info("✅ 退款处理事件已发送: refundId={}, refundNo={}",
                    refund.getId(), refund.getRefundNo());
            return true;
        } else {
            log.error("❌ 退款处理事件发送失败: refundId={}", refundId);
            return false;
        }
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
        log.info("📋 开始查询退款列表: pageNum={}, pageSize={}", pageDTO.getPageNum(), pageDTO.getPageSize());

        // 1. 构建查询条件
        LambdaQueryWrapper<Refund> wrapper = new LambdaQueryWrapper<>();

        // 只查询未删除的记录
        wrapper.eq(Refund::getIsDeleted, 0);

        // 按状态筛选
        if (pageDTO.getStatus() != null) {
            wrapper.eq(Refund::getStatus, pageDTO.getStatus());
        }

        // 按用户ID筛选
        if (pageDTO.getUserId() != null) {
            wrapper.eq(Refund::getUserId, pageDTO.getUserId());
        }

        // 按商家ID筛选
        if (pageDTO.getMerchantId() != null) {
            wrapper.eq(Refund::getMerchantId, pageDTO.getMerchantId());
        }

        // 按订单号筛选
        if (StringUtils.hasText(pageDTO.getOrderNo())) {
            wrapper.eq(Refund::getOrderNo, pageDTO.getOrderNo());
        }

        // 按退款单号筛选
        if (StringUtils.hasText(pageDTO.getRefundNo())) {
            wrapper.eq(Refund::getRefundNo, pageDTO.getRefundNo());
        }

        // 按退款类型筛选
        if (pageDTO.getRefundType() != null) {
            wrapper.eq(Refund::getRefundType, pageDTO.getRefundType());
        }

        // 按时间范围筛选
        if (StringUtils.hasText(pageDTO.getStartDate())) {
            wrapper.ge(Refund::getCreatedAt, pageDTO.getStartDate() + " 00:00:00");
        }
        if (StringUtils.hasText(pageDTO.getEndDate())) {
            wrapper.le(Refund::getCreatedAt, pageDTO.getEndDate() + " 23:59:59");
        }

        // 排序
        String sortField = pageDTO.getSortField();
        String sortOrder = pageDTO.getSortOrder();

        if ("refund_amount".equals(sortField)) {
            if ("asc".equalsIgnoreCase(sortOrder)) {
                wrapper.orderByAsc(Refund::getRefundAmount);
            } else {
                wrapper.orderByDesc(Refund::getRefundAmount);
            }
        } else {
            // 默认按创建时间排序
            if ("asc".equalsIgnoreCase(sortOrder)) {
                wrapper.orderByAsc(Refund::getCreatedAt);
            } else {
                wrapper.orderByDesc(Refund::getCreatedAt);
            }
        }

        // 2. 分页查询
        Page<Refund> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        Page<Refund> resultPage = refundMapper.selectPage(page, wrapper);

        // 3. 转换为VO
        List<RefundVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        log.info("✅ 查询退款列表成功: total={}, pages={}, current={}",
                resultPage.getTotal(), resultPage.getPages(), resultPage.getCurrent());

        return new PageResult<>(
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getTotal(),
                voList
        );
    }

    /**
     * 将Refund实体转换为RefundVO
     */
    private RefundVO convertToVO(Refund refund) {
        RefundVO vo = new RefundVO();
        vo.setId(refund.getId());
        vo.setRefundNo(refund.getRefundNo());
        vo.setOrderId(refund.getOrderId());
        vo.setOrderNo(refund.getOrderNo());
        vo.setUserId(refund.getUserId());
        vo.setMerchantId(refund.getMerchantId());
        vo.setRefundType(refund.getRefundType());
        vo.setRefundTypeName(refund.isRefundOnly() ? "仅退款" : "退货退款");
        vo.setRefundReason(refund.getRefundReason());
        vo.setRefundDescription(refund.getRefundDescription());
        vo.setRefundAmount(refund.getRefundAmount());
        vo.setRefundQuantity(refund.getRefundQuantity());
        vo.setStatus(refund.getStatus());
        vo.setStatusName(RefundStatusEnum.fromCode(refund.getStatus()).getName());
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
