package com.cloud.stock.service.support;

import com.cloud.stock.mapper.StockLedgerMapper;
import com.cloud.stock.module.entity.StockLedger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockRedisCacheServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private StockLedgerMapper stockLedgerMapper;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private StockRedisCacheService stockRedisCacheService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void getLedgerFromCache_parsesFields() {
        Map<Object, Object> entries = Map.of(
                "id", "1",
                "skuId", "2",
                "salable", "5",
                "reserved", "1",
                "onhand", "6"
        );
        when(hashOperations.entries("stock:ledger:2")).thenReturn(entries);

        var result = stockRedisCacheService.getLedgerFromCache(2L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSkuId()).isEqualTo(2L);
        assertThat(result.getSalableQty()).isEqualTo(5);
    }

    @Test
    void applyReserveIfCached_returnsOk() {
        when(stringRedisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);

        var result = stockRedisCacheService.applyReserveIfCached(10L, 2);

        assertThat(result).isEqualTo(StockRedisCacheService.CacheResult.OK);
    }

    @Test
    void getOrLoadLedger_cacheMissLoadsDb() {
        when(hashOperations.entries("stock:ledger:5")).thenReturn(Map.of());
        StockLedger ledger = new StockLedger();
        ledger.setId(3L);
        ledger.setSkuId(5L);
        ledger.setSalableQty(10);
        when(stockLedgerMapper.selectActiveBySkuId(5L)).thenReturn(ledger);

        var result = stockRedisCacheService.getOrLoadLedger(5L);

        assertThat(result).isNotNull();
        verify(hashOperations).putAll(anyString(), any(Map.class));
    }
}
