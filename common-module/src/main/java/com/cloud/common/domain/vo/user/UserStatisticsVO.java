package com.cloud.common.domain.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User statistics overview")
public class UserStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Total number of users")
    private Long totalUsers;

    @Schema(description = "New users today")
    private Long todayNewUsers;

    @Schema(description = "New users this month")
    private Long monthNewUsers;

    @Schema(description = "Active users in recent 7 days")
    private Long activeUsers;

    @Schema(description = "User type distribution Map<type, count>")
    private Map<String, Long> userTypeDistribution;

    @Schema(description = "User status distribution Map<status, count>")
    private Map<String, Long> userStatusDistribution;

    @Schema(description = "User growth rate in percent")
    private Double growthRate;

    @Schema(description = "Average activity score")
    private Double averageActivity;
}
