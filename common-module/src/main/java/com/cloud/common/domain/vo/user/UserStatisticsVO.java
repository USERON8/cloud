package com.cloud.common.domain.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 用户统计概览VO
 *
 * @author what's up
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户统计概览VO")
public class UserStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "总用户数")
    private Long totalUsers;

    @Schema(description = "今日新增用户数")
    private Long todayNewUsers;

    @Schema(description = "本月新增用户数")
    private Long monthNewUsers;

    @Schema(description = "活跃用户数（最近7天）")
    private Long activeUsers;

    @Schema(description = "用户类型分布 Map<类型, 数量>")
    private Map<String, Long> userTypeDistribution;

    @Schema(description = "用户状态分布 Map<状态, 数量>")
    private Map<String, Long> userStatusDistribution;

    @Schema(description = "用户增长率（%）")
    private Double growthRate;

    @Schema(description = "平均活跃度")
    private Double averageActivity;
}
