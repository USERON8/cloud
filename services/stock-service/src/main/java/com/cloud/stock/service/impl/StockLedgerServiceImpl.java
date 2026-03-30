package com.cloud.stock.service.impl;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.stock.mapper.StockReservationMapper;
import com.cloud.stock.mapper.StockSegmentMapper;
import com.cloud.stock.messaging.StockMessageProducer;
import com.cloud.stock.module.entity.StockReservation;
import com.cloud.stock.module.entity.StockSegment;
import com.cloud.stock.module.entity.StockTxn;
import com.cloud.stock.service.StockLedgerService;
import com.cloud.stock.service.support.StockRedisCacheService;
import com.cloud.stock.service.support.StockSearchSyncService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockLedgerServiceImpl implements StockLedgerService {

  private static final String STATUS_LOCKED = "LOCKED";
  private static final String STATUS_SOLD = "SOLD";
  private static final String STATUS_RELEASED = "RELEASED";

  private final StockSegmentMapper stockSegmentMapper;
  private final StockReservationMapper stockReservationMapper;
  private final StockTxnAsyncWriter stockTxnAsyncWriter;
  private final StockMessageProducer stockMessageProducer;
  private final TradeMetrics tradeMetrics;
  private final StockRedisCacheService stockRedisCacheService;
  private final StockSearchSyncService stockSearchSyncService;

  @Override
  public StockLedgerVO getLedgerBySkuId(Long skuId) {
    return stockRedisCacheService.getOrLoadLedger(skuId);
  }

  @Override
  public List<StockLedgerVO> listLedgersBySkuIds(List<Long> skuIds) {
    if (skuIds == null || skuIds.isEmpty()) {
      return List.of();
    }
    List<Long> safeSkuIds =
        skuIds.stream()
            .filter(id -> id != null)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .toList();
    if (safeSkuIds.isEmpty()) {
      return List.of();
    }
    return stockSegmentMapper.listLedgersBySkuIds(safeSkuIds);
  }

  @Override
  public Boolean preCheck(List<StockOperateCommandDTO> commands) {
    return stockRedisCacheService.preCheck(commands);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean reserve(StockOperateCommandDTO command) {
    validateCommand(command);
    try {
      if (!Boolean.TRUE.equals(preCheck(List.of(command)))) {
        throw new BusinessException("insufficient available stock");
      }
      List<StockReservation> existing = listReservations(command);
      if (isReserveCompleted(existing, command)) {
        tradeMetrics.incrementStockFreeze("success");
        return true;
      }
      if (!existing.isEmpty()) {
        throw new BusinessException(
            "reservation already exists for subOrderNo=" + command.getSubOrderNo());
      }

      List<SegmentAllocation> allocations = allocate(command);
      for (SegmentAllocation allocation : allocations) {
        StockReservation reservation = new StockReservation();
        reservation.setMainOrderNo(command.getOrderNo());
        reservation.setSubOrderNo(command.getSubOrderNo());
        reservation.setSkuId(command.getSkuId());
        reservation.setSegmentId(allocation.segmentId());
        reservation.setQuantity(allocation.quantity());
        reservation.setStatus(STATUS_LOCKED);
        reservation.setIdempotencyKey(buildIdempotencyKey(command, allocation.segmentId()));
        stockReservationMapper.insert(reservation);
        writeTxn(command, allocation, "RESERVE", command.getReason());
      }
      stockRedisCacheService.evictLedgerAfterCommit(command.getSkuId());
      stockSearchSyncService.syncProductsBySkuIds(List.of(command.getSkuId()));
      tradeMetrics.incrementStockFreeze("success");
      return true;
    } catch (Exception ex) {
      tradeMetrics.incrementStockFreeze("failed");
      handleReserveFailure(command, ex);
      throw ex;
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean confirm(StockOperateCommandDTO command) {
    validateCommand(command);
    List<StockReservation> reservations = requireReservations(command);
    if (allMatchStatus(reservations, STATUS_SOLD)) {
      return true;
    }
    for (StockReservation reservation : reservations) {
      if (STATUS_SOLD.equals(reservation.getStatus())) {
        continue;
      }
      if (!STATUS_LOCKED.equals(reservation.getStatus())) {
        throw new BusinessException(
            "reservation status invalid for confirm: " + reservation.getStatus());
      }
      int updated =
          stockSegmentMapper.confirmLockedOnSegment(
              reservation.getSkuId(), reservation.getSegmentId(), reservation.getQuantity());
      if (updated != 1) {
        throw new BusinessException("confirm stock failed");
      }
      reservation.setStatus(STATUS_SOLD);
      stockReservationMapper.updateById(reservation);
      writeTxn(
          command,
          reservation.getSegmentId(),
          reservation.getQuantity(),
          "CONFIRM",
          command.getReason());
    }
    stockRedisCacheService.evictLedgerAfterCommit(command.getSkuId());
    stockSearchSyncService.syncProductsBySkuIds(List.of(command.getSkuId()));
    return true;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean release(StockOperateCommandDTO command) {
    validateCommand(command);
    List<StockReservation> reservations = requireReservations(command);
    if (allMatchStatus(reservations, STATUS_RELEASED)) {
      return true;
    }
    int releasedQty = 0;
    for (StockReservation reservation : reservations) {
      if (STATUS_RELEASED.equals(reservation.getStatus())) {
        continue;
      }
      if (!STATUS_LOCKED.equals(reservation.getStatus())) {
        throw new BusinessException(
            "reservation status invalid for release: " + reservation.getStatus());
      }
      int updated =
          stockSegmentMapper.releaseOnSegment(
              reservation.getSkuId(), reservation.getSegmentId(), reservation.getQuantity());
      if (updated != 1) {
        throw new BusinessException("release stock failed");
      }
      reservation.setStatus(STATUS_RELEASED);
      stockReservationMapper.updateById(reservation);
      releasedQty += reservation.getQuantity();
      writeTxn(
          command,
          reservation.getSegmentId(),
          reservation.getQuantity(),
          "RELEASE",
          command.getReason());
    }
    if (releasedQty > 0) {
      stockRedisCacheService.evictLedgerAfterCommit(command.getSkuId());
      stockSearchSyncService.syncProductsBySkuIds(List.of(command.getSkuId()));
    }
    return true;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean rollback(StockOperateCommandDTO command) {
    if (command == null) {
      return true;
    }
    return rollbackBatch(List.of(command));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean rollbackBatch(List<StockOperateCommandDTO> commands) {
    if (commands == null || commands.isEmpty()) {
      return true;
    }
    for (StockOperateCommandDTO command : commands) {
      if (command == null || command.getQuantity() == null || command.getQuantity() <= 0) {
        continue;
      }
      validateCommand(command);
      restoreReservation(command);
    }
    return true;
  }

  private void restoreReservation(StockOperateCommandDTO command) {
    List<StockReservation> reservations = requireReservations(command);
    int remaining = command.getQuantity();
    int restoredFromLocked = 0;
    int restoredFromSold = 0;
    for (StockReservation reservation : reservations) {
      if (remaining <= 0) {
        break;
      }
      if (STATUS_RELEASED.equals(reservation.getStatus())) {
        continue;
      }
      int movable = Math.min(remaining, defaultZero(reservation.getQuantity()));
      if (movable <= 0) {
        continue;
      }
      if (STATUS_LOCKED.equals(reservation.getStatus())) {
        int updated =
            stockSegmentMapper.releaseOnSegment(
                reservation.getSkuId(), reservation.getSegmentId(), movable);
        if (updated != 1) {
          throw new BusinessException("rollback locked stock failed");
        }
        restoredFromLocked += movable;
      } else if (STATUS_SOLD.equals(reservation.getStatus())) {
        int updated =
            stockSegmentMapper.restoreSoldOnSegment(
                reservation.getSkuId(), reservation.getSegmentId(), movable);
        if (updated != 1) {
          throw new BusinessException("rollback sold stock failed");
        }
        restoredFromSold += movable;
      } else {
        throw new BusinessException(
            "reservation status invalid for rollback: " + reservation.getStatus());
      }
      remaining -= movable;
      reservation.setQuantity(reservation.getQuantity() - movable);
      if (defaultZero(reservation.getQuantity()) == 0) {
        reservation.setStatus(STATUS_RELEASED);
      }
      stockReservationMapper.updateById(reservation);
      writeTxn(command, reservation.getSegmentId(), movable, "ROLLBACK", command.getReason());
    }
    if (remaining > 0) {
      throw new BusinessException(
          "rollback quantity exceeds reserved quantity for subOrderNo=" + command.getSubOrderNo());
    }
    if (restoredFromLocked > 0 || restoredFromSold > 0) {
      stockRedisCacheService.evictLedgerAfterCommit(command.getSkuId());
    }
    if (restoredFromLocked > 0 || restoredFromSold > 0) {
      stockSearchSyncService.syncProductsBySkuIds(List.of(command.getSkuId()));
    }
  }

  private List<SegmentAllocation> allocate(StockOperateCommandDTO command) {
    List<StockSegment> segments = stockSegmentMapper.listActiveSegmentsBySkuId(command.getSkuId());
    if (segments == null || segments.isEmpty()) {
      throw new BusinessException("stock segment not found for skuId=" + command.getSkuId());
    }
    List<StockSegment> orderedSegments = reorderSegments(segments, command.getSubOrderNo());
    int remaining = command.getQuantity();
    List<SegmentAllocation> allocations = new ArrayList<>();
    for (StockSegment segment : orderedSegments) {
      int available = defaultZero(segment.getAvailableQty());
      if (available <= 0) {
        continue;
      }
      int take = Math.min(remaining, available);
      if (take <= 0) {
        continue;
      }
      int updated =
          stockSegmentMapper.reserveOnSegment(command.getSkuId(), segment.getSegmentId(), take);
      if (updated != 1) {
        continue;
      }
      allocations.add(new SegmentAllocation(segment.getSegmentId(), take));
      remaining -= take;
      if (remaining == 0) {
        break;
      }
    }
    if (remaining > 0) {
      throw new BusinessException("insufficient available stock");
    }
    return allocations;
  }

  private List<StockSegment> reorderSegments(List<StockSegment> segments, String subOrderNo) {
    if (segments == null || segments.isEmpty()) {
      return List.of();
    }
    int size = segments.size();
    int startIndex = Math.floorMod(subOrderNo == null ? 0 : subOrderNo.hashCode(), size);
    List<StockSegment> ordered = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      ordered.add(segments.get((startIndex + i) % size));
    }
    ordered.sort(Comparator.comparing(StockSegment::getSegmentId));
    if (size > 1) {
      List<StockSegment> rotated = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        rotated.add(ordered.get((startIndex + i) % size));
      }
      return rotated;
    }
    return ordered;
  }

  private List<StockReservation> listReservations(StockOperateCommandDTO command) {
    List<StockReservation> reservations =
        stockReservationMapper.listActiveBySubOrderNoAndSkuId(
            command.getSubOrderNo(), command.getSkuId());
    return reservations == null ? List.of() : reservations;
  }

  private List<StockReservation> requireReservations(StockOperateCommandDTO command) {
    List<StockReservation> reservations = listReservations(command);
    if (reservations.isEmpty()) {
      throw new BusinessException("stock reservation not found");
    }
    int totalQuantity =
        reservations.stream()
            .filter(item -> !STATUS_RELEASED.equals(item.getStatus()))
            .mapToInt(item -> defaultZero(item.getQuantity()))
            .sum();
    if (totalQuantity < command.getQuantity()) {
      throw new BusinessException(
          "reservation quantity mismatch for subOrderNo=" + command.getSubOrderNo());
    }
    return reservations;
  }

  private boolean isReserveCompleted(
      List<StockReservation> reservations, StockOperateCommandDTO command) {
    if (reservations == null || reservations.isEmpty()) {
      return false;
    }
    int totalQuantity =
        reservations.stream().mapToInt(item -> defaultZero(item.getQuantity())).sum();
    if (totalQuantity != command.getQuantity()) {
      return false;
    }
    return reservations.stream()
        .allMatch(
            item -> STATUS_LOCKED.equals(item.getStatus()) || STATUS_SOLD.equals(item.getStatus()));
  }

  private boolean allMatchStatus(List<StockReservation> reservations, String status) {
    return reservations.stream().allMatch(item -> status.equals(item.getStatus()));
  }

  private String buildIdempotencyKey(StockOperateCommandDTO command, Integer segmentId) {
    return command.getSubOrderNo() + ":" + command.getSkuId() + ":" + segmentId;
  }

  private void validateCommand(StockOperateCommandDTO command) {
    if (command == null || command.getSkuId() == null || command.getQuantity() == null) {
      throw new BusinessException("stock command is invalid");
    }
    if (command.getQuantity() <= 0) {
      throw new BusinessException("stock quantity must be greater than 0");
    }
    if (command.getSubOrderNo() == null || command.getSubOrderNo().isBlank()) {
      throw new BusinessException("subOrderNo is required");
    }
  }

  private void handleReserveFailure(StockOperateCommandDTO command, Exception ex) {
    if (!isInsufficientStock(ex) || command == null || command.getOrderNo() == null) {
      return;
    }
    stockMessageProducer.sendStockFreezeFailedEvent(command.getOrderNo(), ex.getMessage());
  }

  private boolean isInsufficientStock(Exception ex) {
    return ex != null
        && ex.getMessage() != null
        && ex.getMessage().toLowerCase().contains("insufficient available stock");
  }

  private int defaultZero(Integer value) {
    return value == null ? 0 : value;
  }

  private void writeTxn(
      StockOperateCommandDTO command, SegmentAllocation allocation, String txnType, String reason) {
    writeTxn(command, allocation.segmentId(), allocation.quantity(), txnType, reason);
  }

  private void writeTxn(
      StockOperateCommandDTO command,
      Integer segmentId,
      Integer quantity,
      String txnType,
      String reason) {
    StockTxn txn = new StockTxn();
    txn.setSkuId(command.getSkuId());
    txn.setSegmentId(segmentId);
    txn.setSubOrderNo(command.getSubOrderNo());
    txn.setTxnType(txnType);
    txn.setQuantity(quantity);
    txn.setRemark(reason);
    stockTxnAsyncWriter.write(txn);
  }

  private record SegmentAllocation(Integer segmentId, Integer quantity) {}
}
