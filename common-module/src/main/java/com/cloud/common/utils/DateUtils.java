package com.cloud.common.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Date;

/**
 * 通用日期时间工具类
 * 基于Java 8+的Time API实现
 */
public class DateUtils {

    /**
     * 默认日期时间格式
     */
    public static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 默认日期格式
     */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 默认时间格式
     */
    public static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 获取当前时间戳（毫秒）
     *
     * @return 当前时间戳
     */
    public static long currentMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间
     *
     * @return 当前时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前日期
     *
     * @return 当前日期
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * 获取当前时间（java.util.Date类型）
     *
     * @return 当前时间
     */
    public static Date currentDate() {
        return new Date();
    }

    /**
     * 格式化日期时间为字符串
     *
     * @param dateTime 日期时间
     * @param pattern  格式模式
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || pattern == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 格式化日期时间为字符串（使用默认格式）
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DEFAULT_PATTERN);
    }

    /**
     * 格式化日期为字符串
     *
     * @param date    日期
     * @param pattern 格式模式
     * @return 格式化后的字符串
     */
    public static String format(LocalDate date, String pattern) {
        if (date == null || pattern == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 格式化日期为字符串（使用默认格式）
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String format(LocalDate date) {
        return format(date, DATE_PATTERN);
    }

    /**
     * 解析日期时间字符串
     *
     * @param dateTimeStr 日期时间字符串
     * @param pattern     格式模式
     * @return 解析后的日期时间
     */
    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || pattern == null) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 解析日期时间字符串（使用默认格式）
     *
     * @param dateTimeStr 日期时间字符串
     * @return 解析后的日期时间
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return parseDateTime(dateTimeStr, DEFAULT_PATTERN);
    }

    /**
     * 解析日期字符串
     *
     * @param dateStr 日期字符串
     * @param pattern 格式模式
     * @return 解析后的日期
     */
    public static LocalDate parseDate(String dateStr, String pattern) {
        if (dateStr == null || pattern == null) {
            return null;
        }
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 解析日期字符串（使用默认格式）
     *
     * @param dateStr 日期字符串
     * @return 解析后的日期
     */
    public static LocalDate parseDate(String dateStr) {
        return parseDate(dateStr, DATE_PATTERN);
    }

    /**
     * 计算两个日期时间之间的差值（秒）
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 差值（秒）
     */
    public static long diffSeconds(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的差值（分钟）
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 差值（分钟）
     */
    public static long diffMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 计算两个日期时间之间的差值（小时）
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 差值（小时）
     */
    public static long diffHours(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个日期之间的差值（天）
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 差值（天）
     */
    public static long diffDays(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 在指定日期时间上增加秒数
     *
     * @param dateTime 日期时间
     * @param seconds  秒数
     * @return 增加后的日期时间
     */
    public static LocalDateTime plusSeconds(LocalDateTime dateTime, long seconds) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusSeconds(seconds);
    }

    /**
     * 在指定日期时间上增加分钟数
     *
     * @param dateTime 日期时间
     * @param minutes  分钟数
     * @return 增加后的日期时间
     */
    public static LocalDateTime plusMinutes(LocalDateTime dateTime, long minutes) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusMinutes(minutes);
    }

    /**
     * 在指定日期时间上增加小时数
     *
     * @param dateTime 日期时间
     * @param hours    小时数
     * @return 增加后的日期时间
     */
    public static LocalDateTime plusHours(LocalDateTime dateTime, long hours) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusHours(hours);
    }

    /**
     * 在指定日期上增加天数
     *
     * @param date 日期
     * @param days 天数
     * @return 增加后的日期
     */
    public static LocalDate plusDays(LocalDate date, long days) {
        if (date == null) {
            return null;
        }
        return date.plusDays(days);
    }

    /**
     * 在指定日期上增加月数
     *
     * @param date   日期
     * @param months 月数
     * @return 增加后的日期
     */
    public static LocalDate plusMonths(LocalDate date, long months) {
        if (date == null) {
            return null;
        }
        return date.plusMonths(months);
    }

    /**
     * 在指定日期上增加年数
     *
     * @param date  日期
     * @param years 年数
     * @return 增加后的日期
     */
    public static LocalDate plusYears(LocalDate date, long years) {
        if (date == null) {
            return null;
        }
        return date.plusYears(years);
    }

    /**
     * 判断日期是否在指定范围内
     *
     * @param date  日期
     * @param start 开始日期
     * @param end   结束日期
     * @return 是否在范围内
     */
    public static boolean isBetween(LocalDate date, LocalDate start, LocalDate end) {
        if (date == null || start == null || end == null) {
            return false;
        }
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * 判断日期时间是否在指定范围内
     *
     * @param dateTime 日期时间
     * @param start    开始日期时间
     * @param end      结束日期时间
     * @return 是否在范围内
     */
    public static boolean isBetween(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        if (dateTime == null || start == null || end == null) {
            return false;
        }
        return !dateTime.isBefore(start) && !dateTime.isAfter(end);
    }
}