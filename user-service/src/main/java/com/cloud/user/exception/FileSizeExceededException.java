package com.cloud.user.exception;

/**
 * 文件大小超出限制异常
 * 当用户上传的文件大小超出系统限制时抛出此异常
 */
public class FileSizeExceededException extends UserServiceException {
    
    public FileSizeExceededException(String message) {
        super(40001, "文件大小超出限制: " + message);
    }
}