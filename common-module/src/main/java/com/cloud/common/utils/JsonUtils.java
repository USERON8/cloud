package com.cloud.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * 通用JSON工具类
 * 基于Jackson实现JSON序列化和反序列化
 * 适配Spring Boot 3.5.3版本
 */
@Slf4j
public class JsonUtils {

    /**
     * ObjectMapper实例
     * -- GETTER --
     * 获取配置好的ObjectMapper实例
     */
    @Getter
    private static final ObjectMapper objectMapper = createObjectMapper();

    /**
     * 创建并配置ObjectMapper实例
     *
     * @return 配置好的ObjectMapper实例
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 配置ObjectMapper
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setTimeZone(TimeZone.getDefault());
        mapper.setDateFormat(new SimpleDateFormat(DateUtils.DEFAULT_PATTERN));

        // 注册JavaTimeModule以支持Java 8时间API
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }

    /**
     * 将对象序列化为JSON字符串
     *
     * @param obj 对象
     * @return JSON字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象序列化为JSON失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将JSON字符串反序列化为指定类型的对象
     *
     * @param json  JSON字符串
     * @param clazz 对象类型
     * @param <T>   对象类型
     * @return 对象实例
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || clazz == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("JSON反序列化失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将JSON字符串反序列化为复杂类型的对象（如List、Map等）
     *
     * @param json          JSON字符串
     * @param typeReference 类型引用
     * @param <T>           对象类型
     * @return 对象实例
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || typeReference == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            log.error("JSON反序列化失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将对象转换为指定类型的对象（深拷贝）
     *
     * @param source 源对象
     * @param clazz  目标类型
     * @param <T>    源对象类型
     * @param <R>    目标对象类型
     * @return 目标对象
     */
    public static <T, R> R convert(T source, Class<R> clazz) {
        if (source == null || clazz == null) {
            return null;
        }
        try {
            String json = toJson(source);
            return fromJson(json, clazz);
        } catch (Exception e) {
            log.error("对象转换失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将对象格式化为美化后的JSON字符串（带缩进）
     *
     * @param obj 对象
     * @return 美化后的JSON字符串
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象序列化为美化JSON失败: {}", e.getMessage(), e);
            return null;
        }
    }
}