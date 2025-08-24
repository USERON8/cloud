package com.cloud.common.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用Bean工具类
 * 提供对象属性复制、类型转换等常用功能
 */
public class BeanUtils {

    /**
     * 复制源对象的属性到目标对象
     * 只复制名称和类型都相同的属性
     *
     * @param source 源对象
     * @param target 目标对象
     * @param <T>    源对象类型
     * @param <R>    目标对象类型
     */
    public static <T, R> void copyProperties(T source, R target) {
        if (source == null || target == null) {
            return;
        }

        Class<?> sourceClass = source.getClass();
        Class<?> targetClass = target.getClass();

        // 获取源对象的所有字段
        Field[] sourceFields = sourceClass.getDeclaredFields();
        Field[] targetFields = targetClass.getDeclaredFields();

        for (Field sourceField : sourceFields) {
            for (Field targetField : targetFields) {
                // 如果字段名称和类型都相同，则复制值
                if (sourceField.getName().equals(targetField.getName()) &&
                        sourceField.getType().equals(targetField.getType())) {
                    try {
                        sourceField.setAccessible(true);
                        targetField.setAccessible(true);
                        Object value = sourceField.get(source);
                        targetField.set(target, value);
                    } catch (IllegalAccessException e) {
                        // 忽略无法访问的字段
                    }
                    break;
                }
            }
        }
    }

    /**
     * 复制源对象列表到目标对象列表
     *
     * @param sourceList 源对象列表
     * @param targetClass 目标对象类型
     * @param <T> 源对象类型
     * @param <R> 目标对象类型
     * @return 目标对象列表
     */
    public static <T, R> List<R> copyList(List<T> sourceList, Class<R> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }

        List<R> targetList = new ArrayList<>();
        for (T source : sourceList) {
            try {
                R target = targetClass.getDeclaredConstructor().newInstance();
                copyProperties(source, target);
                targetList.add(target);
            } catch (Exception e) {
                // 忽略创建实例失败的情况
            }
        }
        return targetList;
    }

    /**
     * 将源对象转换为目标对象
     *
     * @param source 源对象
     * @param targetClass 目标对象类型
     * @param <T> 源对象类型
     * @param <R> 目标对象类型
     * @return 目标对象
     */
    public static <T, R> R convert(T source, Class<R> targetClass) {
        if (source == null) {
            return null;
        }

        try {
            R target = targetClass.getDeclaredConstructor().newInstance();
            copyProperties(source, target);
            return target;
        } catch (Exception e) {
            return null;
        }
    }
}