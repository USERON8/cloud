package com.cloud.api.payment;

import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.domain.vo.payment.PaymentVO;
import com.cloud.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 支付服务Feign客户端接口
 * 提供支付服务对外提供的Feign接口
 */
@FeignClient(name = "payment-service", path = "/payment/feign")
public interface PaymentFeignClient {

    /**
     * 根据订单ID查询支付信息
     *
     * @param orderId 订单ID
     * @return 支付信息
     */
    @GetMapping("/order/{orderId}")
    Result<PaymentVO> getPaymentByOrderId(@PathVariable("orderId") Long orderId);

    /**
     * 创建支付记录
     *
     * @param paymentDTO 支付信息
     * @return 支付信息
     */
    @PostMapping("/create")
    Result<PaymentVO> createPayment(@RequestBody PaymentDTO paymentDTO);

    /**
     * 更新支付状态
     *
     * @param paymentId 支付ID
     * @param status    支付状态
     * @return 是否更新成功
     */
    @PostMapping("/update/status/{paymentId}/{status}")
    Result<Boolean> updatePaymentStatus(@PathVariable("paymentId") Long paymentId,
                                        @PathVariable("status") Integer status);
}