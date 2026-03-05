package com.cloud.common.domain.dto.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class SpuCreateRequestDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Valid
    @NotNull
    private SpuDTO spu;

    @Valid
    @NotEmpty
    private List<SkuDTO> skus;
}
