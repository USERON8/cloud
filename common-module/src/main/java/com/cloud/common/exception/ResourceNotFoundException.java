package com.cloud.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;





@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceNotFoundException extends BusinessException {
    private String resourceType;
    private String resourceId;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(int code, String message) {
        super(code, message);
    }

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("Resource not found: " + resourceType + " with id " + resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(int code, String resourceType, String resourceId) {
        super(code, "Resource not found: " + resourceType + " with id " + resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
}
