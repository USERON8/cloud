package com.cloud.order.service.impl;

import com.cloud.order.service.LogisticsInfo;
import com.cloud.order.service.LogisticsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Primary
@RequiredArgsConstructor
public class MockLogisticsProvider implements LogisticsProvider {

    @Override
    public LogisticsInfo query(String trackingNumber) {
        return LogisticsInfo.builder()
                .status("运输中")
                .currentLocation("北京分拣中心")
                .estimatedArrival(LocalDate.now().plusDays(2))
                .tracks(List.of(
                        LogisticsInfo.Track.builder()
                                .message("已揽收")
                                .timestamp("2024-01-01 10:00")
                                .build(),
                        LogisticsInfo.Track.builder()
                                .message("北京转运中心")
                                .timestamp("2024-01-01 18:00")
                                .build(),
                        LogisticsInfo.Track.builder()
                                .message("运输中")
                                .timestamp("2024-01-02 09:00")
                                .build()
                ))
                .build();
    }
}
