package com.cloud.stock.service.impl;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.stock.mapper.StockLedgerMapper;
import com.cloud.stock.mapper.StockReservationMapper;
import com.cloud.stock.messaging.StockMessageProducer;
import com.cloud.stock.module.entity.StockLedger;
import com.cloud.stock.module.entity.StockReservation;
import com.cloud.stock.module.entity.StockTxn;
import com.cloud.stock.service.StockLedgerService;
import com.cloud.stock.service.support.StockRedisCacheService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockLedgerServiceImpl implements StockLedgerService {

  private static final String STATUS_RESERVED = "RESERVED";
  private static final String STATUS_CONFIRMED = "CONFIRMED";
  private static final String STATUS_RELEASED = "RELEASED";
  private static final String STATUS_ROLLED_BACK = "ROLLED_BACK";
  private static final Set<String> RESERVE_COMPLETED_STATUSES = Set.of("RESERVED", "CONFIRMED");
  private static final Set<String> RESERVE_FORBIDDEN_STATUSES = Set.of("RELEASED", "ROLLED_BACK");

  private final StockLedgerMapper stockLedgerMapper;
  private final StockReservationMapper stockReservationMapper;
  private final StockTxnAsyncWriter stockTxnAsyncWriter;
  private final StockMessageProducer stockMessageProducer;
  private final TradeMetrics tradeMetrics;
  private final StockRedisCacheService stockRedisCacheService;

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
            .filter(skuId -> skuId != null)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .toList();
    if (safeSkuIds.isEmpty()) {
      return List.of();
    }
    return stockLedgerMapper.listActiveBySkuIds(safeSkuIds).stream()
        .filter(ledger -> ledger != null && Integer.valueOf(1).equals(ledger.getStatus()))
        .map(this::toVO)
        .toList();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean reserve(StockOperateCommandDTO command) {
    try {
      StockReservation reservation = insertReservationToken(command);
      if (reservation == null) {
        throw new BusinessException("stock reservation not found");
      }
      if (shouldSkipReserve(reservation, command)) {
        tradeMetrics.incrementStockFreeze("success");
        return true;
      }

      int affected = stockLedgerMapper.reserve(command.getSkuId(), command.getQuantity());
      if (affected <= 0) {
        throw new BusinessException("insufficient salable stock");
      }

      reservation.setStatus(STATUS_RESERVED);
      stockReservationMapper.updateById(reservation);
      StockRedisCacheService.CacheResult cacheResult =
          stockRedisCacheService.applyReserveIfCached(command.getSkuId(), command.getQuantity());
      if (cacheResult != StockRedisCacheService.CacheResult.OK) {
        stockRedisCacheService.refreshFromDb(command.getSkuId());
      }
      writeTxn(command, "RESERVE", command.getReason());
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
  public Boolean confirmReservation(StockOperateCommandDTO command) {
    if (command == null) {
      return true;
    }
    StockReservation reservation = getReservation(command);
    if (reservation == null) {
      return true;
    }
    ensureReservationQuantityMatches(reservation, command);
    String status = reservation.getStatus();
    if (STATUS_RESERVED.equals(status) || STATUS_CONFIRMED.equals(status)) {
      return true;
    }
    if (STATUS_ROLLED_BACK.equals(status) || STATUS_RELEASED.equals(status)) {
      return true;
    }
    if ("RESERVING".equals(status)) {
      reservation.setStatus(STATUS_RESERVED);
      stockReservationMapper.updateById(reservation);
      return true;
    }
    return true;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean confirm(StockOperateCommandDTO command) {
    StockReservation reservation = requireReservation(command);
    if (STATUS_CONFIRMED.equals(reservation.getStatus())) {
      return true;
    }
    if (!STATUS_RESERVED.equals(reservation.getStatus())) {
      throw new BusinessException(
          "reservation status invalid for confirm: " + reservation.getStatus());
    }

    int affectedReserved = stockLedgerMapper.confirm(command.getSkuId(), command.getQuantity());
    int affectedOnHand = stockLedgerMapper.deductOnHand(command.getSkuId(), command.getQuantity());
    if (affectedReserved <= 0 || affectedOnHand <= 0) {
      throw new BusinessException("confirm stock failed");
    }

    reservation.setStatus(STATUS_CONFIRMED);
    stockReservationMapper.updateById(reservation);

    StockLedger after = requireLedger(command.getSkuId());
    StockRedisCacheService.CacheResult cacheResult =
        stockRedisCacheService.applyConfirmIfCached(command.getSkuId(), command.getQuantity());
    if (cacheResult != StockRedisCacheService.CacheResult.OK) {
      stockRedisCacheService.cacheLedger(after);
    }
    writeTxn(command, "CONFIRM", after, command.getReason());
    return true;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean release(StockOperateCommandDTO command) {
    StockReservation reservation = requireReservation(command);
    if (STATUS_RELEASED.equals(reservation.getStatus())) {
      return true;
    }
    if (!STATUS_RESERVED.equals(reservation.getStatus())) {
      throw new BusinessException(
          "reservation status invalid for release: " + reservation.getStatus());
    }

    int affected = stockLedgerMapper.release(command.getSkuId(), command.getQuantity());
    if (affected <= 0) {
      throw new BusinessException("release stock failed");
    }

    reservation.setStatus(STATUS_RELEASED);
    stockReservationMapper.updateById(reservation);

    StockLedger after = requireLedger(command.getSkuId());
    StockRedisCacheService.CacheResult cacheResult =
        stockRedisCacheService.applyReleaseIfCached(command.getSkuId(), command.getQuantity());
    if (cacheResult != StockRedisCacheService.CacheResult.OK) {
      stockRedisCacheService.cacheLedger(after);
    }
    writeTxn(command, "RELEASE", after, command.getReason());
    return true;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean rollback(StockOperateCommandDTO command) {
    if (command == null) {
      return true;
    }
    rollbackBatch(List.of(command));
    return true;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean rollbackBatch(List<StockOperateCommandDTO> commands) {
    if (commands == null || commands.isEmpty()) {
      return true;
    }

    List<StockOperateCommandDTO> validCommands = new ArrayList<>();
    Set<String> subOrderNos = new HashSet<>();
    Set<Long> skuIds = new HashSet<>();
    for (StockOperateCommandDTO command : commands) {
      if (command == null || command.getSkuId() == null || command.getQuantity() == null) {
        continue;
      }
      if (command.getQuantity() <= 0) {
        continue;
      }
      validCommands.add(command);
      if (command.getSubOrderNo() != null) {
        subOrderNos.add(command.getSubOrderNo());
      }
      skuIds.add(command.getSkuId());
    }
    if (validCommands.isEmpty()) {
      return true;
    }

    Map<String, StockReservation> reservationMap = new HashMap<>();
    if (!subOrderNos.isEmpty() && !skuIds.isEmpty()) {
      List<StockReservation> reservations =
          stockReservationMapper.selectActiveBySubOrderNosAndSkuIds(
              new ArrayList<>(subOrderNos), new ArrayList<>(skuIds));
      for (StockReservation reservation : reservations) {
        if (reservation == null
            || reservation.getSubOrderNo() == null
            || reservation.getSkuId() == null) {
          continue;
        }
        reservationMap.put(
            buildReservationKey(reservation.getSubOrderNo(), reservation.getSkuId()), reservation);
      }
    }

    Map<String, StockOperateCommandDTO> normalizedByReservationKey = new LinkedHashMap<>();
    for (StockOperateCommandDTO command : validCommands) {
      if (command.getSubOrderNo() == null) {
        insertRollbackMarker(command);
        continue;
      }
      String reservationKey = buildReservationKey(command.getSubOrderNo(), command.getSkuId());
      StockOperateCommandDTO aggregated = normalizedByReservationKey.get(reservationKey);
      if (aggregated == null) {
        StockOperateCommandDTO copy = new StockOperateCommandDTO();
        copy.setOrderNo(command.getOrderNo());
        copy.setSubOrderNo(command.getSubOrderNo());
        copy.setSkuId(command.getSkuId());
        copy.setQuantity(command.getQuantity());
        copy.setReason(command.getReason());
        normalizedByReservationKey.put(reservationKey, copy);
      } else {
        aggregated.setQuantity(aggregated.getQuantity() + command.getQuantity());
      }
    }

    List<StockOperateCommandDTO> reservedCommands = new ArrayList<>();
    List<StockOperateCommandDTO> confirmedCommands = new ArrayList<>();
    List<ReservationRollbackUpdate> reservationUpdates = new ArrayList<>();

    for (Map.Entry<String, StockOperateCommandDTO> entry : normalizedByReservationKey.entrySet()) {
      StockOperateCommandDTO command = entry.getValue();
      StockReservation reservation = reservationMap.get(entry.getKey());
      if (reservation == null) {
        insertRollbackMarker(command);
        continue;
      }
      String previousStatus = reservation.getStatus();
      if (STATUS_ROLLED_BACK.equals(previousStatus)) {
        continue;
      }
      int rollbackQty = command.getQuantity();
      Integer reservedQty = reservation.getReservedQty();
      if (reservedQty == null || rollbackQty > reservedQty) {
        throw new BusinessException(
            "rollback quantity exceeds reserved quantity for subOrderNo="
                + command.getSubOrderNo());
      }
      String nextStatus = rollbackQty == reservedQty ? STATUS_ROLLED_BACK : previousStatus;
      if (STATUS_RESERVED.equals(previousStatus)) {
        reservedCommands.add(command);
        reservationUpdates.add(
            new ReservationRollbackUpdate(
                reservation.getId(), rollbackQty, previousStatus, nextStatus));
      } else if (STATUS_CONFIRMED.equals(previousStatus)) {
        confirmedCommands.add(command);
        reservationUpdates.add(
            new ReservationRollbackUpdate(
                reservation.getId(), rollbackQty, previousStatus, nextStatus));
      }
    }

    Map<Long, Integer> reservedQtyBySku = aggregateQtyBySku(reservedCommands);
    Map<Long, Integer> confirmedQtyBySku = aggregateQtyBySku(confirmedCommands);

    if (!reservedQtyBySku.isEmpty()) {
      List<StockOperateCommandDTO> aggregated = toAggregatedCommands(reservedQtyBySku);
      int updated = stockLedgerMapper.batchRelease(aggregated);
      if (updated != reservedQtyBySku.size()) {
        throw new BusinessException("batch rollback release failed");
      }
    }
    if (!confirmedQtyBySku.isEmpty()) {
      List<StockOperateCommandDTO> aggregated = toAggregatedCommands(confirmedQtyBySku);
      int updated = stockLedgerMapper.batchRollbackAfterConfirm(aggregated);
      if (updated != confirmedQtyBySku.size()) {
        throw new BusinessException("batch rollback confirm failed");
      }
    }

    for (ReservationRollbackUpdate update : reservationUpdates) {
      int adjusted =
          stockReservationMapper.adjustAfterRollback(
              update.id(), update.quantity(), update.currentStatus(), update.newStatus());
      if (adjusted != 1) {
        throw new BusinessException("rollback reservation status failed");
      }
    }

    Map<Long, StockLedger> afterLedgers = new HashMap<>();
    for (Long skuId : reservedQtyBySku.keySet()) {
      afterLedgers.put(skuId, requireLedger(skuId));
    }
    for (Long skuId : confirmedQtyBySku.keySet()) {
      afterLedgers.putIfAbsent(skuId, requireLedger(skuId));
    }

    for (StockOperateCommandDTO command : reservedCommands) {
      StockLedger after = afterLedgers.get(command.getSkuId());
      StockRedisCacheService.CacheResult cacheResult =
          stockRedisCacheService.applyReleaseIfCached(command.getSkuId(), command.getQuantity());
      if (cacheResult != StockRedisCacheService.CacheResult.OK && after != null) {
        stockRedisCacheService.cacheLedger(after);
      }
      writeTxn(command, "ROLLBACK", after, command.getReason());
    }
    for (StockOperateCommandDTO command : confirmedCommands) {
      StockLedger after = afterLedgers.get(command.getSkuId());
      StockRedisCacheService.CacheResult cacheResult =
          stockRedisCacheService.applyRollbackAfterConfirmIfCached(
              command.getSkuId(), command.getQuantity());
      if (cacheResult != StockRedisCacheService.CacheResult.OK && after != null) {
        stockRedisCacheService.cacheLedger(after);
      }
      writeTxn(command, "ROLLBACK", after, command.getReason());
    }

    return true;
  }

  private StockReservation insertReservationToken(StockOperateCommandDTO command) {
    StockReservation entity = new StockReservation();
    entity.setSubOrderNo(command.getSubOrderNo());
    entity.setSkuId(command.getSkuId());
    entity.setReservedQty(command.getQuantity());
    entity.setStatus("RESERVING");
    entity.setIdempotencyKey(buildIdempotencyKey(command));
    try {
      stockReservationMapper.insert(entity);
      return entity;
    } catch (DuplicateKeyException duplicateKeyException) {
      return getReservation(command);
    }
  }

  private StockReservation getReservation(StockOperateCommandDTO command) {
    return stockReservationMapper.selectActiveBySubOrderNoAndSkuId(
        command.getSubOrderNo(), command.getSkuId());
  }

  private StockReservation requireReservation(StockOperateCommandDTO command) {
    StockReservation reservation = getReservation(command);
    if (reservation == null) {
      throw new BusinessException("stock reservation not found");
    }
    ensureReservationQuantityMatches(reservation, command);
    return reservation;
  }

  private StockLedger requireLedger(Long skuId) {
    StockLedger ledger = stockLedgerMapper.selectActiveBySkuId(skuId);
    if (ledger == null) {
      throw new BusinessException("stock ledger not found for skuId=" + skuId);
    }
    return ledger;
  }

  private String buildIdempotencyKey(StockOperateCommandDTO command) {
    return command.getSubOrderNo() + ":" + command.getSkuId();
  }

  private boolean shouldSkipReserve(StockReservation reservation, StockOperateCommandDTO command) {
    ensureReservationQuantityMatches(reservation, command);
    String status = reservation.getStatus();
    if (RESERVE_COMPLETED_STATUSES.contains(status)) {
      return true;
    }
    if (RESERVE_FORBIDDEN_STATUSES.contains(status)) {
      throw new BusinessException(
          "reservation already rolled back for subOrderNo=" + command.getSubOrderNo());
    }
    if ("RESERVING".equals(status)) {
      return false;
    }
    throw new BusinessException("reservation status invalid for reserve: " + status);
  }

  private void handleReserveFailure(StockOperateCommandDTO command, Exception ex) {
    if (command == null) {
      return;
    }
    if (!isInsufficientStock(ex)) {
      return;
    }
    String orderNo = command.getOrderNo();
    if (orderNo == null || orderNo.isBlank()) {
      return;
    }
    stockMessageProducer.sendStockFreezeFailedEvent(orderNo, ex.getMessage());
  }

  private boolean isInsufficientStock(Exception ex) {
    if (ex == null || ex.getMessage() == null) {
      return false;
    }
    return ex.getMessage().toLowerCase().contains("insufficient salable stock");
  }

  private String buildReservationKey(String subOrderNo, Long skuId) {
    return subOrderNo + ":" + skuId;
  }

  private Map<Long, Integer> aggregateQtyBySku(List<StockOperateCommandDTO> commands) {
    Map<Long, Integer> aggregate = new HashMap<>();
    for (StockOperateCommandDTO command : commands) {
      if (command == null || command.getSkuId() == null || command.getQuantity() == null) {
        continue;
      }
      aggregate.merge(command.getSkuId(), command.getQuantity(), Integer::sum);
    }
    return aggregate;
  }

  private List<StockOperateCommandDTO> toAggregatedCommands(Map<Long, Integer> aggregated) {
    List<StockOperateCommandDTO> commands = new ArrayList<>();
    for (Map.Entry<Long, Integer> entry : aggregated.entrySet()) {
      StockOperateCommandDTO command = new StockOperateCommandDTO();
      command.setSkuId(entry.getKey());
      command.setQuantity(entry.getValue());
      command.setSubOrderNo("BATCH");
      commands.add(command);
    }
    return commands;
  }

  private void insertRollbackMarker(StockOperateCommandDTO command) {
    if (command == null || command.getSubOrderNo() == null || command.getSkuId() == null) {
      return;
    }
    StockReservation marker = new StockReservation();
    marker.setSubOrderNo(command.getSubOrderNo());
    marker.setSkuId(command.getSkuId());
    marker.setReservedQty(command.getQuantity() == null ? 0 : command.getQuantity());
    marker.setStatus(STATUS_ROLLED_BACK);
    marker.setIdempotencyKey(buildIdempotencyKey(command));
    try {
      stockReservationMapper.insert(marker);
    } catch (DuplicateKeyException ignored) {

    }
  }

  private void ensureReservationQuantityMatches(
      StockReservation reservation, StockOperateCommandDTO command) {
    if (reservation.getReservedQty() != null
        && !reservation.getReservedQty().equals(command.getQuantity())) {
      throw new BusinessException(
          "reservation quantity mismatch for subOrderNo=" + command.getSubOrderNo());
    }
  }

  private void writeTxn(StockOperateCommandDTO command, String txnType, String reason) {
    writeTxn(command, txnType, null, reason);
  }

  private void writeTxn(
      StockOperateCommandDTO command, String txnType, StockLedger after, String reason) {
    StockTxn txn = new StockTxn();
    txn.setSkuId(command.getSkuId());
    txn.setSubOrderNo(command.getSubOrderNo());
    txn.setTxnType(txnType);
    txn.setQuantity(command.getQuantity());
    if (after != null) {
      txn.setAfterOnHand(after.getOnHandQty());
      txn.setAfterReserved(after.getReservedQty());
      txn.setAfterSalable(after.getSalableQty());
    }
    txn.setRemark(reason);
    dispatchTxnWrite(txn);
  }

  private void dispatchTxnWrite(StockTxn txn) {
    stockTxnAsyncWriter.write(txn);
  }

  private record ReservationRollbackUpdate(
      Long id, Integer quantity, String currentStatus, String newStatus) {}

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
}
