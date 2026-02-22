package com.cloud.common.domain.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;






@Data
public class MerchantSettlementAccountDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    


    private Long id;
    


    private Long merchantId;
    


    private String accountName;
    


    private String accountNumber;
    


    private Integer accountType;
    


    private String bankName;
    


    private Integer isDefault;
    


    private Integer status;
    


    private LocalDateTime createdAt;
    


    private LocalDateTime updatedAt;
}
