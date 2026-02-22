package com.cloud.common.utils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;





public class CollectionUtils {

    





    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    





    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    





    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    





    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    






    public static <T> T getFirst(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        if (collection instanceof List) {
            return ((List<T>) collection).get(0);
        }
        return collection.iterator().next();
    }

    






    public static <T> T getLast(List<T> list) {
        if (isEmpty(list)) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    







    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return new ArrayList<>();
        }
        return collection.stream().filter(predicate).toList();
    }

    








    public static <T, R> List<R> map(Collection<T> collection, Function<T, R> mapper) {
        if (isEmpty(collection) || mapper == null) {
            return new ArrayList<>();
        }
        return collection.stream().map(mapper).toList();
    }

    







    public static <T> boolean exists(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return false;
        }
        return collection.stream().anyMatch(predicate);
    }

    







    public static <T> T findFirst(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return null;
        }
        return collection.stream().filter(predicate).findFirst().orElse(null);
    }

    






    public static <T> Set<T> toSet(Collection<T> collection) {
        if (isEmpty(collection)) {
            return new HashSet<>();
        }
        return new HashSet<>(collection);
    }

    






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

    







    public static <T> Set<T> intersection(Collection<T> coll1, Collection<T> coll2) {
        if (isEmpty(coll1) || isEmpty(coll2)) {
            return new HashSet<>();
        }
        Set<T> result = new HashSet<>(coll1);
        result.retainAll(coll2);
        return result;
    }

    







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
