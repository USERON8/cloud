package com.cloud.common.exception;

public class InsufficientException extends BizException {

  public InsufficientException(String resourceName, Object required, Object available) {
    super(String.format("%s涓嶈冻锛岄渶瑕? %s锛屽彲鐢? %s", resourceName, required, available));
  }

  public InsufficientException(String message) {
    super(message);
  }

  public InsufficientException(int code, String message) {
    super(code, message);
  }

  public InsufficientException(String message, Throwable cause) {
    super(message, cause);
  }

  public static InsufficientException stock(Long productId, Integer required, Integer available) {
    return new InsufficientException(
        String.format("鍟嗗搧[ID:%d]搴撳瓨涓嶈冻锛岄渶瑕? %d锛屽彲鐢? %d", productId, required, available));
  }

  public static InsufficientException balance(Long userId, Object required, Object available) {
    return new InsufficientException(
        String.format("鐢ㄦ埛[ID:%d]浣欓涓嶈冻锛岄渶瑕? %s锛屽彲鐢? %s", userId, required, available));
  }

  public static InsufficientException points(Long userId, Integer required, Integer available) {
    return new InsufficientException(
        String.format("鐢ㄦ埛[ID:%d]绉垎涓嶈冻锛岄渶瑕? %d锛屽彲鐢? %d", userId, required, available));
  }

  public static InsufficientException permission(String operation) {
    return new InsufficientException(String.format("鏉冮檺涓嶈冻锛屾棤娉曟墽琛屾搷浣? %s", operation));
  }
}
