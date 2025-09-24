package com.cloud.common.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 * 包含所有实体的通用字段：主键、创建时间、更新时间、逻辑删除标识
 *
 * @author 代码规范团队
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)

public abstract class BaseEntity<T extends BaseEntity<T>> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID - 使用雪花算法自动生成
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间 - 插入时自动填充
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间 - 插入和更新时自动填充
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    /**
     * 逻辑删除标识 - 逻辑删除时自动填充
     */
    @TableLogic
    private Integer deleted = 0;

    /**
     * 版本号 - 乐观锁字段（可选，子类可以重写是否启用）
     */
    @com.baomidou.mybatisplus.annotation.Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    private Integer version;

}
