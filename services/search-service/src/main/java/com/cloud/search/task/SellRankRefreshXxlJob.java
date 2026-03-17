package com.cloud.search.task;

import com.cloud.api.order.OrderDubboApi;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.order.ProductSellStatDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.RemoteException;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

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
      failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL)
  public void refreshSellRank() {
    int safeLimit = limit <= 0 ? 200 : limit;
    int safeTtlDays = ttlDays <= 0 ? 2 : ttlDays;
    List<ProductSellStatDTO> stats =
        invokeOrderService(
            "stat sell count today", () -> orderDubboApi.statSellCountToday(safeLimit));
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
      redisTemplate
          .opsForZSet()
          .add(
              DAILY_RANK_KEY,
              String.valueOf(stat.getProductId()),
              stat.getSellCount().doubleValue());
    }
    redisTemplate.expire(DAILY_RANK_KEY, safeTtlDays, TimeUnit.DAYS);
    String message = "sellRankRefreshJob finished, size=" + stats.size();
    XxlJobHelper.log(message);
    log.info(message);
  }

  private <T> T invokeOrderService(String action, Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE, "order-service unavailable when " + action, ex);
    }
  }
}
