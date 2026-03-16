package com.cloud.common.validation;

import com.cloud.common.exception.ValidationException;
import com.cloud.common.result.Result;
import java.util.Collection;
import java.util.List;

public class BatchValidationUtils {

  public static final int DEFAULT_BATCH_SIZE_LIMIT = 100;

  public static void validateBatchSize(Collection<?> collection, String operationName) {
    validateBatchSize(collection, operationName, DEFAULT_BATCH_SIZE_LIMIT);
  }

  public static void validateBatchSize(Collection<?> collection, String operationName, int limit) {
    if (collection == null || collection.isEmpty()) {
      throw new ValidationException(
          "batchData", collection, String.format("%s鏁版嵁鍒楄〃涓嶈兘涓虹┖", operationName));
    }

    if (collection.size() > limit) {
      throw new ValidationException(
          "batchSize",
          collection.size(),
          String.format("%s鏁伴噺涓嶈兘瓒呰繃%d涓紝褰撳墠鏁伴噺: %d", operationName, limit, collection.size()));
    }
  }

  public static Result<String> validateBatchSizeWithError(
      Collection<?> collection, String operationName) {
    return validateBatchSizeWithError(collection, operationName, DEFAULT_BATCH_SIZE_LIMIT);
  }

  public static Result<String> validateBatchSizeWithError(
      Collection<?> collection, String operationName, int limit) {
    if (collection == null || collection.isEmpty()) {
      return Result.badRequest(String.format("%s鏁版嵁鍒楄〃涓嶈兘涓虹┖", operationName));
    }

    if (collection.size() > limit) {
      return Result.error(
          String.format("%s鏁伴噺涓嶈兘瓒呰繃%d涓紝褰撳墠鏁伴噺: %d", operationName, limit, collection.size()));
    }

    return null;
  }

  public static void validateBatchSize(Object[] array, String operationName) {
    validateBatchSize(array, operationName, DEFAULT_BATCH_SIZE_LIMIT);
  }

  public static void validateBatchSize(Object[] array, String operationName, int limit) {
    if (array == null || array.length == 0) {
      throw new ValidationException(
          "batchArray", array, String.format("%s鏁扮粍涓嶈兘涓虹┖", operationName));
    }

    if (array.length > limit) {
      throw new ValidationException(
          "batchSize",
          array.length,
          String.format("%s鏁伴噺涓嶈兘瓒呰繃%d涓紝褰撳墠鏁伴噺: %d", operationName, limit, array.length));
    }
  }

  public static void validateIdList(List<Long> ids, String operationName) {
    validateBatchSize(ids, operationName);

    for (Long id : ids) {
      if (id == null || id <= 0) {
        throw new ValidationException(
            "id", id, String.format("%s鍖呭惈鏃犳晥鐨処D: %s", operationName, id));
      }
    }
  }

  public static void validateIdArray(Long[] ids, String operationName) {
    validateBatchSize(ids, operationName);

    for (Long id : ids) {
      if (id == null || id <= 0) {
        throw new ValidationException(
            "id", id, String.format("%s鍖呭惈鏃犳晥鐨処D: %s", operationName, id));
      }
    }
  }

  public static <T> Collection<T> validateAndReturn(
      Collection<T> collection, String operationName) {
    validateBatchSize(collection, operationName);
    return collection;
  }

  public static <T> T[] validateAndReturn(T[] array, String operationName) {
    validateBatchSize(array, operationName);
    return array;
  }
}
