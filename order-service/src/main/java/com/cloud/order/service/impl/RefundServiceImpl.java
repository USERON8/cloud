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
            throw new BusinessException("璁㈠崟涓嶅瓨鍦?);
        }

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("鏃犳潈鎿嶄綔姝よ鍗?);
        }

        
        OrderStatusEnum orderStatus = order.getStatusEnum();
        if (orderStatus != OrderStatusEnum.PAID &&
                orderStatus != OrderStatusEnum.SHIPPED &&
                orderStatus != OrderStatusEnum.COMPLETED) {
            throw new BusinessException("璁㈠崟鐘舵€佷笉鍏佽閫€娆?褰撳墠鐘舵€?" + orderStatus.getName());
        }

        
        Refund existingRefund = getRefundByOrderId(dto.getOrderId());
        if (existingRefund != null && !existingRefund.isCompleted()) {
            throw new BusinessException("璇ヨ鍗曞凡鏈夐€€娆剧敵璇峰湪澶勭悊涓?);
        }

        
        if (dto.getRefundAmount().compareTo(order.getPayAmount()) > 0) {
            throw new BusinessException("閫€娆鹃噾棰濅笉鑳借秴杩囧疄浠橀噾棰?);
        }

        
        Refund refund = new Refund();
        refund.setRefundNo(Refund.generateRefundNo());
        refund.setOrderId(dto.getOrderId());
        refund.setOrderNo(dto.getOrderNo());
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
            throw new BusinessException("閫€娆惧崟涓嶅瓨鍦?);
        }

        
        if (!refund.getMerchantId().equals(merchantId)) {
            throw new BusinessException("鏃犳潈瀹℃牳姝ら€€娆惧崟");
        }

        
        if (!refund.isPendingAudit()) {
            throw new BusinessException("閫€娆惧崟鐘舵€佷笉鏄緟瀹℃牳,鏃犳硶瀹℃牳");
        }

        
        refund.setStatus(approved ? RefundStatusEnum.AUDIT_PASSED.getCode() :
                RefundStatusEnum.AUDIT_REJECTED.getCode());
        refund.setAuditTime(LocalDateTime.now());
        refund.setAuditRemark(auditRemark);
        refund.setUpdatedAt(LocalDateTime.now());

        int rows = refundMapper.updateById(refund);

        if (rows > 0) {
            

            
            if (approved && refund.isRefundOnly()) {
                processRefund(refundId);
            }

            
            refundMessageProducer.sendRefundAuditedEvent(refund, approved);

            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelRefund(Long refundId, Long userId) {
        

        
        Refund refund = getRefundById(refundId);
        if (refund == null) {
            throw new BusinessException("閫€娆惧崟涓嶅瓨鍦?);
        }

        
        if (!refund.getUserId().equals(userId)) {
            throw new BusinessException("鏃犳潈鍙栨秷姝ら€€娆惧崟");
        }

        
        if (!refund.canCancel()) {
            throw new BusinessException("閫€娆惧崟褰撳墠鐘舵€佷笉鍏佽鍙栨秷");
        }

        
        refund.setStatus(RefundStatusEnum.CANCELLED.getCode());
        refund.setUpdatedAt(LocalDateTime.now());

        int rows = refundMapper.updateById(refund);

        if (rows > 0) {
            

            
            refundMessageProducer.sendRefundCancelledEvent(refund);

            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean processRefund(Long refundId) {
        

        
        Refund refund = getRefundById(refundId);
        if (refund == null) {
            throw new BusinessException("閫€娆惧崟涓嶅瓨鍦?);
        }

        
        if (!refund.isAuditPassed() && refund.getStatus() != RefundStatusEnum.GOODS_RECEIVED.getCode()) {
            throw new BusinessException("閫€娆惧崟鐘舵€佷笉鍏佽澶勭悊閫€娆?);
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

        if (sent) {
            

            return true;
        } else {
            log.error("鉂?閫€娆惧鐞嗕簨浠跺彂閫佸け璐? refundId={}", refundId);
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

        
        String sortField = pageDTO.getSortField();
        String sortOrder = pageDTO.getSortOrder();

        if ("refund_amount".equals(sortField)) {
            if ("asc".equalsIgnoreCase(sortOrder)) {
                wrapper.orderByAsc(Refund::getRefundAmount);
            } else {
                wrapper.orderByDesc(Refund::getRefundAmount);
            }
        } else {
            
            if ("asc".equalsIgnoreCase(sortOrder)) {
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

        


        return new PageResult<>(
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getTotal(),
                voList
        );
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
        vo.setRefundTypeName(refund.isRefundOnly() ? "浠呴€€娆? : "閫€璐ч€€娆?);
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
