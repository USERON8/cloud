package com.cloud.auth.task;

import com.cloud.auth.service.SimpleRedisHashOAuth2AuthorizationService;
import com.cloud.auth.service.TokenBlacklistService;
import com.cloud.common.annotation.DistributedLock;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenCleanupXxlJob {

    private final SimpleRedisHashOAuth2AuthorizationService authorizationService;
    private final TokenBlacklistService tokenBlacklistService;

    @XxlJob("authTokenCleanupJob")
    @DistributedLock(
            key = "'xxl:auth:token-cleanup'",
            waitTime = 1,
            leaseTime = 300,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void cleanup() {
        try {
            authorizationService.cleanupExpiredTokens();
            int cleaned = tokenBlacklistService.cleanupExpiredEntries();
            String message = "authTokenCleanupJob finished, cleaned blacklist=" + cleaned;
            XxlJobHelper.log(message);
            log.info(message);
        } catch (Exception ex) {
            log.error("Auth token cleanup job failed", ex);
            XxlJobHelper.handleFail(ex.getMessage());
        }
    }
}
