package com.cloud.common.domain.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


@Data
public class AvatarUploadDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "头像文件不能为空")
    private MultipartFile avatar;
}
