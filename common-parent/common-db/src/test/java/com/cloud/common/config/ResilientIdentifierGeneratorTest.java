package com.cloud.common.config;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResilientIdentifierGeneratorTest {

    @Test
    void shouldRemainMonotonicWhenClockMovesBackwards() {
        long[] timestamps = {1_000L, 1_005L, 1_004L, 1_006L};
        AtomicInteger index = new AtomicInteger();
        ResilientIdentifierGenerator generator = new ResilientIdentifierGenerator(1L, 1L,
                () -> timestamps[Math.min(index.getAndIncrement(), timestamps.length - 1)]);

        long first = generator.nextId(null);
        long second = generator.nextId(null);
        long third = generator.nextId(null);
        long fourth = generator.nextId(null);

        assertTrue(second > first);
        assertTrue(third > second);
        assertTrue(fourth > third);
    }

    @Test
    void shouldKeepIdsUniqueWhenClockStaysOnSameMillisecond() {
        ResilientIdentifierGenerator generator = new ResilientIdentifierGenerator(1L, 1L, () -> 2_000L);
        Set<Long> ids = new HashSet<>();

        for (int i = 0; i < 5_000; i++) {
            ids.add(generator.nextId(null));
        }

        assertEquals(5_000, ids.size());
    }
}
