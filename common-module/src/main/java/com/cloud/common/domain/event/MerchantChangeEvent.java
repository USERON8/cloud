package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家变更事件对象
 * 用于在服务间传递商家变更信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantChangeEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商家ID
     */
    private Long merchantId;

    /**
     * 商家名称
     */
    private String merchantName;

    /**
     * 商家邮箱
     */
    private String email;

    /**
     * 商家电话
     */
    private String phone;

    /**
     * 用户类型
     */
    private String userType;

    /**
     * 认证状态：0-待审核，1-审核通过，2-审核拒绝
     */
    private Integer authStatus;

    /**
     * 变更前状态
     */
    private Integer beforeStatus;

    /**
     * 变更后状态
     */
    private Integer afterStatus;

    /**
     * 变更类型：1-创建商家，2-更新商家，3-删除商家
     */
    private Integer changeType;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 跟踪ID，用于幂等性处理
     */
    private String traceId;
}