package com.cloud.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.payment.mapper.PaymentCallbackLogMapper;
import com.cloud.payment.mapper.PaymentOrderMapper;
import com.cloud.payment.mapper.PaymentRefundMapper;
import com.cloud.payment.module.entity.PaymentCallbackLogEntity;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.module.entity.PaymentRefundEntity;
import com.cloud.payment.service.PaymentOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentOrderServiceImpl implements PaymentOrderService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundMapper paymentRefundMapper;
    private final PaymentCallbackLogMapper paymentCallbackLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPaymentOrder(PaymentOrderCommandDTO command) {
        PaymentOrderEntity existing = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getIdempotencyKey, command.getIdempotencyKey())
                .eq(PaymentOrderEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }

        PaymentOrderEntity entity = new PaymentOrderEntity();
        entity.setPaymentNo(command.getPaymentNo());
        entity.setMainOrderNo(command.getMainOrderNo());
        entity.setSubOrderNo(command.getSubOrderNo());
        entity.setUserId(command.getUserId());
        entity.setAmount(command.getAmount());
        entity.setChannel(command.getChannel());
        entity.setStatus("CREATED");
        entity.setIdempotencyKey(command.getIdempotencyKey());
        paymentOrderMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public PaymentOrderVO getPaymentOrderByNo(String paymentNo) {
        PaymentOrderEntity entity = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getPaymentNo, paymentNo)
                .eq(PaymentOrderEntity::getDeleted, 0)
                .last("LIMIT 1"));
        return entity == null ? null : toOrderVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean handlePaymentCallback(PaymentCallbackCommandDTO command) {
        PaymentCallbackLogEntity callbackLog = paymentCallbackLogMapper.selectOne(new LambdaQueryWrapper<PaymentCallbackLogEntity>()
                .eq(PaymentCallbackLogEntity::getIdempotencyKey, command.getIdempotencyKey())
                .eq(PaymentCallbackLogEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (callbackLog != null) {
            return true;
        }

        PaymentOrderEntity order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getPaymentNo, command.getPaymentNo())
                .eq(PaymentOrderEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException("payment order not found");
        }

        PaymentCallbackLogEntity log = new PaymentCallbackLogEntity();
        log.setPaymentNo(command.getPaymentNo());
        log.setCallbackNo(command.getCallbackNo());
        log.setCallbackStatus(command.getCallbackStatus());
        log.setProviderTxnNo(command.getProviderTxnNo());
        log.setPayload(command.getPayload());
        log.setIdempotencyKey(command.getIdempotencyKey());
        paymentCallbackLogMapper.insert(log);

        if ("SUCCESS".equalsIgnoreCase(command.getCallbackStatus())) {
            order.setStatus("PAID");
            order.setProviderTxnNo(command.getProviderTxnNo());
            if (order.getPaidAt() == null) {
                order.setPaidAt(LocalDateTime.now());
            }
        } else if ("FAIL".equalsIgnoreCase(command.getCallbackStatus())) {
            order.setStatus("FAILED");
        }
        paymentOrderMapper.updateById(order);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRefund(PaymentRefundCommandDTO command) {
        PaymentRefundEntity existing = paymentRefundMapper.selectOne(new LambdaQueryWrapper<PaymentRefundEntity>()
                .eq(PaymentRefundEntity::getIdempotencyKey, command.getIdempotencyKey())
                .eq(PaymentRefundEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }

        PaymentOrderEntity paymentOrder = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getPaymentNo, command.getPaymentNo())
                .eq(PaymentOrderEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (paymentOrder == null) {
            throw new BusinessException("payment order not found");
        }

        PaymentRefundEntity entity = new PaymentRefundEntity();
        entity.setRefundNo(command.getRefundNo());
        entity.setPaymentNo(command.getPaymentNo());
        entity.setAfterSaleNo(command.getAfterSaleNo());
        entity.setRefundAmount(command.getRefundAmount());
        entity.setReason(command.getReason());
        entity.setStatus("REFUNDING");
        entity.setIdempotencyKey(command.getIdempotencyKey());
        paymentRefundMapper.insert(entity);

        entity.setStatus("REFUNDED");
        entity.setRefundedAt(LocalDateTime.now());
        paymentRefundMapper.updateById(entity);
        return entity.getId();
    }

    @Override
    public PaymentRefundVO getRefundByNo(String refundNo) {
        PaymentRefundEntity entity = paymentRefundMapper.selectOne(new LambdaQueryWrapper<PaymentRefundEntity>()
                .eq(PaymentRefundEntity::getRefundNo, refundNo)
                .eq(PaymentRefundEntity::getDeleted, 0)
                .last("LIMIT 1"));
        return entity == null ? null : toRefundVO(entity);
    }

    private PaymentOrderVO toOrderVO(PaymentOrderEntity entity) {
        PaymentOrderVO vo = new PaymentOrderVO();
        vo.setId(entity.getId());
        vo.setPaymentNo(entity.getPaymentNo());
        vo.setMainOrderNo(entity.getMainOrderNo());
        vo.setSubOrderNo(entity.getSubOrderNo());
        vo.setUserId(entity.getUserId());
        vo.setAmount(entity.getAmount());
        vo.setChannel(entity.getChannel());
        vo.setStatus(entity.getStatus());
        vo.setProviderTxnNo(entity.getProviderTxnNo());
        vo.setIdempotencyKey(entity.getIdempotencyKey());
        vo.setPaidAt(entity.getPaidAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private PaymentRefundVO toRefundVO(PaymentRefundEntity entity) {
        PaymentRefundVO vo = new PaymentRefundVO();
        vo.setId(entity.getId());
        vo.setRefundNo(entity.getRefundNo());
        vo.setPaymentNo(entity.getPaymentNo());
        vo.setAfterSaleNo(entity.getAfterSaleNo());
        vo.setRefundAmount(entity.getRefundAmount());
        vo.setStatus(entity.getStatus());
        vo.setReason(entity.getReason());
        vo.setIdempotencyKey(entity.getIdempotencyKey());
        vo.setRefundedAt(entity.getRefundedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
