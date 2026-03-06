package com.cloud.common.domain.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Product category DTO")
public class CategoryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Category ID")
    private Long id;

    @Schema(description = "Parent category ID")
    private Long parentId;

    @Schema(description = "Category name")
    private String name;

    @Schema(description = "Category description")
    private String description;

    @Schema(description = "Category icon URL")
    private String iconUrl;

    @Schema(description = "Category image URL")
    private String imageUrl;

    @Schema(description = "Sort order")
    private Integer sortOrder;

    @Schema(description = "Category level")
    private Integer level;

    @Schema(description = "Category path")
    private String path;

    @Schema(description = "Status: 0-disabled, 1-enabled")
    private Integer status;

    @Schema(description = "Visibility: 0-hidden, 1-visible")
    private Integer isVisible;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Update time")
    private LocalDateTime updatedAt;

    @Schema(description = "Soft delete flag")
    private Boolean deleted;

    @Schema(description = "Child categories")
    private List<CategoryDTO> children;
}
