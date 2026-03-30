package com.cloud.stock.service.support;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.stock.mapper.StockSegmentMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StockRedisCacheService {

  private static final int ACTIVE_STATUS = 1;
  private static final String KEY_PREFIX = "stock:summary:";
  private static final String FIELD_SKU_ID = "skuId";
  private static final String FIELD_AVAILABLE = "available";
  private static final String FIELD_LOCKED = "locked";
  private static final String FIELD_SOLD = "sold";
  private static final String FIELD_SEGMENT_COUNT = "segmentCount";
  private static final String FIELD_ALERT = "alert";
  private static final String FIELD_STATUS = "status";
  private static final String FIELD_CREATED = "createdAt";
  private static final String FIELD_UPDATED = "updatedAt";
  private static final Long CACHE_MISS = -1L;
  private static final Long CACHE_FAIL = 0L;
  private static final Long CACHE_OK = 1L;

  @Value("${stock.cache.ledger.l1-max-size:2000}")
  private long l1MaxSize;

  @Value("${stock.cache.ledger.l1-ttl-seconds:3}")
  private long l1TtlSeconds;

  private final StringRedisTemplate stringRedisTemplate;
  private final StockSegmentMapper stockSegmentMapper;
  private final DefaultRedisScript<Long> preCheckScript;
  private Cache<Long, StockLedgerVO> localLedgerCache;

  public StockRedisCacheService(
      StringRedisTemplate stringRedisTemplate, StockSegmentMapper stockSegmentMapper) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.stockSegmentMapper = stockSegmentMapper;
    this.preCheckScript =
        buildScript(
            """
                if redis.call('EXISTS', KEYS[1]) == 0 then
                    return -1
                end
                local qty = tonumber(ARGV[1])
                local available = tonumber(redis.call('HGET', KEYS[1], 'available') or '-1')
                if available < qty then
                    return 0
                end
                return 1
                """);
    this.localLedgerCache =
        Caffeine.newBuilder().maximumSize(2000L).expireAfterWrite(Duration.ofSeconds(3L)).build();
  }

  @PostConstruct
  public void init() {
    this.localLedgerCache =
        Caffeine.newBuilder()
            .maximumSize(Math.max(100L, l1MaxSize))
            .expireAfterWrite(Duration.ofSeconds(Math.max(1L, l1TtlSeconds)))
            .build();
  }

  public StockLedgerVO getOrLoadLedger(Long skuId) {
    StockLedgerVO local = getLocalLedger(skuId);
    if (local != null) {
      return local;
    }
    StockLedgerVO cached = getLedgerFromCache(skuId);
    if (cached != null) {
      return cached;
    }
    StockLedgerVO ledger = stockSegmentMapper.selectLedgerBySkuId(skuId);
    if (ledger == null) {
      return null;
    }
    cacheLedger(ledger);
    return copyVo(ledger);
  }

  public StockLedgerVO getLedgerFromCache(Long skuId) {
    if (skuId == null) {
      return null;
    }
    StockLedgerVO local = getLocalLedger(skuId);
    if (local != null) {
      return local;
    }
    Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(buildKey(skuId));
    if (entries == null || entries.isEmpty()) {
      return null;
    }
    StockLedgerVO vo = new StockLedgerVO();
    vo.setSkuId(parseLong(entries.getOrDefault(FIELD_SKU_ID, skuId)));
    vo.setAvailableQty(parseInteger(entries.get(FIELD_AVAILABLE)));
    vo.setLockedQty(parseInteger(entries.get(FIELD_LOCKED)));
    vo.setSoldQty(parseInteger(entries.get(FIELD_SOLD)));
    vo.setSegmentCount(parseInteger(entries.get(FIELD_SEGMENT_COUNT)));
    vo.setAlertThreshold(parseInteger(entries.get(FIELD_ALERT)));
    vo.setStatus(parseInteger(entries.get(FIELD_STATUS)));
    vo.setCreatedAt(parseDateTime(entries.get(FIELD_CREATED)));
    vo.setUpdatedAt(parseDateTime(entries.get(FIELD_UPDATED)));
    if (!isActive(vo.getStatus())) {
      localLedgerCache.invalidate(skuId);
      stringRedisTemplate.delete(buildKey(skuId));
      return null;
    }
    putLocalLedger(vo);
    return copyVo(vo);
  }

  public void cacheLedger(StockLedgerVO ledger) {
    if (ledger == null || ledger.getSkuId() == null) {
      return;
    }
    if (!isActive(ledger.getStatus())) {
      localLedgerCache.invalidate(ledger.getSkuId());
      stringRedisTemplate.delete(buildKey(ledger.getSkuId()));
      return;
    }
    putLocalLedger(ledger);
    Map<String, String> map = new HashMap<>();
    map.put(FIELD_SKU_ID, stringify(ledger.getSkuId()));
    map.put(FIELD_AVAILABLE, stringify(ledger.getAvailableQty()));
    map.put(FIELD_LOCKED, stringify(ledger.getLockedQty()));
    map.put(FIELD_SOLD, stringify(ledger.getSoldQty()));
    map.put(FIELD_SEGMENT_COUNT, stringify(ledger.getSegmentCount()));
    map.put(FIELD_ALERT, stringify(ledger.getAlertThreshold()));
    map.put(FIELD_STATUS, stringify(ledger.getStatus()));
    map.put(FIELD_CREATED, stringify(ledger.getCreatedAt()));
    map.put(FIELD_UPDATED, stringify(ledger.getUpdatedAt()));
    stringRedisTemplate.opsForHash().putAll(buildKey(ledger.getSkuId()), map);
  }

  public void refreshFromDb(Long skuId) {
    if (skuId == null) {
      return;
    }
    StockLedgerVO ledger = stockSegmentMapper.selectLedgerBySkuId(skuId);
    if (ledger == null) {
      localLedgerCache.invalidate(skuId);
      stringRedisTemplate.delete(buildKey(skuId));
      return;
    }
    cacheLedger(ledger);
  }

  public boolean preCheck(List<StockOperateCommandDTO> commands) {
    if (commands == null || commands.isEmpty()) {
      return true;
    }
    for (StockOperateCommandDTO command : commands) {
      if (!preCheck(command)) {
        return false;
      }
    }
    return true;
  }

  public boolean preCheck(StockOperateCommandDTO command) {
    if (command == null || command.getSkuId() == null || command.getQuantity() == null) {
      return false;
    }
    Long result =
        stringRedisTemplate.execute(
            preCheckScript,
            List.of(buildKey(command.getSkuId())),
            String.valueOf(command.getQuantity()));
    if (Objects.equals(result, CACHE_OK)) {
      return true;
    }
    if (Objects.equals(result, CACHE_FAIL)) {
      return false;
    }
    refreshFromDb(command.getSkuId());
    result =
        stringRedisTemplate.execute(
            preCheckScript,
            List.of(buildKey(command.getSkuId())),
            String.valueOf(command.getQuantity()));
    return Objects.equals(result, CACHE_OK);
  }

  public void applyReserve(Long skuId, Integer qty) {
    mutateLocalLedger(
        skuId,
        ledger -> {
          ledger.setAvailableQty(ledger.getAvailableQty() - qty);
          ledger.setLockedQty(ledger.getLockedQty() + qty);
        });
  }

  public void applyRelease(Long skuId, Integer qty) {
    mutateLocalLedger(
        skuId,
        ledger -> {
          ledger.setLockedQty(ledger.getLockedQty() - qty);
          ledger.setAvailableQty(ledger.getAvailableQty() + qty);
        });
  }

  public void applyConfirmFromLocked(Long skuId, Integer qty) {
    mutateLocalLedger(
        skuId,
        ledger -> {
          ledger.setLockedQty(ledger.getLockedQty() - qty);
          ledger.setSoldQty(ledger.getSoldQty() + qty);
        });
  }

  public void applyDirectSell(Long skuId, Integer qty) {
    mutateLocalLedger(
        skuId,
        ledger -> {
          ledger.setAvailableQty(ledger.getAvailableQty() - qty);
          ledger.setSoldQty(ledger.getSoldQty() + qty);
        });
  }

  public void applyRollbackFromSold(Long skuId, Integer qty) {
    mutateLocalLedger(
        skuId,
        ledger -> {
          ledger.setSoldQty(ledger.getSoldQty() - qty);
          ledger.setAvailableQty(ledger.getAvailableQty() + qty);
        });
  }

  private DefaultRedisScript<Long> buildScript(String scriptText) {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setResultType(Long.class);
    script.setScriptText(scriptText);
    return script;
  }

  private boolean isActive(Integer status) {
    return status != null && status == ACTIVE_STATUS;
  }

  private String buildKey(Long skuId) {
    return KEY_PREFIX + skuId;
  }

  private StockLedgerVO getLocalLedger(Long skuId) {
    if (skuId == null) {
      return null;
    }
    StockLedgerVO cached = localLedgerCache.getIfPresent(skuId);
    if (cached == null || !isActive(cached.getStatus())) {
      return null;
    }
    return copyVo(cached);
  }

  private void putLocalLedger(StockLedgerVO ledger) {
    localLedgerCache.put(ledger.getSkuId(), copyVo(ledger));
  }

  private void mutateLocalLedger(Long skuId, Consumer<StockLedgerVO> mutator) {
    if (skuId == null) {
      return;
    }
    StockLedgerVO cached = localLedgerCache.getIfPresent(skuId);
    if (cached == null) {
      refreshFromDb(skuId);
      cached = localLedgerCache.getIfPresent(skuId);
      if (cached == null) {
        return;
      }
    }
    mutator.accept(cached);
    cached.setUpdatedAt(LocalDateTime.now());
    cacheLedger(cached);
  }

  private String stringify(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private Long parseLong(Object value) {
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    return Long.parseLong(String.valueOf(value));
  }

  private Integer parseInteger(Object value) {
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    return Integer.parseInt(String.valueOf(value));
  }

  private LocalDateTime parseDateTime(Object value) {
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    return LocalDateTime.parse(String.valueOf(value));
  }

  private StockLedgerVO copyVo(StockLedgerVO source) {
    StockLedgerVO copy = new StockLedgerVO();
    copy.setSkuId(source.getSkuId());
    copy.setAvailableQty(source.getAvailableQty());
    copy.setLockedQty(source.getLockedQty());
    copy.setSoldQty(source.getSoldQty());
    copy.setSegmentCount(source.getSegmentCount());
    copy.setAlertThreshold(source.getAlertThreshold());
    copy.setStatus(source.getStatus());
    copy.setCreatedAt(source.getCreatedAt());
    copy.setUpdatedAt(source.getUpdatedAt());
    return copy;
  }
}
