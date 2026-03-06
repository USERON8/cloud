package com.cloud.user.module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserNotificationBatchRequestDTO {

    @NotEmpty(message = "user ids are required")
    private List<@NotNull(message = "user id cannot be null") Long> userIds;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "content is required")
    private String content;
}
