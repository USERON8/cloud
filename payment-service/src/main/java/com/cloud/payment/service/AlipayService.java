package com.cloud.payment.service;

import com.cloud.payment.module.dto.AlipayCreateRequest;
import com.cloud.payment.module.dto.AlipayCreateResponse;
import com.cloud.payment.module.dto.AlipayNotifyRequest;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝支付服务接口
 *
 * @author what's up
 * @since 1.0.0
 */
public interface AlipayService {

    /**
     * 创建支付宝支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    AlipayCreateResponse createPayment(AlipayCreateRequest request);

    /**
     * 处理支付宝异步通知
     *
     * @param params 通知参数
     * @return 处理结果
     */
    boolean handleNotify(Map<String, String> params);

    /**
     * 验证支付宝签名
     *
     * @param params 参数
     * @return 验证结果
     */
    boolean verifySign(Map<String, String> params);

    /**
     * 查询支付状态
     *
     * @param outTradeNo 商户订单号
     * @return 支付状态
     */
    String queryPaymentStatus(String outTradeNo);

    /**
     * 申请退款
     *
     * @param outTradeNo   商户订单号
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     * @return 退款结果
     */
    boolean refund(String outTradeNo, BigDecimal refundAmount, String refundReason);

    /**
     * 关闭订单
     *
     * @param outTradeNo 商户订单号
     * @return 关闭结果
     */
    boolean closeOrder(String outTradeNo);

    /**
     * 验证支付结果
     *
     * @param outTradeNo 商户订单号
     * @return 支付是否成功
     */
    boolean verifyPayment(String outTradeNo);
}
