package com.cloud.payment.service.provider;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.cloud.payment.config.AlipayConfig;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.module.entity.PaymentRefundEntity;
import com.cloud.payment.service.provider.model.PaymentOrderQueryResult;
import com.cloud.payment.service.provider.model.PaymentRefundResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AlipayPaymentProviderGateway implements PaymentProviderGateway {

  private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

  private final AlipayClient alipayClient;
  private final AlipayConfig alipayConfig;
  private final ObjectMapper objectMapper;

  @Override
  public boolean supports(String channel) {
    return "ALIPAY".equalsIgnoreCase(channel);
  }

  @Override
  public String buildCheckoutPage(PaymentOrderEntity order) {
    AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
    Map<String, Object> bizContent = new LinkedHashMap<>();
    bizContent.put("out_trade_no", order.getPaymentNo());
    bizContent.put("total_amount", order.getAmount().toPlainString());
    bizContent.put(
        "subject",
        firstNonBlank(order.getSubOrderNo(), order.getMainOrderNo(), order.getPaymentNo()));
    bizContent.put("product_code", "QUICK_WAP_WAY");
    request.setBizContent(writeJson(bizContent));
    if (StringUtils.hasText(alipayConfig.getNotifyUrl())) {
      request.setNotifyUrl(alipayConfig.getNotifyUrl());
    }
    if (StringUtils.hasText(alipayConfig.getReturnUrl())) {
      request.setReturnUrl(
          appendCheckoutContext(alipayConfig.getReturnUrl(), order.getPaymentNo()));
    }

    try {
      AlipayTradeWapPayResponse response = alipayClient.pageExecute(request, "GET");
      if (!StringUtils.hasText(response.getBody())) {
        throw new IllegalStateException(
            buildFailureMessage(response.getSubMsg(), response.getMsg()));
      }
      return response.getBody();
    } catch (AlipayApiException ex) {
      throw new IllegalStateException("Failed to build Alipay checkout page", ex);
    }
  }

  @Override
  public PaymentOrderQueryResult queryPaymentOrder(PaymentOrderEntity order) {
    AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
    Map<String, Object> bizContent = new LinkedHashMap<>();
    bizContent.put("out_trade_no", order.getPaymentNo());
    if (StringUtils.hasText(order.getProviderTxnNo())) {
      bizContent.put("trade_no", order.getProviderTxnNo());
    }
    request.setBizContent(writeJson(bizContent));

    try {
      AlipayTradeQueryResponse response = alipayClient.execute(request);
      if (!response.isSuccess()) {
        return PaymentOrderQueryResult.error(
            buildFailureMessage(response.getSubMsg(), response.getMsg()));
      }
      String providerTxnNo = firstNonBlank(response.getTradeNo(), order.getProviderTxnNo());
      String tradeStatus = firstNonBlank(response.getTradeStatus(), "UNKNOWN");
      return switch (tradeStatus.toUpperCase()) {
        case "TRADE_SUCCESS", "TRADE_FINISHED" ->
            PaymentOrderQueryResult.paid(
                providerTxnNo, toLocalDateTime(response.getSendPayDate()), tradeStatus);
        case "TRADE_CLOSED" -> PaymentOrderQueryResult.failed(providerTxnNo, tradeStatus);
        case "WAIT_BUYER_PAY" -> PaymentOrderQueryResult.pending(providerTxnNo, tradeStatus);
        default -> PaymentOrderQueryResult.pending(providerTxnNo, tradeStatus);
      };
    } catch (AlipayApiException ex) {
      return PaymentOrderQueryResult.error(ex.getMessage());
    }
  }

  @Override
  public PaymentRefundResult executeRefund(PaymentOrderEntity order, PaymentRefundEntity refund) {
    AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
    Map<String, Object> bizContent = new LinkedHashMap<>();
    bizContent.put("out_trade_no", order.getPaymentNo());
    if (StringUtils.hasText(order.getProviderTxnNo())) {
      bizContent.put("trade_no", order.getProviderTxnNo());
    }
    bizContent.put("out_request_no", refund.getRefundNo());
    bizContent.put("refund_amount", refund.getRefundAmount().toPlainString());
    bizContent.put("refund_reason", refund.getReason());
    request.setBizContent(writeJson(bizContent));

    try {
      AlipayTradeRefundResponse response = alipayClient.execute(request);
      if (!response.isSuccess()) {
        return PaymentRefundResult.error(
            buildFailureMessage(response.getSubMsg(), response.getMsg()));
      }
      if ("Y".equalsIgnoreCase(response.getFundChange()) || response.getGmtRefundPay() != null) {
        return PaymentRefundResult.refunded(
            toLocalDateTime(response.getGmtRefundPay()),
            firstNonBlank(response.getFundChange(), "SUCCESS"));
      }
      return PaymentRefundResult.pending(
          firstNonBlank(response.getFundChange(), response.getMsg()));
    } catch (AlipayApiException ex) {
      return PaymentRefundResult.error(ex.getMessage());
    }
  }

  private String writeJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize Alipay request payload", ex);
    }
  }

  private LocalDateTime toLocalDateTime(java.util.Date date) {
    return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), SYSTEM_ZONE);
  }

  private String buildFailureMessage(String subMsg, String msg) {
    return firstNonBlank(subMsg, msg, "provider call failed");
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }

  private String appendCheckoutContext(String url, String paymentNo) {
    if (!StringUtils.hasText(url) || !StringUtils.hasText(paymentNo)) {
      return url;
    }
    String result = url;
    if (!result.contains("paymentNo=")) {
      String separator = result.contains("?") ? "&" : "?";
      result = result + separator + "paymentNo=" + paymentNo;
    }
    if (!result.contains("autoPoll=")) {
      result = result + (result.contains("?") ? "&" : "?") + "autoPoll=1";
    }
    return result;
  }
}
