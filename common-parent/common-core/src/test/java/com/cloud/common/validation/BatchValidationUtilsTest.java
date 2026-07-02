package com.cloud.common.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.ValidationException;
import com.cloud.common.result.Result;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class BatchValidationUtilsTest {

  @Test
  void shouldRejectEmptyCollections() {
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> BatchValidationUtils.validateBatchSize(Collections.emptyList(), "Delete"));

    assertEquals("batchData", exception.getField());
    assertEquals("Delete batch data must not be empty", exception.getMessage());
  }

  @Test
  void shouldRejectCollectionsAboveLimit() {
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> BatchValidationUtils.validateBatchSize(List.of(1, 2, 3), "Update", 2));

    assertEquals("batchSize", exception.getField());
    assertEquals("Update batch size must not exceed 2, actual size: 3", exception.getMessage());
  }

  @Test
  void shouldReturnNullErrorWhenCollectionIsValid() {
    assertNull(BatchValidationUtils.validateBatchSizeWithError(List.of(1, 2), "Create", 2));
  }

  @Test
  void shouldReturnBadRequestResultForEmptyCollection() {
    Result<String> result =
        BatchValidationUtils.validateBatchSizeWithError(Collections.emptyList(), "Create");

    assertEquals(ResultCode.BAD_REQUEST.getCode(), result.getCode());
    assertEquals("Create batch data must not be empty", result.getMessage());
  }

  @Test
  void shouldRejectInvalidIds() {
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> BatchValidationUtils.validateIdList(List.of(1L, 0L), "Delete"));

    assertEquals("id", exception.getField());
    assertEquals("Delete contains invalid id: 0", exception.getMessage());
  }

  @Test
  void shouldReturnOriginalValidCollection() {
    List<Long> ids = List.of(1L, 2L);

    assertSame(ids, BatchValidationUtils.validateAndReturn(ids, "Delete"));
  }

  @Test
  void shouldAcceptValidArray() {
    assertDoesNotThrow(() -> BatchValidationUtils.validateBatchSize(new Long[] {1L}, "Delete"));
  }
}
