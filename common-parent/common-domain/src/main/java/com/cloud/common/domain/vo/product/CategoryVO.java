package com.cloud.common.domain.vo.product;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;




@Data
public class CategoryVO {
    


    private Long id;

    


    private Long parentId;

    


    private String name;

    


    private Integer level;

    


    private Integer sortOrder;

    


    private Integer status;

    


    private LocalDateTime createdAt;

    


    private LocalDateTime updatedAt;

    


    private List<CategoryVO> children;
}
