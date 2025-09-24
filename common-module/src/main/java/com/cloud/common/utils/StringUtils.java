package com.cloud.common.utils;

import java.util.Collection;
import java.util.UUID;

/**
 * 通用字符串工具类
 * 提供常用的字符串处理方法
 */
public class StringUtils {

    /**
     * 判断字符串是否为空
     *
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * 判断字符串是否不为空
     *
     * @param str 字符串
     * @return 是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 判断字符串是否为空白字符串
     *
     * @param str 字符串
     * @return 是否为空白字符串
     */
    public static boolean isBlank(String str) {
        if (str == null) {
            return true;
        }
        int len = str.length();
        if (len == 0) {
            return true;
        }
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否不为空白字符串
     *
     * @param str 字符串
     * @return 是否不为空白字符串
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 去除字符串两端空白字符
     *
     * @param str 字符串
     * @return 去除两端空白字符后的字符串
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * 将字符串首字母大写
     *
     * @param str 字符串
     * @return 首字母大写的字符串
     */
    public static String capitalize(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        char firstChar = str.charAt(0);
        if (Character.isTitleCase(firstChar)) {
            return str;
        }
        return Character.toTitleCase(firstChar) + str.substring(1);
    }

    /**
     * 将字符串首字母小写
     *
     * @param str 字符串
     * @return 首字母小写的字符串
     */
    public static String uncapitalize(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        char firstChar = str.charAt(0);
        if (Character.isLowerCase(firstChar)) {
            return str;
        }
        return Character.toLowerCase(firstChar) + str.substring(1);
    }

    /**
     * 生成UUID字符串（不包含横线）
     *
     * @return UUID字符串
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 将集合元素用指定分隔符连接成字符串
     *
     * @param collection 集合
     * @param separator  分隔符
     * @return 连接后的字符串
     */
    public static String join(Collection<?> collection, String separator) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object obj : collection) {
            if (first) {
                first = false;
            } else {
                sb.append(separator);
            }
            sb.append(obj);
        }
        return sb.toString();
    }

    /**
     * 检查字符串是否包含指定的子字符串（忽略大小写）
     *
     * @param str    字符串
     * @param search 搜索的子字符串
     * @return 是否包含
     */
    public static boolean containsIgnoreCase(String str, String search) {
        if (str == null || search == null) {
            return false;
        }
        return str.toLowerCase().contains(search.toLowerCase());
    }

    // ==================== 统一的ID生成方法 ====================

    /**
     * 生成追踪ID
     * 统一的追踪ID生成方法，用于分布式链路追踪
     *
     * @return 追踪ID
     */
    public static String generateTraceId() {
        return uuid();
    }

    /**
     * 生成日志ID
     * 统一的日志ID生成方法
     *
     * @param prefix 前缀
     * @return 日志ID
     */
    public static String generateLogId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + uuid().substring(0, 8);
    }

    // ==================== 统一的脱敏方法 ====================

    /**
     * 手机号脱敏处理
     * 保留前3位和后4位，中间用*代替
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏处理
     * 保留@前的第一个字符和@后的域名，中间用*代替
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts[0].length() <= 1) {
            return email;
        }
        return parts[0].charAt(0) + "***@" + parts[1];
    }

    /**
     * 银行卡号脱敏处理
     * 保留前4位和后4位，中间用*代替
     *
     * @param cardNumber 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return cardNumber;
        }
        return cardNumber.substring(0, 4) + "****" + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * 通用敏感信息脱敏
     * 对文本中的手机号、邮箱、银行卡号进行脱敏
     *
     * @param content 原始内容
     * @return 脱敏后的内容
     */
    public static String sanitizeContent(String content) {
        if (content == null) {
            return null;
        }
        return content.replaceAll("\\d{11}", "***")  // 手机号
                .replaceAll("\\d{15,19}", "***")  // 银行卡号
                .replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "***@***.com");  // 邮箱
    }

    /**
     * 截断过长的文本
     *
     * @param text      原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    /**
     * 生成唯一用户名（处理用户名冲突）
     *
     * @param baseUsername  基础用户名
     * @param prefix        前缀
     * @param checkFunction 检查用户名是否存在的函数
     * @return 唯一的用户名
     */
    public static String generateUniqueUsername(String baseUsername, String prefix,
                                                java.util.function.Function<String, Boolean> checkFunction) {
        String candidateUsername = prefix + baseUsername;
        int suffix = 1;

        while (checkFunction.apply(candidateUsername)) {
            candidateUsername = prefix + baseUsername + "_" + suffix;
            suffix++;
            if (suffix > 1000) { // 防止无限循环
                candidateUsername = prefix + baseUsername + "_" + System.currentTimeMillis();
                break;
            }
        }

        return candidateUsername;
    }
}