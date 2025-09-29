package com.cloud.common.domain.vo.user;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商家VO类
 * 
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Data
public class MerchantVO {
    
    /**
     * 商家ID
     */
    private Long id;
    
    /**
     * 商家名称
     */
    private String merchantName;
    
    /**
     * 商家状态
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
