package com.cloud.common.domain.vo.order;

import lombok.Data;

@Data
public class OrderSubStatusVO {
    private Long mainOrderId;
    private Long subOrderId;
    private String mainOrderNo;
    private String subOrderNo;
    private String orderStatus;
    private Long userId;
}
