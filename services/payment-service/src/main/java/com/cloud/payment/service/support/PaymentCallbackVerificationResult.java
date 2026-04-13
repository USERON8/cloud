package com.cloud.payment.service.support;

import java.math.BigDecimal;

public record PaymentCallbackVerificationResult(
    String normalizedStatus,
    String provider,
    String providerEventType,
    String verifiedAppId,
    String verifiedSellerId,
    String providerTxnNo,
    BigDecimal amount,
    String payload,
    String rawPayloadHash) {}
