package com.cloud.common.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_WELCOME = "USER_WELCOME";
    public static final String TYPE_PASSWORD_RESET = "USER_PASSWORD_RESET";
    public static final String TYPE_ACTIVATION = "USER_ACTIVATION";
    public static final String TYPE_STATUS_CHANGE = "USER_STATUS_CHANGE";
    public static final String TYPE_BATCH = "USER_BATCH";
    public static final String TYPE_SYSTEM = "USER_SYSTEM";

    private String eventId;

    private String eventType;

    private Long userId;

    private List<Long> userIds;

    private String title;

    private String content;

    private String token;

    private Integer newStatus;

    private String reason;

    private Long timestamp;
}
