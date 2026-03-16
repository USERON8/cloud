package com.cloud.common.exception;
















public class InvalidStatusException extends BusinessException {








    public InvalidStatusException(String entityName, String currentStatus, String operation) {
        super(String.format("%s褰撳墠鐘舵€佷负[%s]锛屾棤娉曟墽琛孾%s]鎿嶄綔", entityName, currentStatus, operation));
    }






    public InvalidStatusException(String message) {
        super(message);
    }







    public InvalidStatusException(int code, String message) {
        super(code, message);
    }







    public InvalidStatusException(String message, Throwable cause) {
        super(message, cause);
    }


    public static InvalidStatusException order(String currentStatus, String operation) {
        return new InvalidStatusException("璁㈠崟", currentStatus, operation);
    }

    public static InvalidStatusException payment(String currentStatus, String operation) {
        return new InvalidStatusException("鏀粯璁板綍", currentStatus, operation);
    }

    public static InvalidStatusException product(String currentStatus, String operation) {
        return new InvalidStatusException("鍟嗗搧", currentStatus, operation);
    }

    public static InvalidStatusException stock(String currentStatus, String operation) {
        return new InvalidStatusException("搴撳瓨", currentStatus, operation);
    }

    public static InvalidStatusException user(String currentStatus, String operation) {
        return new InvalidStatusException("鐢ㄦ埛", currentStatus, operation);
    }
}
