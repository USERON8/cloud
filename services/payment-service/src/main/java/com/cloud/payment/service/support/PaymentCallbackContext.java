package com.cloud.payment.service.support;

public record PaymentCallbackContext(
    String provider,
    String providerEventType,
    String verifiedAppId,
    String verifiedSellerId,
    String rawPayloadHash) {}
