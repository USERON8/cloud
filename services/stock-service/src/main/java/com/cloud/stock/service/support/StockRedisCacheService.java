package com.cloud.stock.service.support;

import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.stock.mapper.StockLedgerMapper;
import com.cloud.stock.module.entity.StockLedger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class StockRedisCacheService {

    private static final String KEY_PREFIX = "stock:ledger:";
    private static final String FIELD_ID = "id";
    private static final String FIELD_SKU_ID = "skuId";
    private static final String FIELD_ON_HAND = "onhand";
    private static final String FIELD_RESERVED = "reserved";
    private static final String FIELD_SALABLE = "salable";
    private static final String FIELD_ALERT = "alert";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_CREATED = "createdAt";
    private static final String FIELD_UPDATED = "updatedAt";

    private static final Long CACHE_MISS = -1L;
    private static final Long INSUFFICIENT = 0L;
    private static final Long CACHE_OK = 1L;

    private final StringRedisTemplate stringRedisTemplate;
    private final StockLedgerMapper stockLedgerMapper;

    private final DefaultRedisScript<Long> reserveScript;
    private final DefaultRedisScript<Long> releaseScript;
    private final DefaultRedisScript<Long> confirmScript;
    private final DefaultRedisScript<Long> rollbackConfirmScript;

    public StockRedisCacheService(StringRedisTemplate stringRedisTemplate,
                                  StockLedgerMapper stockLedgerMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.stockLedgerMapper = stockLedgerMapper;
        this.reserveScript = buildScript("""
                if redis.call('EXISTS', KEYS[1]) == 0 then
                    return -1
                end
                local qty = tonumber(ARGV[1])
                local salable = tonumber(redis.call('HGET', KEYS[1], 'salable') or '-1')
                if salable < qty then
                    return 0
                end
                redis.call('HINCRBY', KEYS[1], 'salable', -qty)
                redis.call('HINCRBY', KEYS[1], 'reserved', qty)
                return 1
                """);
        this.releaseScript = buildScript("""
                if redis.call('EXISTS', KEYS[1]) == 0 then
                    return -1
                end
                local qty = tonumber(ARGV[1])
                local reserved = tonumber(redis.call('HGET', KEYS[1], 'reserved') or '-1')
                if reserved < qty then
                    return 0
                end
                redis.call('HINCRBY', KEYS[1], 'reserved', -qty)
                redis.call('HINCRBY', KEYS[1], 'salable', qty)
                return 1
                """);
        this.confirmScript = buildScript("""
                if redis.call('EXISTS', KEYS[1]) == 0 then
                    return -1
                end
                local qty = tonumber(ARGV[1])
                local reserved = tonumber(redis.call('HGET', KEYS[1], 'reserved') or '-1')
                local onhand = tonumber(redis.call('HGET', KEYS[1], 'onhand') or '-1')
                if reserved < qty or onhand < qty then
                    return 0
                end
                redis.call('HINCRBY', KEYS[1], 'reserved', -qty)
                redis.call('HINCRBY', KEYS[1], 'onhand', -qty)
                return 1
                """);
        this.rollbackConfirmScript = buildScript("""
                if redis.call('EXISTS', KEYS[1]) == 0 then
                    return -1
                end
                local qty = tonumber(ARGV[1])
                redis.call('HINCRBY', KEYS[1], 'onhand', qty)
                redis.call('HINCRBY', KEYS[1], 'salable', qty)
                return 1
                """);
    }

    public StockLedgerVO getOrLoadLedger(Long skuId) {
        StockLedgerVO cached = getLedgerFromCache(skuId);
        if (cached != null) {
            return cached;
        }
        StockLedger ledger = stockLedgerMapper.selectActiveBySkuId(skuId);
        if (ledger == null) {
            return null;
        }
        cacheLedger(ledger);
        return toVO(ledger);
    }

    public StockLedgerVO getLedgerFromCache(Long skuId) {
        if (skuId == null) {
            return null;
        }
        String key = buildKey(skuId);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        StockLedgerVO vo = new StockLedgerVO();
        vo.setId(parseLong(entries.get(FIELD_ID)));
        vo.setSkuId(parseLong(entries.getOrDefault(FIELD_SKU_ID, skuId)));
        vo.setOnHandQty(parseInteger(entries.get(FIELD_ON_HAND)));
        vo.setReservedQty(parseInteger(entries.get(FIELD_RESERVED)));
        vo.setSalableQty(parseInteger(entries.get(FIELD_SALABLE)));
        vo.setAlertThreshold(parseInteger(entries.get(FIELD_ALERT)));
        vo.setStatus(parseInteger(entries.get(FIELD_STATUS)));
        vo.setCreatedAt(parseDateTime(entries.get(FIELD_CREATED)));
        vo.setUpdatedAt(parseDateTime(entries.get(FIELD_UPDATED)));
        return vo;
    }

    public void cacheLedger(StockLedger ledger) {
        if (ledger == null || ledger.getSkuId() == null) {
            return;
        }
        String key = buildKey(ledger.getSkuId());
        Map<String, String> map = new HashMap<>();
        map.put(FIELD_ID, stringify(ledger.getId()));
        map.put(FIELD_SKU_ID, stringify(ledger.getSkuId()));
        map.put(FIELD_ON_HAND, stringify(ledger.getOnHandQty()));
        map.put(FIELD_RESERVED, stringify(ledger.getReservedQty()));
        map.put(FIELD_SALABLE, stringify(ledger.getSalableQty()));
        map.put(FIELD_ALERT, stringify(ledger.getAlertThreshold()));
        map.put(FIELD_STATUS, stringify(ledger.getStatus()));
        map.put(FIELD_CREATED, stringify(ledger.getCreatedAt()));
        map.put(FIELD_UPDATED, stringify(ledger.getUpdatedAt()));
        try {
            stringRedisTemplate.opsForHash().putAll(key, map);
        } catch (Exception ex) {
            log.warn("Cache stock ledger failed: skuId={}", ledger.getSkuId(), ex);
        }
    }

    public void refreshFromDb(Long skuId) {
        if (skuId == null) {
            return;
        }
        StockLedger ledger = stockLedgerMapper.selectActiveBySkuId(skuId);
        if (ledger == null) {
            stringRedisTemplate.delete(buildKey(skuId));
            return;
        }
        cacheLedger(ledger);
    }

    public CacheResult applyReserveIfCached(Long skuId, Integer qty) {
        return execute(reserveScript, skuId, qty);
    }

    public CacheResult applyReleaseIfCached(Long skuId, Integer qty) {
        return execute(releaseScript, skuId, qty);
    }

    public CacheResult applyConfirmIfCached(Long skuId, Integer qty) {
        return execute(confirmScript, skuId, qty);
    }

    public CacheResult applyRollbackAfterConfirmIfCached(Long skuId, Integer qty) {
        return execute(rollbackConfirmScript, skuId, qty);
    }

    private CacheResult execute(DefaultRedisScript<Long> script, Long skuId, Integer qty) {
        if (skuId == null || qty == null || qty <= 0) {
            return CacheResult.MISS;
        }
        try {
            Long result = stringRedisTemplate.execute(script, java.util.List.of(buildKey(skuId)), String.valueOf(qty));
            if (Objects.equals(result, CACHE_OK)) {
                return CacheResult.OK;
            }
            if (Objects.equals(result, INSUFFICIENT)) {
                return CacheResult.INSUFFICIENT;
            }
            return CacheResult.MISS;
        } catch (Exception ex) {
            log.warn("Apply stock cache script failed: skuId={}, qty={}", skuId, qty, ex);
            return CacheResult.MISS;
        }
    }

    private DefaultRedisScript<Long> buildScript(String scriptText) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(scriptText);
        return script;
    }

    private String buildKey(Long skuId) {
        return KEY_PREFIX + skuId;
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        String raw = value.toString();
        if (raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        String raw = value.toString();
        if (raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        String raw = value.toString();
        if (raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private StockLedgerVO toVO(StockLedger ledger) {
        StockLedgerVO vo = new StockLedgerVO();
        vo.setId(ledger.getId());
        vo.setSkuId(ledger.getSkuId());
        vo.setOnHandQty(ledger.getOnHandQty());
        vo.setReservedQty(ledger.getReservedQty());
        vo.setSalableQty(ledger.getSalableQty());
        vo.setAlertThreshold(ledger.getAlertThreshold());
        vo.setStatus(ledger.getStatus());
        vo.setCreatedAt(ledger.getCreatedAt());
        vo.setUpdatedAt(ledger.getUpdatedAt());
        return vo;
    }

    public enum CacheResult {
        OK,
        INSUFFICIENT,
        MISS
    }
}
