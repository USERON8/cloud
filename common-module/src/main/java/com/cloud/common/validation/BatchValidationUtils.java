package com.cloud.common.validation;

import com.cloud.common.exception.ValidationException;
import com.cloud.common.result.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;

/**
 * 批量操作通用校验工具类
 * 提供统一的批量操作参数校验，避免重复代码
 *
 * @author cloud
 */
@Slf4j
public class BatchValidationUtils {

    /**
     * 默认批量操作最大数量
     */
    public static final int DEFAULT_BATCH_SIZE_LIMIT = 100;

    /**
     * 校验批量操作数量限制
     *
     * @param collection    集合对象
     * @param operationName 操作名称
     * @throws ValidationException 校验失败时抛出
     */
    public static void validateBatchSize(Collection<?> collection, String operationName) {
        validateBatchSize(collection, operationName, DEFAULT_BATCH_SIZE_LIMIT);
    }

    /**
     * 校验批量操作数量限制
     *
     * @param collection    集合对象
     * @param operationName 操作名称
     * @param limit         数量限制
     * @throws ValidationException 校验失败时抛出
     */
    public static void validateBatchSize(Collection<?> collection, String operationName, int limit) {
        if (collection == null || collection.isEmpty()) {
            throw new ValidationException("batchData", collection, String.format("%s数据列表不能为空", operationName));
        }

        if (collection.size() > limit) {
            throw new ValidationException("batchSize", collection.size(),
                    String.format("%s数量不能超过%d个，当前数量: %d", operationName, limit, collection.size()));
        }
    }

    /**
     * 校验批量操作数量限制，返回Result格式的错误
     *
     * @param collection    集合对象
     * @param operationName 操作名称
     * @return 校验失败时返回错误Result，成功时返回null
     */
    public static Result<String> validateBatchSizeWithError(Collection<?> collection, String operationName) {
        return validateBatchSizeWithError(collection, operationName, DEFAULT_BATCH_SIZE_LIMIT);
    }

    /**
     * 校验批量操作数量限制，返回Result格式的错误
     *
     * @param collection    集合对象
     * @param operationName 操作名称
     * @param limit         数量限制
     * @return 校验失败时返回错误Result，成功时返回null
     */
    public static Result<String> validateBatchSizeWithError(Collection<?> collection, String operationName, int limit) {
        if (collection == null || collection.isEmpty()) {
            return Result.badRequest(String.format("%s数据列表不能为空", operationName));
        }

        if (collection.size() > limit) {
            return Result.error(String.format("%s数量不能超过%d个，当前数量: %d", operationName, limit, collection.size()));
        }

        return null; // 校验成功
    }

    /**
     * 校验数组批量操作数量限制
     *
     * @param array         数组对象
     * @param operationName 操作名称
     * @throws ValidationException 校验失败时抛出
     */
    public static void validateBatchSize(Object[] array, String operationName) {
        validateBatchSize(array, operationName, DEFAULT_BATCH_SIZE_LIMIT);
    }

    /**
     * 校验数组批量操作数量限制
     *
     * @param array         数组对象
     * @param operationName 操作名称
     * @param limit         数量限制
     * @throws ValidationException 校验失败时抛出
     */
    public static void validateBatchSize(Object[] array, String operationName, int limit) {
        if (array == null || array.length == 0) {
            throw new ValidationException("batchArray", array, String.format("%s数组不能为空", operationName));
        }

        if (array.length > limit) {
            throw new ValidationException("batchSize", array.length,
                    String.format("%s数量不能超过%d个，当前数量: %d", operationName, limit, array.length));
        }
    }

    /**
     * 校验ID列表
     *
     * @param ids           ID列表
     * @param operationName 操作名称
     */
    public static void validateIdList(List<Long> ids, String operationName) {
        validateBatchSize(ids, operationName);

        // 校验ID的有效性
        for (Long id : ids) {
            if (id == null || id <= 0) {
                throw new ValidationException("id", id, String.format("%s包含无效的ID: %s", operationName, id));
            }
        }
    }

    /**
     * 校验ID数组
     *
     * @param ids           ID数组
     * @param operationName 操作名称
     */
    public static void validateIdArray(Long[] ids, String operationName) {
        validateBatchSize(ids, operationName);

        // 校验ID的有效性
        for (Long id : ids) {
            if (id == null || id <= 0) {
                throw new ValidationException("id", id, String.format("%s包含无效的ID: %s", operationName, id));
            }
        }
    }

    /**
     * 快速校验并返回批量操作参数
     *
     * @param collection    集合对象
     * @param operationName 操作名称
     * @return 校验通过的集合
     * @throws ValidationException 校验失败时抛出
     */
    public static <T> Collection<T> validateAndReturn(Collection<T> collection, String operationName) {
        validateBatchSize(collection, operationName);
        return collection;
    }

    /**
     * 快速校验并返回批量操作参数
     *
     * @param array         数组对象
     * @param operationName 操作名称
     * @return 校验通过的数组
     * @throws ValidationException 校验失败时抛出
     */
    public static <T> T[] validateAndReturn(T[] array, String operationName) {
        validateBatchSize(array, operationName);
        return array;
    }
}