package com.cloud.common.threadpool;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;








@Data
public class ThreadPoolHealthStatus {

    


    private LocalDateTime checkTime = LocalDateTime.now();

    


    private String overallStatus = "HEALTHY";

    


    private int totalPools = 0;

    


    private int healthyPools = 0;

    


    private int warningPools = 0;

    


    private int criticalPools = 0;

    


    private List<HealthIssue> warnings = new ArrayList<>();

    


    private List<HealthIssue> criticals = new ArrayList<>();

    


    public void addWarning(String threadPoolName, String message) {
        warnings.add(new HealthIssue(threadPoolName, message, "WARNING"));
    }

    


    public void addCritical(String threadPoolName, String message) {
        criticals.add(new HealthIssue(threadPoolName, message, "CRITICAL"));
    }

    


    public double getHealthRate() {
        if (totalPools == 0) {
            return 100.0;
        }
        return (double) healthyPools / totalPools * 100;
    }

    


    public boolean isOverallHealthy() {
        return "HEALTHY".equals(overallStatus);
    }

    


    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    


    public boolean hasCriticals() {
        return !criticals.isEmpty();
    }

    


    public int getTotalIssues() {
        return warnings.size() + criticals.size();
    }

    


    @Data
    public static class HealthIssue {
        


        private String threadPoolName;

        


        private String message;

        


        private String level;

        


        private LocalDateTime discoveredTime = LocalDateTime.now();

        public HealthIssue(String threadPoolName, String message, String level) {
            this.threadPoolName = threadPoolName;
            this.message = message;
            this.level = level;
        }
    }
}
