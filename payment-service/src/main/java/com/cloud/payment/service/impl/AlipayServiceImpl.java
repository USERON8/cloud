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
        

        try {
            
            if (paymentService.isPaymentRecordExists(request.getOrderId())) {
                throw new BusinessException("鏀粯璁板綍宸插瓨鍦紝璇峰嬁閲嶅鍒涘缓");
            }

            
            String outTradeNo = AlipayUtils.generateOutTradeNo(request.getOrderId());
            String traceId = StringUtils.generateTraceId();

            
            Payment payment = createPaymentRecord(request, outTradeNo, traceId);

            
            AlipayTradePagePayRequest alipayRequest = buildAlipayRequest(request, outTradeNo);

            
            AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest);

            if (!response.isSuccess()) {
                log.error("鏀粯瀹濇敮浠樺垱寤哄け璐?- 璁㈠崟ID: {}, 閿欒: {}", request.getOrderId(), response.getSubMsg());
                throw new BusinessException("鏀粯瀹濇敮浠樺垱寤哄け璐? " + response.getSubMsg());
            }

            

            return AlipayCreateResponse.builder()
                    .paymentForm(response.getBody())
                    .paymentId(payment.getId())
                    .outTradeNo(outTradeNo)
                    .status(0) 
                    .timestamp(System.currentTimeMillis())
                    .traceId(traceId)
                    .build();

        } catch (AlipayApiException e) {
            log.error("鏀粯瀹滱PI璋冪敤寮傚父 - 璁㈠崟ID: {}", request.getOrderId(), e);
            throw new BusinessException("鏀粯瀹濇敮浠樺垱寤哄け璐? " + e.getMessage());
        }
    }

    @Override
    public boolean handleNotify(Map<String, String> params) {
        

        try {
            
            if (!AlipayUtils.validateNotifyParams(params)) {
                log.error("鏀粯瀹濆紓姝ラ€氱煡鍙傛暟涓嶅畬鏁?);
                return false;
            }

            
            if (!verifySign(params)) {
                log.error("鏀粯瀹濆紓姝ラ€氱煡绛惧悕楠岃瘉澶辫触");
                return false;
            }

            
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String totalAmount = params.get("total_amount");

            


            
            if (AlipayUtils.isPaymentSuccess(tradeStatus)) {
                return processSuccessfulPayment(outTradeNo, tradeNo, new BigDecimal(totalAmount));
            }

            
            if (AlipayUtils.isPaymentClosed(tradeStatus)) {
                return processClosedPayment(outTradeNo);
            }

            log.warn("鏈鐞嗙殑鏀粯鐘舵€?- 璁㈠崟鍙? {}, 鐘舵€? {}", outTradeNo, tradeStatus);
            return true;

        } catch (Exception e) {
            log.error("澶勭悊鏀粯瀹濆紓姝ラ€氱煡寮傚父", e);
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
            log.error("鏀粯瀹濈鍚嶉獙璇佸紓甯?, e);
            return false;
        }
    }

    @Override
    public String queryPaymentStatus(String outTradeNo) {
        

        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent(String.format("{\"out_trade_no\":\"%s\"}", outTradeNo));

            AlipayTradeQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                
                return response.getTradeStatus();
            } else {
                log.error("鏀粯鐘舵€佹煡璇㈠け璐?- 璁㈠崟鍙? {}, 閿欒: {}", outTradeNo, response.getSubMsg());
                return null;
            }

        } catch (AlipayApiException e) {
            log.error("鏀粯鐘舵€佹煡璇㈠紓甯?- 璁㈠崟鍙? {}", outTradeNo, e);
            return null;
        }
    }

    @Override
    public boolean refund(String outTradeNo, BigDecimal refundAmount, String refundReason) {
        

        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            String bizContent = String.format(
                    "{\"out_trade_no\":\"%s\",\"refund_amount\":\"%s\",\"refund_reason\":\"%s\"}",
                    outTradeNo, refundAmount.toString(), refundReason
            );
            request.setBizContent(bizContent);

            AlipayTradeRefundResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                
                return true;
            } else {
                log.error("閫€娆剧敵璇峰け璐?- 璁㈠崟鍙? {}, 閿欒: {}", outTradeNo, response.getSubMsg());
                return false;
            }

        } catch (AlipayApiException e) {
            log.error("閫€娆剧敵璇峰紓甯?- 璁㈠崟鍙? {}", outTradeNo, e);
            return false;
        }
    }

    @Override
    public boolean closeOrder(String outTradeNo) {
        

        try {
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            request.setBizContent(String.format("{\"out_trade_no\":\"%s\"}", outTradeNo));

            AlipayTradeCloseResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                
                return true;
            } else {
                log.error("璁㈠崟鍏抽棴澶辫触 - 璁㈠崟鍙? {}, 閿欒: {}", outTradeNo, response.getSubMsg());
                return false;
            }

        } catch (AlipayApiException e) {
            log.error("璁㈠崟鍏抽棴寮傚父 - 璁㈠崟鍙? {}", outTradeNo, e);
            return false;
        }
    }

    @Override
    public boolean verifyPayment(String outTradeNo) {
        String status = queryPaymentStatus(outTradeNo);
        return AlipayUtils.isPaymentSuccess(status);
    }

    


    private Payment createPaymentRecord(AlipayCreateRequest request, String outTradeNo, String traceId) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount());
        payment.setStatus(0); 
        payment.setChannel(1); 
        payment.setTraceId(traceId);

        if (!paymentService.save(payment)) {
            throw new BusinessException("鍒涘缓鏀粯璁板綍澶辫触");
        }

        return payment;
    }

    


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

    


    @Transactional(rollbackFor = Exception.class)
    private boolean processSuccessfulPayment(String outTradeNo, String tradeNo, BigDecimal amount) {
        

        try {
            
            Long orderId = AlipayUtils.extractOrderIdFromOutTradeNo(outTradeNo);
            if (orderId == null) {
                log.error("鏃犳硶浠庡晢鎴疯鍗曞彿涓彁鍙栬鍗旾D - 璁㈠崟鍙? {}", outTradeNo);
                return false;
            }

            Payment payment = paymentService.lambdaQuery()
                    .eq(Payment::getOrderId, orderId)
                    .eq(Payment::getStatus, 0) 
                    .one();

            if (payment == null) {
                log.warn("鏈壘鍒板緟鏀粯鐨勬敮浠樿褰?- 璁㈠崟鍙? {}", outTradeNo);
                return false;
            }

            
            if (payment.getTransactionId() != null && payment.getTransactionId().equals(tradeNo)) {
                
                return true;
            }

            
            payment.setStatus(1); 
            payment.setTransactionId(tradeNo);
            payment.setUpdatedAt(LocalDateTime.now());

            boolean updateSuccess = paymentService.updateById(payment);
            if (!updateSuccess) {
                log.error("鏇存柊鏀粯璁板綍澶辫触 - 璁㈠崟鍙? {}", outTradeNo);
                return false;
            }

            
            createPaymentFlow(payment.getId(), 1, amount, payment.getTraceId());

            
            

            
            return true;

        } catch (Exception e) {
            log.error("澶勭悊鏀粯鎴愬姛寮傚父 - 璁㈠崟鍙? {}", outTradeNo, e);
            return false;
        }
    }

    


    @Transactional(rollbackFor = Exception.class)
    private boolean processClosedPayment(String outTradeNo) {
        

        try {
            
            Long orderId = AlipayUtils.extractOrderIdFromOutTradeNo(outTradeNo);
            if (orderId == null) {
                log.error("鏃犳硶浠庡晢鎴疯鍗曞彿涓彁鍙栬鍗旾D - 璁㈠崟鍙? {}", outTradeNo);
                return false;
            }

            Payment payment = paymentService.lambdaQuery()
                    .eq(Payment::getOrderId, orderId)
                    .eq(Payment::getStatus, 0) 
                    .one();

            if (payment == null) {
                log.warn("鏈壘鍒板緟鏀粯鐨勬敮浠樿褰?- 璁㈠崟鍙? {}", outTradeNo);
                return false;
            }

            
            payment.setStatus(2); 
            payment.setUpdatedAt(LocalDateTime.now());

            boolean updateSuccess = paymentService.updateById(payment);
            if (!updateSuccess) {
                log.error("鏇存柊鏀粯璁板綍澶辫触 - 璁㈠崟鍙? {}", outTradeNo);
                return false;
            }

            
            

            
            return true;

        } catch (Exception e) {
            log.error("澶勭悊鏀粯鍏抽棴寮傚父 - 璁㈠崟鍙? {}", outTradeNo, e);
            return false;
        }
    }


    


    private void createPaymentFlow(Long paymentId, Integer flowType, BigDecimal amount, String traceId) {
        
        
    }
}
