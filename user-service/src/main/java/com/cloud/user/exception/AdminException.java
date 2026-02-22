package com.cloud.user.exception;

import com.cloud.common.exception.BusinessException;
import lombok.Getter;








@Getter
public class AdminException extends BusinessException {

    public AdminException(int code, String message) {
        super(code, message);
    }

    public AdminException(String message) {
        super(message);
    }

    public AdminException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    


    public static class AdminNotFoundException extends AdminException {
        public AdminNotFoundException(Long adminId) {
            super(404, "绠＄悊鍛樹笉瀛樺湪: " + adminId);
        }

        public AdminNotFoundException(String username) {
            super(404, "绠＄悊鍛樹笉瀛樺湪: " + username);
        }
    }

    


    public static class AdminAlreadyExistsException extends AdminException {
        public AdminAlreadyExistsException(String username) {
            super(409, "绠＄悊鍛樺凡瀛樺湪: " + username);
        }
    }

    


    public static class AdminStatusException extends AdminException {
        public AdminStatusException(String message) {
            super(400, message);
        }

        public AdminStatusException(Long adminId, String status) {
            super(400, "绠＄悊鍛樼姸鎬佸紓甯? ID: " + adminId + ", 鐘舵€? " + status);
        }
    }

    


    public static class AdminPermissionException extends AdminException {
        public AdminPermissionException(String message) {
            super(403, message);
        }
    }

    


    public static class AdminPasswordException extends AdminException {
        public AdminPasswordException(String message) {
            super(400, message);
        }
    }

    


    public static class AdminCreateFailedException extends AdminException {
        public AdminCreateFailedException(String message) {
            super(500, "绠＄悊鍛樺垱寤哄け璐? " + message);
        }
    }

    


    public static class AdminUpdateFailedException extends AdminException {
        public AdminUpdateFailedException(String message) {
            super(500, "绠＄悊鍛樻洿鏂板け璐? " + message);
        }
    }

    


    public static class AdminDeleteFailedException extends AdminException {
        public AdminDeleteFailedException(String message) {
            super(500, "绠＄悊鍛樺垹闄ゅけ璐? " + message);
        }
    }

    


    public static class AdminStatusErrorException extends AdminException {
        public AdminStatusErrorException(String message) {
            super(400, "绠＄悊鍛樼姸鎬侀敊璇? " + message);
        }
    }

    


    public static class AdminQueryFailedException extends AdminException {
        public AdminQueryFailedException(String message) {
            super(500, "绠＄悊鍛樻煡璇㈠け璐? " + message);
        }
    }
}
