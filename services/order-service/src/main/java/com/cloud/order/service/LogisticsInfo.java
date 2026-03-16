package com.cloud.order.service;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogisticsInfo {

  private String status;
  private String currentLocation;
  private LocalDate estimatedArrival;
  private List<Track> tracks;

  @Data
  @Builder
  public static class Track {
    private String message;
    private String timestamp;
  }
}
