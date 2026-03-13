package com.cloud.order.service;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

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
