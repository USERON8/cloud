package com.cloud.payment.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.utils.StringUtils;
import com.cloud.payment.config.AlipayConfig;
import com.cloud.payment.module.dto.AlipayCreateRequest;
import com.cloud.payment.module.dto.AlipayCreateResponse;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.AlipayService;
import com.cloud.payment.service.PaymentService;
import com.cloud.payment.utils.AlipayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付宝支付服务实现
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayServiceImpl implements AlipayService {

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;
    private final PaymentService paymentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlipayCreateResponse createPayment(AlipayCreateRequest request) {
        log.info("创建支付宝支付 - 订单ID: {}, 金额: {}", request.getOrderId(), request.getAmount());

        try {
            // 1. 检查支付记录是否已存在
            if (paymentService.isPaymentRecordExists(request.getOrderId())) {
                throw new BusinessException("支付记录已存在，请勿重复创建");
            }

            // 2. 生成商户订单号和跟踪ID
            String outTradeNo = AlipayUtils.generateOutTradeNo(request.getOrderId());
            String traceId = StringUtils.generateTraceId();

            // 3. 创建支付记录
            Payment payment = createPaymentRecord(request, outTradeNo, traceId);

            // 4. 构建支付宝支付请求
            AlipayTradePagePayRequest alipayRequest = buildAlipayRequest(request, outTradeNo);

            // 5. 调用支付宝API
            AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest);

            if (!response.isSuccess()) {
                log.error("支付宝支付创建失败 - 订单ID: {}, 错误: {}", request.getOrderId(), response.getSubMsg());
                throw new BusinessException("支付宝支付创建失败: " + response.getSubMsg());
            }

            log.info("支付宝支付创建成功 - 订单ID: {}, 支付ID: {}", request.getOrderId(), payment.getId());

            return AlipayCreateResponse.builder()
                    .paymentForm(response.getBody())
                    .paymentId(payment.getId())
                    .outTradeNo(outTradeNo)
                    .status(0) // 待支付
                    .timestamp(System.currentTimeMillis())
                    .traceId(traceId)
                    .build();

        } catch (AlipayApiException e) {
            log.error("支付宝API调用异常 - 订单ID: {}", request.getOrderId(), e);
            throw new BusinessException("支付宝支付创建失败: " + e.getMessage());
        }
    }

    @Override
    public boolean handleNotify(Map<String, String> params) {
        log.info("处理支付宝异步通知 - 参数: {}", params);

        try {
            // 1. 验证参数完整性
            if (!AlipayUtils.validateNotifyParams(params)) {
                log.error("支付宝异步通知参数不完整");
                return false;
            }

            // 2. 验证签名
            if (!verifySign(params)) {
                log.error("支付宝异步通知签名验证失败");
                return false;
            }

            // 3. 获取关键参数
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String totalAmount = params.get("total_amount");

            log.info("支付宝通知 - 订单号: {}, 交易号: {}, 状态: {}, 金额: {}",
                    outTradeNo, AlipayUtils.maskTradeNo(tradeNo), tradeStatus, totalAmount);

            // 4. 处理支付成功
            if (AlipayUtils.isPaymentSuccess(tradeStatus)) {
                return processSuccessfulPayment(outTradeNo, tradeNo, new BigDecimal(totalAmount));
            }

            // 5. 处理支付关闭
            if (AlipayUtils.isPaymentClosed(tradeStatus)) {
                return processClosedPayment(outTradeNo);
            }

            log.warn("未处理的支付状态 - 订单号: {}, 状态: {}", outTradeNo, tradeStatus);
            return true;

        } catch (Exception e) {
            log.error("处理支付宝异步通知异常", e);
            return false;
        }
    }

    @Override
    public boolean verifySign(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );
        } catch (AlipayApiException e) {
            log.error("支付宝签名验证异常", e);
            return false;
        }
    }

    @Override
    public String queryPaymentStatus(String outTradeNo) {
        log.info("查询支付状态 - 订单号: {}", outTradeNo);

        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent(String.format("{\"out_trade_no\":\"%s\"}", outTradeNo));

            AlipayTradeQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("支付状态查询成功 - 订单号: {}, 状态: {}", outTradeNo, response.getTradeStatus());
                return response.getTradeStatus();
            } else {
                log.error("支付状态查询失败 - 订单号: {}, 错误: {}", outTradeNo, response.getSubMsg());
                return null;
            }

        } catch (AlipayApiException e) {
            log.error("支付状态查询异常 - 订单号: {}", outTradeNo, e);
            return null;
        }
    }

    @Override
    public boolean refund(String outTradeNo, BigDecimal refundAmount, String refundReason) {
        log.info("申请退款 - 订单号: {}, 金额: {}, 原因: {}", outTradeNo, refundAmount, refundReason);

        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            String bizContent = String.format(
                    "{\"out_trade_no\":\"%s\",\"refund_amount\":\"%s\",\"refund_reason\":\"%s\"}",
                    outTradeNo, refundAmount.toString(), refundReason
            );
            request.setBizContent(bizContent);

            AlipayTradeRefundResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("退款申请成功 - 订单号: {}", outTradeNo);
                return true;
            } else {
                log.error("退款申请失败 - 订单号: {}, 错误: {}", outTradeNo, response.getSubMsg());
                return false;
            }

        } catch (AlipayApiException e) {
            log.error("退款申请异常 - 订单号: {}", outTradeNo, e);
            return false;
        }
    }

    @Override
    public boolean closeOrder(String outTradeNo) {
        log.info("关闭订单 - 订单号: {}", outTradeNo);

        try {
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            request.setBizContent(String.format("{\"out_trade_no\":\"%s\"}", outTradeNo));

            AlipayTradeCloseResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("订单关闭成功 - 订单号: {}", outTradeNo);
                return true;
            } else {
                log.error("订单关闭失败 - 订单号: {}, 错误: {}", outTradeNo, response.getSubMsg());
                return false;
            }

        } catch (AlipayApiException e) {
            log.error("订单关闭异常 - 订单号: {}", outTradeNo, e);
            return false;
        }
    }

    @Override
    public boolean verifyPayment(String outTradeNo) {
        String status = queryPaymentStatus(outTradeNo);
        return AlipayUtils.isPaymentSuccess(status);
    }

    /**
     * 创建支付记录
     */
    private Payment createPaymentRecord(AlipayCreateRequest request, String outTradeNo, String traceId) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount());
        payment.setStatus(0); // 待支付
        payment.setChannel(1); // 支付宝
        payment.setTraceId(traceId);

        if (!paymentService.save(payment)) {
            throw new BusinessException("创建支付记录失败");
        }

        return payment;
    }

    /**
     * 构建支付宝支付请求
     */
    private AlipayTradePagePayRequest buildAlipayRequest(AlipayCreateRequest request, String outTradeNo) {
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());
        alipayRequest.setReturnUrl(alipayConfig.getReturnUrl());

        String bizContent = String.format(
                "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\",\"body\":\"%s\",\"product_code\":\"%s\",\"timeout_express\":\"%s\"}",
                outTradeNo,
                request.getAmount().toString(),
                request.getSubject(),
                request.getBody() != null ? request.getBody() : request.getSubject(),
                request.getProductCode(),
                request.getTimeoutMinutes() + "m"
        );

        alipayRequest.setBizContent(bizContent);
        return alipayRequest;
    }

    /**
     * 处理支付成功
     */
    @Transactional(rollbackFor = Exception.class)
    private boolean processSuccessfulPayment(String outTradeNo, String tradeNo, BigDecimal amount) {
        log.info("处理支付成功 - 订单号: {}, 交易号: {}, 金额: {}", outTradeNo, tradeNo, amount);

        try {
            // 1. 根据商户订单号查找支付记录
            Long orderId = AlipayUtils.extractOrderIdFromOutTradeNo(outTradeNo);
            if (orderId == null) {
                log.error("无法从商户订单号中提取订单ID - 订单号: {}", outTradeNo);
                return false;
            }

            Payment payment = paymentService.lambdaQuery()
                    .eq(Payment::getOrderId, orderId)
                    .eq(Payment::getStatus, 0) // 待支付状态
                    .one();

            if (payment == null) {
                log.warn("未找到待支付的支付记录 - 订单号: {}", outTradeNo);
                return false;
            }

            // 2. 防重复处理：检查是否已经处理过
            if (payment.getTransactionId() != null && payment.getTransactionId().equals(tradeNo)) {
                log.info("支付已处理过 - 订单号: {}, 交易号: {}", outTradeNo, tradeNo);
                return true;
            }

            // 3. 更新支付记录状态
            payment.setStatus(1); // 支付成功
            payment.setTransactionId(tradeNo);
            payment.setUpdatedAt(LocalDateTime.now());

            boolean updateSuccess = paymentService.updateById(payment);
            if (!updateSuccess) {
                log.error("更新支付记录失败 - 订单号: {}", outTradeNo);
                return false;
            }

            // 4. 创建支付流水记录
            createPaymentFlow(payment.getId(), 1, amount, payment.getTraceId());

            // 5. 发送支付成功事件（这里可以通过消息队列通知订单服务）
            // publishPaymentSuccessEvent(payment);

            log.info("支付成功处理完成 - 订单号: {}, 支付ID: {}", outTradeNo, payment.getId());
            return true;

        } catch (Exception e) {
            log.error("处理支付成功异常 - 订单号: {}", outTradeNo, e);
            return false;
        }
    }

    /**
     * 处理支付关闭
     */
    @Transactional(rollbackFor = Exception.class)
    private boolean processClosedPayment(String outTradeNo) {
        log.info("处理支付关闭 - 订单号: {}", outTradeNo);

        try {
            // 1. 根据商户订单号查找支付记录
            Long orderId = AlipayUtils.extractOrderIdFromOutTradeNo(outTradeNo);
            if (orderId == null) {
                log.error("无法从商户订单号中提取订单ID - 订单号: {}", outTradeNo);
                return false;
            }

            Payment payment = paymentService.lambdaQuery()
                    .eq(Payment::getOrderId, orderId)
                    .eq(Payment::getStatus, 0) // 待支付状态
                    .one();

            if (payment == null) {
                log.warn("未找到待支付的支付记录 - 订单号: {}", outTradeNo);
                return false;
            }

            // 2. 更新支付记录状态为失败
            payment.setStatus(2); // 支付失败
            payment.setUpdatedAt(LocalDateTime.now());

            boolean updateSuccess = paymentService.updateById(payment);
            if (!updateSuccess) {
                log.error("更新支付记录失败 - 订单号: {}", outTradeNo);
                return false;
            }

            // 3. 发送支付关闭事件
            // publishPaymentClosedEvent(payment);

            log.info("支付关闭处理完成 - 订单号: {}, 支付ID: {}", outTradeNo, payment.getId());
            return true;

        } catch (Exception e) {
            log.error("处理支付关闭异常 - 订单号: {}", outTradeNo, e);
            return false;
        }
    }


    /**
     * 创建支付流水记录
     */
    private void createPaymentFlow(Long paymentId, Integer flowType, BigDecimal amount, String traceId) {
        // 这里需要注入PaymentFlowService，暂时省略具体实现
        log.info("创建支付流水 - 支付ID: {}, 类型: {}, 金额: {}", paymentId, flowType, amount);
    }
}
