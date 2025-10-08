package com.cloud.payment.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 支付宝支付工具类
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
public class AlipayUtils {

    /**
     * 商户订单号前缀
     */
    private static final String OUT_TRADE_NO_PREFIX = "PAY_";

    /**
     * 时间格式
     */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 生成商户订单号
     *
     * @param orderId 订单ID
     * @return 商户订单号
     */
    public static String generateOutTradeNo(Long orderId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        return String.format("%s%s_%d", OUT_TRADE_NO_PREFIX, timestamp, orderId);
    }

    /**
     * 从商户订单号中提取订单ID
     *
     * @param outTradeNo 商户订单号
     * @return 订单ID
     */
    public static Long extractOrderIdFromOutTradeNo(String outTradeNo) {
        try {
            // 商户订单号格式: PAY_yyyyMMddHHmmss_orderId
            String[] parts = outTradeNo.split("_");
            if (parts.length >= 3) {
                return Long.parseLong(parts[2]);
            }
        } catch (NumberFormatException e) {
            log.error("解析商户订单号失败 - 订单号: {}", outTradeNo, e);
        }
        return null;
    }

    /**
     * 验证商户订单号格式
     *
     * @param outTradeNo 商户订单号
     * @return 是否有效
     */
    public static boolean isValidOutTradeNo(String outTradeNo) {
        if (outTradeNo == null || outTradeNo.trim().isEmpty()) {
            return false;
        }

        return outTradeNo.startsWith(OUT_TRADE_NO_PREFIX) &&
                outTradeNo.split("_").length >= 3;
    }

    /**
     * 判断支付状态是否为成功
     *
     * @param tradeStatus 交易状态
     * @return 是否成功
     */
    public static boolean isPaymentSuccess(String tradeStatus) {
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }

    /**
     * 判断支付状态是否为关闭
     *
     * @param tradeStatus 交易状态
     * @return 是否关闭
     */
    public static boolean isPaymentClosed(String tradeStatus) {
        return "TRADE_CLOSED".equals(tradeStatus);
    }

    /**
     * 获取支付状态描述
     *
     * @param tradeStatus 交易状态
     * @return 状态描述
     */
    public static String getTradeStatusDescription(String tradeStatus) {
        if (tradeStatus == null) {
            return "未知状态";
        }

        switch (tradeStatus) {
            case "WAIT_BUYER_PAY":
                return "交易创建，等待买家付款";
            case "TRADE_CLOSED":
                return "未付款交易超时关闭，或支付完成后全额退款";
            case "TRADE_SUCCESS":
                return "交易支付成功";
            case "TRADE_FINISHED":
                return "交易结束，不可退款";
            default:
                return "未知状态: " + tradeStatus;
        }
    }

    /**
     * 验证通知参数完整性
     *
     * @param params 通知参数
     * @return 是否完整
     */
    public static boolean validateNotifyParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }

        // 检查必要参数
        String[] requiredParams = {
                "trade_status", "out_trade_no", "trade_no", "total_amount", "app_id"
        };

        for (String param : requiredParams) {
            if (!params.containsKey(param) || params.get(param) == null || params.get(param).trim().isEmpty()) {
                log.error("缺少必要参数: {}", param);
                return false;
            }
        }

        return true;
    }

    /**
     * 脱敏支付宝交易号
     *
     * @param tradeNo 交易号
     * @return 脱敏后的交易号
     */
    public static String maskTradeNo(String tradeNo) {
        if (tradeNo == null || tradeNo.length() <= 8) {
            return tradeNo;
        }

        int length = tradeNo.length();
        return tradeNo.substring(0, 4) + "****" + tradeNo.substring(length - 4);
    }

    /**
     * 脱敏买家账号
     *
     * @param buyerLogonId 买家账号
     * @return 脱敏后的账号
     */
    public static String maskBuyerLogonId(String buyerLogonId) {
        if (buyerLogonId == null || buyerLogonId.length() <= 4) {
            return buyerLogonId;
        }

        if (buyerLogonId.contains("@")) {
            // 邮箱格式
            String[] parts = buyerLogonId.split("@");
            String username = parts[0];
            String domain = parts[1];

            if (username.length() <= 2) {
                return buyerLogonId;
            }

            return username.substring(0, 2) + "***@" + domain;
        } else {
            // 手机号格式
            if (buyerLogonId.length() == 11) {
                return buyerLogonId.substring(0, 3) + "****" + buyerLogonId.substring(7);
            } else {
                return buyerLogonId.substring(0, 2) + "***";
            }
        }
    }
}
