package com.cloud.payment.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;







@Slf4j
public class AlipayUtils {

    


    private static final String OUT_TRADE_NO_PREFIX = "PAY_";

    


    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    





    public static String generateOutTradeNo(Long orderId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        return String.format("%s%s_%d", OUT_TRADE_NO_PREFIX, timestamp, orderId);
    }

    





    public static Long extractOrderIdFromOutTradeNo(String outTradeNo) {
        try {
            
            String[] parts = outTradeNo.split("_");
            if (parts.length >= 3) {
                return Long.parseLong(parts[2]);
            }
        } catch (NumberFormatException e) {
            log.error("瑙ｆ瀽鍟嗘埛璁㈠崟鍙峰け璐?- 璁㈠崟鍙? {}", outTradeNo, e);
        }
        return null;
    }

    





    public static boolean isValidOutTradeNo(String outTradeNo) {
        if (outTradeNo == null || outTradeNo.trim().isEmpty()) {
            return false;
        }

        return outTradeNo.startsWith(OUT_TRADE_NO_PREFIX) &&
                outTradeNo.split("_").length >= 3;
    }

    





    public static boolean isPaymentSuccess(String tradeStatus) {
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }

    





    public static boolean isPaymentClosed(String tradeStatus) {
        return "TRADE_CLOSED".equals(tradeStatus);
    }

    





    public static String getTradeStatusDescription(String tradeStatus) {
        if (tradeStatus == null) {
            return "鏈煡鐘舵€?;
        }

        switch (tradeStatus) {
            case "WAIT_BUYER_PAY":
                return "浜ゆ槗鍒涘缓锛岀瓑寰呬拱瀹朵粯娆?;
            case "TRADE_CLOSED":
                return "鏈粯娆句氦鏄撹秴鏃跺叧闂紝鎴栨敮浠樺畬鎴愬悗鍏ㄩ閫€娆?;
            case "TRADE_SUCCESS":
                return "浜ゆ槗鏀粯鎴愬姛";
            case "TRADE_FINISHED":
                return "浜ゆ槗缁撴潫锛屼笉鍙€€娆?;
            default:
                return "鏈煡鐘舵€? " + tradeStatus;
        }
    }

    





    public static boolean validateNotifyParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }

        
        String[] requiredParams = {
                "trade_status", "out_trade_no", "trade_no", "total_amount", "app_id"
        };

        for (String param : requiredParams) {
            if (!params.containsKey(param) || params.get(param) == null || params.get(param).trim().isEmpty()) {
                log.error("缂哄皯蹇呰鍙傛暟: {}", param);
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
            
            String[] parts = buyerLogonId.split("@");
            String username = parts[0];
            String domain = parts[1];

            if (username.length() <= 2) {
                return buyerLogonId;
            }

            return username.substring(0, 2) + "***@" + domain;
        } else {
            
            if (buyerLogonId.length() == 11) {
                return buyerLogonId.substring(0, 3) + "****" + buyerLogonId.substring(7);
            } else {
                return buyerLogonId.substring(0, 2) + "***";
            }
        }
    }
}
