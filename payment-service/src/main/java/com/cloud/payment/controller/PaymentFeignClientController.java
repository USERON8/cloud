package com.cloud.payment.controller;

import com.cloud.api.payment.PaymentFeignClient;
import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.domain.vo.PaymentVO;
import com.cloud.common.result.Result;
import com.cloud.payment.converter.PaymentConverter;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付服务Feign客户端接口实现控制器
 * 实现支付服务对外提供的Feign接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentFeignClientController implements PaymentFeignClient {

    private final PaymentService paymentService;
    private final PaymentConverter paymentConverter = PaymentConverter.INSTANCE;

    /**
     * 根据订单ID查询支付信息
     *
     * @param orderId 订单ID
     * @return 支付信息
     */
    @Override
    public Result<PaymentVO> getPaymentByOrderId(Long orderId) {
        log.info("Feign调用：根据订单ID查询支付信息，订单ID: {}", orderId);
        try {
            // 根据订单ID查询支付记录
            Payment payment = paymentService.lambdaQuery()
                    .eq(Payment::getOrderId, orderId)
                    .one();
            
            if (payment == null) {
                log.warn("Feign调用：未找到支付记录，订单ID: {}", orderId);
                return Result.success(null);
            }
            
            PaymentVO paymentVO = paymentConverter.toVO(payment);
            return Result.success(paymentVO);
        } catch (Exception e) {
            log.error("Feign调用：根据订单ID查询支付信息失败，订单ID: {}", orderId, e);
            return Result.error("查询支付信息失败: " + e.getMessage());
        }
    }

    /**
     * 创建支付记录
     *
     * @param paymentDTO 支付信息
     * @return 支付信息
     */
    @Override
    public Result<PaymentVO> createPayment(PaymentDTO paymentDTO) {
        log.info("Feign调用：创建支付记录，订单ID: {}", paymentDTO.getOrderId());
        try {
            // 验证参数
            if (paymentDTO.getOrderId() == null || paymentDTO.getAmount() == null) {
                return Result.error("订单ID和支付金额不能为空");
            }
            
            // 检查支付记录是否已存在
            Payment existingPayment = paymentService.lambdaQuery()
                    .eq(Payment::getOrderId, paymentDTO.getOrderId())
                    .one();
            
            if (existingPayment != null) {
                log.warn("Feign调用：支付记录已存在，订单ID: {}", paymentDTO.getOrderId());
                return Result.success(paymentConverter.toVO(existingPayment));
            }
            
            // 创建支付记录
            Payment payment = paymentConverter.toEntity(paymentDTO);
            payment.setStatus(0); // 设置为待支付状态
            
            boolean saved = paymentService.save(payment);
            if (!saved) {
                return Result.error("支付记录创建失败");
            }
            
            PaymentVO paymentVO = paymentConverter.toVO(payment);
            return Result.success(paymentVO);
        } catch (Exception e) {
            log.error("Feign调用：创建支付记录失败，订单ID: {}", paymentDTO.getOrderId(), e);
            return Result.error("创建支付记录失败: " + e.getMessage());
        }
    }

    /**
     * 更新支付状态
     *
     * @param paymentId 支付ID
     * @param status    支付状态
     * @return 是否更新成功
     */
    @Override
    public Result<Boolean> updatePaymentStatus(Long paymentId, Integer status) {
        log.info("Feign调用：更新支付状态，支付ID: {}，状态: {}", paymentId, status);
        try {
            // 验证参数
            if (paymentId == null || status == null) {
                return Result.error("支付ID和状态不能为空");
            }
            
            // 验证状态值的合法性
            if (status < 0 || status > 3) {
                return Result.error("无效的支付状态");
            }
            
            // 查询支付记录
            Payment payment = paymentService.getById(paymentId);
            if (payment == null) {
                return Result.error("支付记录不存在");
            }
            
            // 更新状态
            boolean updated = paymentService.lambdaUpdate()
                    .eq(Payment::getId, paymentId)
                    .set(Payment::getStatus, status)
                    .update();
                    
            if (updated) {
                log.info("Feign调用：支付状态更新成功，支付ID: {}，新状态: {}", paymentId, status);
            }
            
            return Result.success(updated);
        } catch (Exception e) {
            log.error("Feign调用：更新支付状态失败，支付ID: {}，状态: {}", paymentId, status, e);
            return Result.error("更新支付状态失败: " + e.getMessage());
        }
    }
}