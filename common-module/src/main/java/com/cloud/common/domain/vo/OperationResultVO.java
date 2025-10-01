package com.cloud.common.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用操作结果VO
 * 用于Feign接口返回操作结果，替代Boolean类型
 */
@Data
public class OperationResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 操作结果消息
     */
    private String message;

    /**
     * 默认构造函数
     */
    public OperationResultVO() {
    }

    /**
     * 构造函数
     *
     * @param success 是否成功
     * @param message 消息
     */
    public OperationResultVO(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    /**
     * 创建成功结果
     *
     * @return 操作结果VO
     */
    public static OperationResultVO success() {
        return new OperationResultVO(true, "操作成功");
    }

    /**
     * 创建成功结果
     *
     * @param message 消息
     * @return 操作结果VO
     */
    public static OperationResultVO success(String message) {
        return new OperationResultVO(true, message);
    }

    /**
     * 创建失败结果
     *
     * @return 操作结果VO
     */
    public static OperationResultVO failure() {
        return new OperationResultVO(false, "操作失败");
    }

    /**
     * 创建失败结果
     *
     * @param message 消息
     * @return 操作结果VO
     */
    public static OperationResultVO failure(String message) {
        return new OperationResultVO(false, message);
    }
}