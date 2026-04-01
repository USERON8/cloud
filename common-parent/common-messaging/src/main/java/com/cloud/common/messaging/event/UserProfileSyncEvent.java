package com.cloud.common.messaging.event;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileSyncEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String TYPE_UPSERT = "UPSERT";

  private String eventId;
  private String eventType;
  private Long userId;
  private String username;
  private String phone;
  private String nickname;
  private String email;
  private String avatarUrl;
  private Integer status;
}
