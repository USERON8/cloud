package com.cloud.common.exception;

public class EntityNotFoundException extends ResourceNotFoundException {

  public EntityNotFoundException(String entityName, Object id) {
    super(String.format("%s not found, id: %s", entityName, id));
  }

  public EntityNotFoundException(String entityName, String condition) {
    super(String.format("%s not found, %s", entityName, condition));
  }

  public EntityNotFoundException(String message) {
    super(message);
  }

  public EntityNotFoundException(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }
}
