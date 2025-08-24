package com.cloud.common.utils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 通用集合工具类
 * 提供常用的集合处理方法
 */
public class CollectionUtils {

    /**
     * 判断集合是否为空
     *
     * @param collection 集合
     * @return 是否为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断集合是否不为空
     *
     * @param collection 集合
     * @return 是否不为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 判断Map是否为空
     *
     * @param map Map
     * @return 是否为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断Map是否不为空
     *
     * @param map Map
     * @return 是否不为空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 获取集合的第一个元素
     *
     * @param collection 集合
     * @param <T>        元素类型
     * @return 第一个元素，如果集合为空则返回null
     */
    public static <T> T getFirst(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        if (collection instanceof List) {
            return ((List<T>) collection).get(0);
        }
        return collection.iterator().next();
    }

    /**
     * 获取集合的最后一个元素
     *
     * @param list 集合
     * @param <T>  元素类型
     * @return 最后一个元素，如果集合为空则返回null
     */
    public static <T> T getLast(List<T> list) {
        if (isEmpty(list)) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    /**
     * 根据条件过滤集合
     *
     * @param collection 集合
     * @param predicate  过滤条件
     * @param <T>        元素类型
     * @return 过滤后的集合
     */
    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return new ArrayList<>();
        }
        return collection.stream().filter(predicate).toList();
    }

    /**
     * 将集合转换为另一个类型的集合
     *
     * @param collection 集合
     * @param mapper     转换函数
     * @param <T>        源元素类型
     * @param <R>        目标元素类型
     * @return 转换后的集合
     */
    public static <T, R> List<R> map(Collection<T> collection, Function<T, R> mapper) {
        if (isEmpty(collection) || mapper == null) {
            return new ArrayList<>();
        }
        return collection.stream().map(mapper).toList();
    }

    /**
     * 检查集合中是否存在满足条件的元素
     *
     * @param collection 集合
     * @param predicate  条件
     * @param <T>        元素类型
     * @return 是否存在满足条件的元素
     */
    public static <T> boolean exists(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return false;
        }
        return collection.stream().anyMatch(predicate);
    }

    /**
     * 查找集合中满足条件的第一个元素
     *
     * @param collection 集合
     * @param predicate  条件
     * @param <T>        元素类型
     * @return 满足条件的第一个元素，如果不存在则返回null
     */
    public static <T> T findFirst(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return null;
        }
        return collection.stream().filter(predicate).findFirst().orElse(null);
    }

    /**
     * 将集合转换为Set去重
     *
     * @param collection 集合
     * @param <T>        元素类型
     * @return 去重后的Set
     */
    public static <T> Set<T> toSet(Collection<T> collection) {
        if (isEmpty(collection)) {
            return new HashSet<>();
        }
        return new HashSet<>(collection);
    }

    /**
     * 合并多个集合
     *
     * @param collections 集合数组
     * @param <T>         元素类型
     * @return 合并后的集合
     */
    @SafeVarargs
    public static <T> List<T> merge(Collection<T>... collections) {
        List<T> result = new ArrayList<>();
        if (collections == null) {
            return result;
        }
        for (Collection<T> collection : collections) {
            if (isNotEmpty(collection)) {
                result.addAll(collection);
            }
        }
        return result;
    }

    /**
     * 获取两个集合的交集
     *
     * @param coll1 第一个集合
     * @param coll2 第二个集合
     * @param <T>   元素类型
     * @return 交集
     */
    public static <T> Set<T> intersection(Collection<T> coll1, Collection<T> coll2) {
        if (isEmpty(coll1) || isEmpty(coll2)) {
            return new HashSet<>();
        }
        Set<T> result = new HashSet<>(coll1);
        result.retainAll(coll2);
        return result;
    }

    /**
     * 获取两个集合的并集
     *
     * @param coll1 第一个集合
     * @param coll2 第二个集合
     * @param <T>   元素类型
     * @return 并集
     */
    public static <T> Set<T> union(Collection<T> coll1, Collection<T> coll2) {
        Set<T> result = new HashSet<>();
        if (isNotEmpty(coll1)) {
            result.addAll(coll1);
        }
        if (isNotEmpty(coll2)) {
            result.addAll(coll2);
        }
        return result;
    }

    /**
     * 获取两个集合的差集
     *
     * @param coll1 第一个集合
     * @param coll2 第二个集合
     * @param <T>   元素类型
     * @return 差集
     */
    public static <T> Set<T> difference(Collection<T> coll1, Collection<T> coll2) {
        if (isEmpty(coll1)) {
            return new HashSet<>();
        }
        Set<T> result = new HashSet<>(coll1);
        if (isNotEmpty(coll2)) {
            result.removeAll(coll2);
        }
        return result;
    }
}