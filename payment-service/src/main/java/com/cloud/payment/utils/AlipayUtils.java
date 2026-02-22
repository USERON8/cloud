package com.cloud.payment.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
public final class AlipayUtils {

    private static final String OUT_TRADE_NO_PREFIX = "PAY_";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private AlipayUtils() {
    }

    public static String generateOutTradeNo(Long orderId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        return String.format("%s%s_%d", OUT_TRADE_NO_PREFIX, timestamp, orderId);
    }

    public static Long extractOrderIdFromOutTradeNo(String outTradeNo) {
        if (!isValidOutTradeNo(outTradeNo)) {
            return null;
        }
        try {
            String[] parts = outTradeNo.split("_");
            return Long.parseLong(parts[2]);
        } catch (Exception e) {
            log.error("Failed to extract order id from outTradeNo: {}", outTradeNo, e);
            return null;
        }
    }

    public static boolean isValidOutTradeNo(String outTradeNo) {
        if (outTradeNo == null || outTradeNo.trim().isEmpty()) {
            return false;
        }
        return outTradeNo.startsWith(OUT_TRADE_NO_PREFIX) && outTradeNo.split("_").length >= 3;
    }

    public static boolean isPaymentSuccess(String tradeStatus) {
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }

    public static boolean isPaymentClosed(String tradeStatus) {
        return "TRADE_CLOSED".equals(tradeStatus);
    }

    public static String getTradeStatusDescription(String tradeStatus) {
        if (tradeStatus == null) {
            return "UNKNOWN";
        }
        return switch (tradeStatus) {
            case "WAIT_BUYER_PAY" -> "WAIT_BUYER_PAY";
            case "TRADE_CLOSED" -> "TRADE_CLOSED";
            case "TRADE_SUCCESS" -> "TRADE_SUCCESS";
            case "TRADE_FINISHED" -> "TRADE_FINISHED";
            default -> "UNKNOWN: " + tradeStatus;
        };
    }

    public static boolean validateNotifyParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        String[] requiredParams = {
                "trade_status", "out_trade_no", "trade_no", "total_amount", "app_id"
        };
        for (String required : requiredParams) {
            String value = params.get(required);
            if (value == null || value.trim().isEmpty()) {
                log.error("Missing required notify param: {}", required);
                return false;
            }
        }
        return true;
    }

    public static String maskTradeNo(String tradeNo) {
        if (tradeNo == null || tradeNo.length() <= 8) {
            return tradeNo;
        }
        int length = tradeNo.length();
        return tradeNo.substring(0, 4) + "****" + tradeNo.substring(length - 4);
    }

    public static String maskBuyerLogonId(String buyerLogonId) {
        if (buyerLogonId == null || buyerLogonId.length() <= 4) {
            return buyerLogonId;
        }
        if (buyerLogonId.contains("@")) {
            String[] parts = buyerLogonId.split("@", 2);
            String username = parts[0];
            String domain = parts[1];
            if (username.length() <= 2) {
                return buyerLogonId;
            }
            return username.substring(0, 2) + "***@" + domain;
        }
        if (buyerLogonId.length() == 11) {
            return buyerLogonId.substring(0, 3) + "****" + buyerLogonId.substring(7);
        }
        return buyerLogonId.substring(0, 2) + "***";
    }
}
