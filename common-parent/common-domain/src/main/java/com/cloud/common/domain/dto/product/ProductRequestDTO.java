package com.cloud.common.domain.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;






@Data
public class ProductRequestDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    


    @NotNull(message = "搴楅摵ID涓嶈兘涓虹┖")
    private Long shopId;

    


    @NotBlank(message = "鍟嗗搧鍚嶇О涓嶈兘涓虹┖")
    private String name;

    


    @NotNull(message = "鍟嗗搧浠锋牸涓嶈兘涓虹┖")
    @DecimalMin(value = "0.01", message = "鍟嗗搧浠锋牸蹇呴』澶т簬0")
    private BigDecimal price;

    


    @NotNull(message = "搴撳瓨鏁伴噺涓嶈兘涓虹┖")
    private Integer stockQuantity;

    


    @NotNull(message = "鍒嗙被ID涓嶈兘涓虹┖")
    private Long categoryId;

    


    private Long brandId;

    


    private Integer status = 1;

    


    private String description;

    


    private String imageUrl;
}
