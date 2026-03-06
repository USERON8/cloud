package com.cloud.common.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;







public class PageUtils {

    






    public static <T> Page<T> buildPage(Object pageDTO) {
        
        try {
            
            java.lang.reflect.Method getCurrentMethod = pageDTO.getClass().getMethod("getCurrent");
            java.lang.reflect.Method getSizeMethod = pageDTO.getClass().getMethod("getSize");

            Long current = (Long) getCurrentMethod.invoke(pageDTO);
            Long size = (Long) getSizeMethod.invoke(pageDTO);

            current = current != null ? current : 1L;
            size = size != null ? size : 10L;

            return new Page<>(current, size);
        } catch (Exception e) {
            
            return new Page<>(1, 10);
        }
    }

    







    public static <T> Page<T> buildPage(long current, long size) {
        return new Page<>(current, size);
    }
}
