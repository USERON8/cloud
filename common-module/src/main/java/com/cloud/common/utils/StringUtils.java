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
}