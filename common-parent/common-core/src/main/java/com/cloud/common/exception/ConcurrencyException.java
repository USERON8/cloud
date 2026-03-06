package com.cloud.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;





@Data
@EqualsAndHashCode(callSuper = true)
public class ConcurrencyException extends BusinessException {
    private String resourceType;
    private String resourceId;

    public ConcurrencyException(String message) {
        super(message);
    }

    public ConcurrencyException(int code, String message) {
        super(code, message);
    }

    public ConcurrencyException(String resourceType, String resourceId) {
        super("Concurrent modification detected for resource: " + resourceType + " with id " + resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public ConcurrencyException(int code, String resourceType, String resourceId) {
        super(code, "Concurrent modification detected for resource: " + resourceType + " with id " + resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
}
