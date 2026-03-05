package com.cloud.common.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;





@Data
public class OperationResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    


    private Boolean success;

    


    private String message;

    


    public OperationResultVO() {
    }

    





    public OperationResultVO(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    




    public static OperationResultVO success() {
        return new OperationResultVO(true, "鎿嶄綔鎴愬姛");
    }

    





    public static OperationResultVO success(String message) {
        return new OperationResultVO(true, message);
    }

    




    public static OperationResultVO failure() {
        return new OperationResultVO(false, "鎿嶄綔澶辫触");
    }

    





    public static OperationResultVO failure(String message) {
        return new OperationResultVO(false, message);
    }
}
