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






@Slf4j
public class JsonUtils {

    




    @Getter
    private static final ObjectMapper objectMapper = createObjectMapper();

    




    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setTimeZone(TimeZone.getDefault());
        mapper.setDateFormat(new SimpleDateFormat(DateUtils.DEFAULT_PATTERN));

        
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }

    





    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("瀵硅薄搴忓垪鍖栦负JSON澶辫触: {}", e.getMessage(), e);
            return null;
        }
    }

    







    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || clazz == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("JSON鍙嶅簭鍒楀寲澶辫触: {}", e.getMessage(), e);
            return null;
        }
    }

    







    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || typeReference == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            log.error("JSON鍙嶅簭鍒楀寲澶辫触: {}", e.getMessage(), e);
            return null;
        }
    }

    








    public static <T, R> R convert(T source, Class<R> clazz) {
        if (source == null || clazz == null) {
            return null;
        }
        try {
            String json = toJson(source);
            return fromJson(json, clazz);
        } catch (Exception e) {
            log.error("瀵硅薄杞崲澶辫触: {}", e.getMessage(), e);
            return null;
        }
    }

    





    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("瀵硅薄搴忓垪鍖栦负缇庡寲JSON澶辫触: {}", e.getMessage(), e);
            return null;
        }
    }
}
