package com.cloud.user.module.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserSystemAnnouncementRequestDTO {

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "content is required")
    private String content;
}
