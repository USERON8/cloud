package com.cloud.user.service;

import com.cloud.user.module.enums.UserActivityType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;







public interface UserActivityLogService {

    







    CompletableFuture<Boolean> logLoginActivityAsync(Long userId, String ip, String device);

    





    CompletableFuture<Boolean> logLogoutActivityAsync(Long userId);

    






    CompletableFuture<Boolean> logRegistrationActivityAsync(Long userId, String registrationType);

    






    CompletableFuture<Boolean> logProfileUpdateActivityAsync(Long userId, List<String> modifiedFields);

    





    CompletableFuture<Boolean> logPasswordChangeActivityAsync(Long userId);

    








    CompletableFuture<Boolean> logActivityAsync(Long userId, UserActivityType activityType,
                                                String description, Map<String, Object> metadata);

    






    CompletableFuture<List<Map<String, Object>>> getRecentActivitiesAsync(Long userId, Integer limit);

    






    CompletableFuture<Long> calculateUserActivityScoreAsync(Long userId, Integer days);

    





    CompletableFuture<Boolean> logBatchActivitiesAsync(List<Map<String, Object>> activities);
}
