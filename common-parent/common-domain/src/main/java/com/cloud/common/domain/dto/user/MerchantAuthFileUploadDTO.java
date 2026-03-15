package com.cloud.common.domain.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class MerchantAuthFileUploadDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String fileKey;

    private String previewUrl;
}
