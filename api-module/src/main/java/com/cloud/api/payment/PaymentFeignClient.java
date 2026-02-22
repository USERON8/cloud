package com.cloud.api.payment;

import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.domain.vo.payment.PaymentVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 支付服务Feign客户端接口
 * 提供支付服务对外提供的Feign接口
 * 直接返回业务对象，仅用于服务内部调用
 *
 * @author cloud
 */
@FeignClient(name = "payment-service", path = "/internal/payment", contextId = "paymentFeignClient")
public interface PaymentFeignClient {

    /**
     * 根据ID查询支付信息
     *
     * @param paymentId 支付ID
     * @return 支付信息，不存在时返回null
     */
    @GetMapping("/{paymentId}")
    PaymentVO getPaymentById(@PathVariable("paymentId") Long paymentId);

    /**
     * 根据订单ID查询支付信息
     *
     * @param orderId 订单ID
     * @return 支付信息，不存在时返回null
     */
    @GetMapping("/order/{orderId}")
    PaymentDTO getPaymentByOrderId(@PathVariable("orderId") Long orderId);

    /**
     * 创建支付记录
     *
     * @param paymentDTO 支付信息
     * @return 创建的支付记录ID
     */
    @PostMapping("/create")
    Long createPayment(@RequestBody PaymentDTO paymentDTO);

    /**
     * 更新支付记录
     *
     * @param paymentId  支付ID
     * @param paymentDTO 支付信息
     * @return 是否更新成功
     */
    @PutMapping("/{paymentId}")
    Boolean updatePayment(@PathVariable("paymentId") Long paymentId, @RequestBody PaymentDTO paymentDTO);

    /**
     * 删除支付记录
     *
     * @param paymentId 支付ID
     * @return 是否删除成功
     */
    @DeleteMapping("/{paymentId}")
    Boolean deletePayment(@PathVariable("paymentId") Long paymentId);

    /**
     * 处理支付成功
     *
     * @param paymentId 支付ID
     * @return 是否处理成功
     */
    @PostMapping("/{paymentId}/success")
    Boolean paymentSuccess(@PathVariable("paymentId") Long paymentId);

    /**
     * 处理支付失败
     *
     * @param paymentId  支付ID
     * @param failReason 失败原因
     * @return 是否处理成功
     */
    @PostMapping("/{paymentId}/fail")
    Boolean paymentFail(@PathVariable("paymentId") Long paymentId,
                        @RequestParam(required = false) String failReason);

    /**
     * 支付退款
     *
     * @param paymentId    支付ID
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     * @return 是否退款成功
     */
    @PostMapping("/{paymentId}/refund")
    Boolean refundPayment(@PathVariable("paymentId") Long paymentId,
                          @RequestParam BigDecimal refundAmount,
                          @RequestParam(required = false) String refundReason);

    /**
     * 支付风控检查
     *
     * @param userId        用户ID
     * @param amount        支付金额
     * @param paymentMethod 支付方式
     * @return 风控检查是否通过
     */
    @PostMapping("/risk-check")
    Boolean riskCheck(@RequestParam Long userId,
                      @RequestParam BigDecimal amount,
                      @RequestParam String paymentMethod);

    // ==================== 批量操作接口 ====================

    /**
     * 批量查询支付记录
     *
     * @param paymentIds 支付ID列表
     * @return 支付记录列表，无数据时返回空列表
     */
    @GetMapping("/batch")
    List<PaymentVO> getPaymentsByIds(@RequestParam List<Long> paymentIds);

    /**
     * 批量创建支付记录
     *
     * @param paymentList 支付信息列表
     * @return 创建成功的支付记录数量
     */
    @PostMapping("/batch")
    Integer createPaymentsBatch(@RequestBody List<PaymentDTO> paymentList);

    /**
     * 批量支付成功处理
     *
     * @param paymentIds 支付ID列表
     * @return 处理成功的数量
     */
    @PostMapping("/batch/success")
    Integer paymentSuccessBatch(@RequestBody List<Long> paymentIds);

    /**
     * 批量支付失败处理
     *
     * @param paymentIds 支付ID列表
     * @param failReason 失败原因
     * @return 处理成功的数量
     */
    @PostMapping("/batch/fail")
    Integer paymentFailBatch(@RequestBody List<Long> paymentIds,
                             @RequestParam(required = false) String failReason);
}