package com.cloud.search.task;

import com.cloud.api.order.OrderDubboApi;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.order.ProductSellStatDTO;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SellRankRefreshXxlJob {

    private static final String DAILY_RANK_KEY = "rank:sell:daily";

    private final StringRedisTemplate redisTemplate;

    @Value("${search.sell-rank.limit:200}")
    private int limit;

    @Value("${search.sell-rank.ttl-days:2}")
    private int ttlDays;

    @DubboReference(check = false, timeout = 5000, retries = 0)
    private OrderDubboApi orderDubboApi;

    @XxlJob("sellRankRefreshJob")
    @DistributedLock(
            key = "'xxl:search:sell-rank-refresh'",
            waitTime = 1,
            leaseTime = 300,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void refreshSellRank() {
        int safeLimit = limit <= 0 ? 200 : limit;
        int safeTtlDays = ttlDays <= 0 ? 2 : ttlDays;
        List<ProductSellStatDTO> stats = orderDubboApi.statSellCountToday(safeLimit);
        if (stats == null || stats.isEmpty()) {
            redisTemplate.delete(DAILY_RANK_KEY);
            XxlJobHelper.log("sellRankRefreshJob finished, empty stats");
            return;
        }

        redisTemplate.delete(DAILY_RANK_KEY);
        for (ProductSellStatDTO stat : stats) {
            if (stat == null || stat.getProductId() == null || stat.getSellCount() == null) {
                continue;
            }
            redisTemplate.opsForZSet()
                    .add(DAILY_RANK_KEY, String.valueOf(stat.getProductId()), stat.getSellCount().doubleValue());
        }
        redisTemplate.expire(DAILY_RANK_KEY, safeTtlDays, TimeUnit.DAYS);
        String message = "sellRankRefreshJob finished, size=" + stats.size();
        XxlJobHelper.log(message);
        log.info(message);
    }
}
