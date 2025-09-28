package com.cloud.common.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页工具类
 * 
 * @author CloudDevAgent
 * @since 2025-09-28
 */
public class PageUtils {
    
    /**
     * 构建分页对象
     * 
     * @param pageDTO 分页DTO
     * @param <T> 实体类型
     * @return 分页对象
     */
    public static <T> Page<T> buildPage(Object pageDTO) {
        // 默认实现，可以根据实际的PageDTO结构调整
        try {
            // 通过反射获取页码和页大小
            java.lang.reflect.Method getCurrentMethod = pageDTO.getClass().getMethod("getCurrent");
            java.lang.reflect.Method getSizeMethod = pageDTO.getClass().getMethod("getSize");
            
            Long current = (Long) getCurrentMethod.invoke(pageDTO);
            Long size = (Long) getSizeMethod.invoke(pageDTO);
            
            current = current != null ? current : 1L;
            size = size != null ? size : 10L;
            
            return new Page<>(current, size);
        } catch (Exception e) {
            // 如果反射失败，使用默认值
            return new Page<>(1, 10);
        }
    }
    
    /**
     * 构建分页对象 - 简化版本
     * 
     * @param current 当前页
     * @param size 页大小
     * @param <T> 实体类型
     * @return 分页对象
     */
    public static <T> Page<T> buildPage(long current, long size) {
        return new Page<>(current, size);
    }
}
