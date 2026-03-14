package com.cloud.search.task;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.search.service.support.HotKeywordSyncService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotKeywordXxlJob {

    private final HotKeywordSyncService hotKeywordSyncService;

    @XxlJob("hotKeywordPersistJob")
    @DistributedLock(
            key = "'xxl:search:hot-keyword-persist'",
            waitTime = 1,
            leaseTime = 300,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void persistHotKeywords() {
        try {
            hotKeywordSyncService.syncToDb();
            logMessage("hotKeywordPersistJob finished");
        } catch (Exception ex) {
            log.error("Hot keyword persist job failed", ex);
            XxlJobHelper.handleFail(ex.getMessage());
        }
    }

    @XxlJob("hotKeywordWarmUpJob")
    @DistributedLock(
            key = "'xxl:search:hot-keyword-warmup'",
            waitTime = 1,
            leaseTime = 300,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void warmUpHotKeywords() {
        try {
            hotKeywordSyncService.restoreFromDbOnStartup();
            logMessage("hotKeywordWarmUpJob finished");
        } catch (Exception ex) {
            log.error("Hot keyword warmup job failed", ex);
            XxlJobHelper.handleFail(ex.getMessage());
        }
    }

    private void logMessage(String message) {
        XxlJobHelper.log(message);
        log.info(message);
    }
}
