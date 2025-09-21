package com.cloud.common.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean拷贝工具类
 *
 * @author what's up
 */
public class BeanCopyUtils {

    /**
     * 单个对象拷贝
     *
     * @param source      源对象
     * @param targetClass 目标对象类型
     * @return 拷贝后的目标对象
     */
    public static <T> T copyProperties(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Bean拷贝失败", e);
        }
    }

    /**
     * 列表对象拷贝
     *
     * @param sourceList  源对象列表
     * @param targetClass 目标对象类型
     * @return 拷贝后的目标对象列表
     */
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

    /**
     * 对象拷贝（指定忽略属性）
     *
     * @param source           源对象
     * @param targetClass      目标对象类型
     * @param ignoreProperties 忽略的属性名
     * @return 拷贝后的目标对象
     */
    public static <T> T copyProperties(Object source, Class<T> targetClass, String... ignoreProperties) {
        if (source == null) {
            return null;
        }

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target, ignoreProperties);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Bean拷贝失败", e);
        }
    }
}
