package com.cloud.common.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;








@Data
@EqualsAndHashCode(callSuper = false)

public abstract class BaseEntity<T extends BaseEntity<T>> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    


    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    


    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    


    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    


    @TableLogic
    private Integer deleted = 0;

    


    @com.baomidou.mybatisplus.annotation.Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    private Integer version;

}
