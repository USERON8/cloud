package com.cloud.order.enums;

import lombok.Getter;

/**
 * 退款状态枚举
 *
 * @author CloudDevAgent
 * @since 2025-01-15
 */
@Getter
public enum RefundStatusEnum {

    PENDING_AUDIT(0, "待审核", "用户提交退款申请,等待商家审核"),
    AUDIT_PASSED(1, "审核通过", "商家审核通过,等待用户退货或直接退款"),
    AUDIT_REJECTED(2, "审核拒绝", "商家拒绝退款申请"),
    RETURNING(3, "退货中", "用户已发货,商品退货中"),
    GOODS_RECEIVED(4, "已收货", "商家已收到退货,等待退款"),
    REFUNDING(5, "退款中", "退款处理中"),
    COMPLETED(6, "已完成", "退款已完成"),
    CANCELLED(7, "已取消", "用户取消退款申请"),
    CLOSED(8, "已关闭", "退款流程异常关闭");

    private final Integer code;
    private final String name;
    private final String description;

    RefundStatusEnum(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static RefundStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RefundStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 是否为终态
     */
    public boolean isFinalStatus() {
        return this == AUDIT_REJECTED || this == COMPLETED || this == CANCELLED || this == CLOSED;
    }

    /**
     * 是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING_AUDIT || this == AUDIT_PASSED;
    }
}
