package com.cloud.user.exception;

/**
 * 文件上传异常
 * 当用户上传文件过程中发生错误时抛出此异常
 */
public class FileUploadException extends UserServiceException {
    
    public FileUploadException(String message) {
        super(50001, "文件上传失败: " + message);
    }
    
    public FileUploadException(String message, Throwable cause) {
        super(50001, "文件上传失败: " + message);
        this.initCause(cause);
    }
}