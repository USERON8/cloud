package com.cloud.common.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResilientIdentifierGenerator implements IdentifierGenerator {

  private static final long EPOCH = 1704067200000L;
  private static final long WORKER_ID_BITS = 5L;
  private static final long DATACENTER_ID_BITS = 5L;
  private static final long SEQUENCE_BITS = 12L;
  private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
  private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
  private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
  private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
  private static final long TIMESTAMP_LEFT_SHIFT =
      SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
  private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
  private static final long MAX_ROLLBACK_WAIT_MS = 500L;

  private final long workerId;
  private final long datacenterId;
  private final LongSupplier clock;

  private long sequence;
  private long lastTimestamp = -1L;

  public ResilientIdentifierGenerator(
      Long workerId, Long datacenterId, boolean allowRandomFallback) {
    this(resolveNodeIds(workerId, datacenterId, allowRandomFallback), System::currentTimeMillis);
  }

  ResilientIdentifierGenerator(long workerId, long datacenterId, LongSupplier clock) {
    if (workerId < 0 || workerId > MAX_WORKER_ID) {
      throw new IllegalArgumentException("workerId out of range: " + workerId);
    }
    if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
      throw new IllegalArgumentException("datacenterId out of range: " + datacenterId);
    }
    this.workerId = workerId;
    this.datacenterId = datacenterId;
    this.clock = clock;
  }

  private ResilientIdentifierGenerator(long[] nodeIds, LongSupplier clock) {
    this(nodeIds[0], nodeIds[1], clock);
  }

  @Override
  public synchronized Long nextId(Object entity) {
    long timestamp = clock.getAsLong();

    if (timestamp < lastTimestamp) {
      long rollbackMs = lastTimestamp - timestamp;
      if (rollbackMs <= MAX_ROLLBACK_WAIT_MS) {
        sleepQuietly(rollbackMs);
        timestamp = clock.getAsLong();
      }
      if (timestamp < lastTimestamp) {
        log.warn(
            "Clock moved backwards by {} ms, continuing with logical timestamp {}",
            rollbackMs,
            lastTimestamp);
        timestamp = lastTimestamp;
      }
    }

    if (timestamp == lastTimestamp) {
      sequence = (sequence + 1) & SEQUENCE_MASK;
      if (sequence == 0L) {
        timestamp = lastTimestamp + 1L;
      }
    } else {
      sequence = 0L;
    }

    lastTimestamp = timestamp;
    return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
        | (datacenterId << DATACENTER_ID_SHIFT)
        | (workerId << WORKER_ID_SHIFT)
        | sequence;
  }

  private static long resolveDatacenterId(boolean allowRandomFallback) {
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces != null && interfaces.hasMoreElements()) {
        NetworkInterface networkInterface = interfaces.nextElement();
        if (networkInterface.isLoopback()
            || networkInterface.isVirtual()
            || !networkInterface.isUp()) {
          continue;
        }
        byte[] mac = networkInterface.getHardwareAddress();
        if (mac == null || mac.length == 0) {
          continue;
        }
        long value = 0L;
        for (byte b : mac) {
          value = (value << 5) - value + (b & 0xFF);
        }
        return Math.floorMod(value, MAX_DATACENTER_ID + 1);
      }
    } catch (Exception exception) {
      log.warn("Failed to resolve datacenter id from network interface", exception);
    }
    if (!allowRandomFallback) {
      throw new IllegalStateException(
          "Unable to resolve datacenterId automatically and random fallback is disabled");
    }
    return new SecureRandom().nextInt((int) MAX_DATACENTER_ID + 1);
  }

  private static long resolveWorkerId(long datacenterId) {
    String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
    String seed = datacenterId + ":" + runtimeName;
    int hash = seed.getBytes(StandardCharsets.UTF_8).length == 0 ? 0 : seed.hashCode();
    return Math.floorMod(hash, (int) MAX_WORKER_ID + 1);
  }

  private static long[] resolveNodeIds(boolean allowRandomFallback) {
    long datacenterId = resolveDatacenterId(allowRandomFallback);
    long workerId = resolveWorkerId(datacenterId);
    return new long[] {workerId, datacenterId};
  }

  private static long[] resolveNodeIds(
      Long workerId, Long datacenterId, boolean allowRandomFallback) {
    if (workerId != null || datacenterId != null) {
      if (workerId == null || datacenterId == null) {
        throw new IllegalArgumentException("workerId and datacenterId must be configured together");
      }
      return new long[] {workerId, datacenterId};
    }
    return resolveNodeIds(allowRandomFallback);
  }

  private static void sleepQuietly(long rollbackMs) {
    try {
      TimeUnit.MILLISECONDS.sleep(rollbackMs);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
    }
  }
}
