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
import com.cloud.payment.module.entity.PaymentFlow;
import com.cloud.payment.service.AlipayService;
import com.cloud.payment.service.PaymentFlowService;
import com.cloud.payment.service.PaymentService;
import com.cloud.payment.utils.AlipayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayServiceImpl implements AlipayService {

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;
    private final PaymentService paymentService;
    private final PaymentFlowService paymentFlowService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlipayCreateResponse createPayment(AlipayCreateRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Invalid payment amount");
        }
        if (paymentService.isPaymentRecordExists(request.getOrderId())) {
            throw new BusinessException("Payment record already exists for this order");
        }

        String outTradeNo = AlipayUtils.generateOutTradeNo(request.getOrderId());
        String traceId = StringUtils.generateTraceId();

        Payment payment = createPaymentRecord(request, traceId);
        AlipayTradePagePayRequest alipayRequest = buildAlipayRequest(request, outTradeNo);

        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest);
            if (!response.isSuccess()) {
                throw new BusinessException("Create Alipay payment failed: " + response.getSubMsg());
            }
            return AlipayCreateResponse.builder()
                    .paymentForm(response.getBody())
                    .paymentId(payment.getId())
                    .outTradeNo(outTradeNo)
                    .status(payment.getStatus())
                    .timestamp(System.currentTimeMillis())
                    .traceId(traceId)
                    .build();
        } catch (AlipayApiException e) {
            throw new BusinessException("Call Alipay API failed", e);
        }
    }

    @Override
    public boolean handleNotify(Map<String, String> params) {
        try {
            if (!AlipayUtils.validateNotifyParams(params)) {
                return false;
            }
            if (!verifySign(params)) {
                return false;
            }

            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            BigDecimal totalAmount = new BigDecimal(params.get("total_amount"));

            if (AlipayUtils.isPaymentSuccess(tradeStatus)) {
                return processSuccessfulPayment(outTradeNo, tradeNo, totalAmount);
            }
            if (AlipayUtils.isPaymentClosed(tradeStatus)) {
                return processClosedPayment(outTradeNo);
            }
            return true;
        } catch (Exception e) {
            log.error("Handle Alipay notify failed", e);
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
            log.error("Verify Alipay signature failed", e);
            return false;
        }
    }

    @Override
    public String queryPaymentStatus(String outTradeNo) {
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent(String.format("{\"out_trade_no\":\"%s\"}", sanitizeJsonValue(outTradeNo)));
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                log.warn("Query Alipay payment status failed: outTradeNo={}, subMsg={}", outTradeNo, response.getSubMsg());
                return null;
            }
            return response.getTradeStatus();
        } catch (AlipayApiException e) {
            log.error("Query Alipay payment status failed: outTradeNo={}", outTradeNo, e);
            return null;
        }
    }

    @Override
    public boolean refund(String outTradeNo, BigDecimal refundAmount, String refundReason) {
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            request.setBizContent(String.format(
                    "{\"out_trade_no\":\"%s\",\"refund_amount\":\"%s\",\"refund_reason\":\"%s\"}",
                    sanitizeJsonValue(outTradeNo),
                    refundAmount,
                    sanitizeJsonValue(refundReason)
            ));
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                log.warn("Refund failed: outTradeNo={}, subMsg={}", outTradeNo, response.getSubMsg());
                return false;
            }
            return true;
        } catch (AlipayApiException e) {
            log.error("Refund failed: outTradeNo={}", outTradeNo, e);
            return false;
        }
    }

    @Override
    public boolean closeOrder(String outTradeNo) {
        try {
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            request.setBizContent(String.format("{\"out_trade_no\":\"%s\"}", sanitizeJsonValue(outTradeNo)));
            AlipayTradeCloseResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                log.warn("Close order failed: outTradeNo={}, subMsg={}", outTradeNo, response.getSubMsg());
                return false;
            }
            return true;
        } catch (AlipayApiException e) {
            log.error("Close order failed: outTradeNo={}", outTradeNo, e);
            return false;
        }
    }

    @Override
    public boolean verifyPayment(String outTradeNo) {
        String status = queryPaymentStatus(outTradeNo);
        return AlipayUtils.isPaymentSuccess(status);
    }

    private Payment createPaymentRecord(AlipayCreateRequest request, String traceId) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount());
        payment.setStatus(0);
        payment.setChannel(1);
        payment.setTraceId(traceId);
        payment.setUpdatedAt(LocalDateTime.now());

        if (!paymentService.save(payment)) {
            throw new BusinessException("Create payment record failed");
        }
        return payment;
    }

    private AlipayTradePagePayRequest buildAlipayRequest(AlipayCreateRequest request, String outTradeNo) {
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());
        alipayRequest.setReturnUrl(alipayConfig.getReturnUrl());

        String subject = sanitizeJsonValue(request.getSubject());
        String body = sanitizeJsonValue(request.getBody() == null ? request.getSubject() : request.getBody());
        String productCode = sanitizeJsonValue(request.getProductCode());
        int timeoutMinutes = request.getTimeoutMinutes() == null || request.getTimeoutMinutes() <= 0
                ? 30 : request.getTimeoutMinutes();

        String bizContent = String.format(
                "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\",\"body\":\"%s\",\"product_code\":\"%s\",\"timeout_express\":\"%sm\"}",
                sanitizeJsonValue(outTradeNo),
                request.getAmount(),
                subject,
                body,
                productCode,
                timeoutMinutes
        );
        alipayRequest.setBizContent(bizContent);
        return alipayRequest;
    }

    @Transactional(rollbackFor = Exception.class)
    private boolean processSuccessfulPayment(String outTradeNo, String tradeNo, BigDecimal amount) {
        Long orderId = AlipayUtils.extractOrderIdFromOutTradeNo(outTradeNo);
        if (orderId == null) {
            return false;
        }

        Payment payment = paymentService.lambdaQuery()
                .eq(Payment::getOrderId, orderId)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1")
                .one();
        if (payment == null) {
            return false;
        }
        if (payment.getAmount() != null && amount != null && payment.getAmount().compareTo(amount) != 0) {
            log.warn("Notify amount mismatch: paymentId={}, dbAmount={}, notifyAmount={}", payment.getId(), payment.getAmount(), amount);
            return false;
        }
        if (payment.getStatus() != null && payment.getStatus() == 2) {
            return true;
        }

        payment.setTransactionId(tradeNo);
        payment.setUpdatedAt(LocalDateTime.now());
        if (!paymentService.updateById(payment)) {
            return false;
        }

        boolean updated = Boolean.TRUE.equals(paymentService.processPaymentSuccess(payment.getId()));
        if (updated) {
            createPaymentFlow(payment.getId(), 1, amount, payment.getTraceId());
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    private boolean processClosedPayment(String outTradeNo) {
        Long orderId = AlipayUtils.extractOrderIdFromOutTradeNo(outTradeNo);
        if (orderId == null) {
            return false;
        }

        Payment payment = paymentService.lambdaQuery()
                .eq(Payment::getOrderId, orderId)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1")
                .one();
        if (payment == null) {
            return false;
        }
        if (payment.getStatus() != null && payment.getStatus() == 3) {
            return true;
        }

        boolean updated = Boolean.TRUE.equals(paymentService.processPaymentFailed(payment.getId(), "closed by Alipay"));
        if (updated) {
            createPaymentFlow(payment.getId(), 2, payment.getAmount(), payment.getTraceId());
        }
        return updated;
    }

    private void createPaymentFlow(Long paymentId, Integer flowType, BigDecimal amount, String traceId) {
        if (paymentId == null || flowType == null || amount == null) {
            return;
        }
        try {
            PaymentFlow flow = new PaymentFlow();
            flow.setPaymentId(paymentId);
            flow.setFlowType(flowType);
            flow.setAmount(amount);
            flow.setTraceId(traceId);
            paymentFlowService.save(flow);
        } catch (Exception e) {
            log.warn("Create payment flow failed: paymentId={}, flowType={}", paymentId, flowType, e);
        }
    }

    private String sanitizeJsonValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
