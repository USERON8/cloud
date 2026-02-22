package com.cloud.common.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;






public class BeanCopyUtils {

    






    public static <T> T copyProperties(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Bean鎷疯礉澶辫触", e);
        }
    }

    






    public static <S, T> List<T> copyListProperties(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }

        List<T> targetList = new ArrayList<>();
        for (S source : sourceList) {
            T target = copyProperties(source, targetClass);
            if (target != null) {
                targetList.add(target);
            }
        }
        return targetList;
    }

    







    public static <T> T copyProperties(Object source, Class<T> targetClass, String... ignoreProperties) {
        if (source == null) {
            return null;
        }

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target, ignoreProperties);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Bean鎷疯礉澶辫触", e);
        }
    }
}
