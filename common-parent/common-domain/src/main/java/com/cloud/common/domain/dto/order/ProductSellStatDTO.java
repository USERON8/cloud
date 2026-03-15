package com.cloud.common.domain.dto.order;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ProductSellStatDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long productId;
    private Long sellCount;
}
