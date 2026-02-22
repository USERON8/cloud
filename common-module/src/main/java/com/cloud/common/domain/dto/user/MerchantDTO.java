package com.cloud.common.domain.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;




@Data
public class MerchantDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    


    private Long id;
    


    private String username;
    


    private String merchantName;
    


    private String email;
    


    private String phone;
    


    private String userType;
    


    private Integer status;
    


    private Integer authStatus;
    


    private LocalDateTime createdAt;
    


    private LocalDateTime updatedAt;
    


    private Integer deleted;
}
