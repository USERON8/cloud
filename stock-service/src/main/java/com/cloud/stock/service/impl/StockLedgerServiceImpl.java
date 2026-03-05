package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.stock.mapper.StockLedgerMapper;
import com.cloud.stock.mapper.StockReservationMapper;
import com.cloud.stock.mapper.StockTxnMapper;
import com.cloud.stock.module.entity.StockLedger;
import com.cloud.stock.module.entity.StockReservation;
import com.cloud.stock.module.entity.StockTxn;
import com.cloud.stock.service.StockLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockLedgerServiceImpl implements StockLedgerService {

    private final StockLedgerMapper stockLedgerMapper;
    private final StockReservationMapper stockReservationMapper;
    private final StockTxnMapper stockTxnMapper;

    @Override
    public StockLedgerVO getLedgerBySkuId(Long skuId) {
        StockLedger ledger = stockLedgerMapper.selectOne(new LambdaQueryWrapper<StockLedger>()
                .eq(StockLedger::getSkuId, skuId)
                .eq(StockLedger::getDeleted, 0)
                .last("LIMIT 1"));
        return ledger == null ? null : toVO(ledger);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reserve(StockOperateCommandDTO command) {
        StockReservation reservation = getReservation(command);
        if (reservation != null) {
            return true;
        }

        int affected = stockLedgerMapper.reserve(command.getSkuId(), command.getQuantity());
        if (affected <= 0) {
            throw new BusinessException("insufficient salable stock");
        }

        StockLedger after = requireLedger(command.getSkuId());
        StockReservation entity = new StockReservation();
        entity.setSubOrderNo(command.getSubOrderNo());
        entity.setSkuId(command.getSkuId());
        entity.setReservedQty(command.getQuantity());
        entity.setStatus("RESERVED");
        entity.setIdempotencyKey(buildIdempotencyKey(command));
        stockReservationMapper.insert(entity);

        writeTxn(command, "RESERVE", after, command.getReason());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean confirm(StockOperateCommandDTO command) {
        StockReservation reservation = requireReservation(command);
        if ("CONFIRMED".equals(reservation.getStatus())) {
            return true;
        }
        if (!"RESERVED".equals(reservation.getStatus())) {
            throw new BusinessException("reservation status invalid for confirm: " + reservation.getStatus());
        }

        int affectedReserved = stockLedgerMapper.confirm(command.getSkuId(), command.getQuantity());
        int affectedOnHand = stockLedgerMapper.deductOnHand(command.getSkuId(), command.getQuantity());
        if (affectedReserved <= 0 || affectedOnHand <= 0) {
            throw new BusinessException("confirm stock failed");
        }

        reservation.setStatus("CONFIRMED");
        stockReservationMapper.updateById(reservation);

        StockLedger after = requireLedger(command.getSkuId());
        writeTxn(command, "CONFIRM", after, command.getReason());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean release(StockOperateCommandDTO command) {
        StockReservation reservation = requireReservation(command);
        if ("RELEASED".equals(reservation.getStatus())) {
            return true;
        }
        if (!"RESERVED".equals(reservation.getStatus())) {
            throw new BusinessException("reservation status invalid for release: " + reservation.getStatus());
        }

        int affected = stockLedgerMapper.release(command.getSkuId(), command.getQuantity());
        if (affected <= 0) {
            throw new BusinessException("release stock failed");
        }

        reservation.setStatus("RELEASED");
        stockReservationMapper.updateById(reservation);

        StockLedger after = requireLedger(command.getSkuId());
        writeTxn(command, "RELEASE", after, command.getReason());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean rollback(StockOperateCommandDTO command) {
        StockReservation reservation = requireReservation(command);
        if ("ROLLED_BACK".equals(reservation.getStatus())) {
            return true;
        }

        if ("RESERVED".equals(reservation.getStatus())) {
            int affected = stockLedgerMapper.release(command.getSkuId(), command.getQuantity());
            if (affected <= 0) {
                throw new BusinessException("rollback release failed");
            }
        } else if ("CONFIRMED".equals(reservation.getStatus())) {
            int affected = stockLedgerMapper.rollbackAfterConfirm(command.getSkuId(), command.getQuantity());
            if (affected <= 0) {
                throw new BusinessException("rollback confirm failed");
            }
        } else {
            return true;
        }

        reservation.setStatus("ROLLED_BACK");
        stockReservationMapper.updateById(reservation);

        StockLedger after = requireLedger(command.getSkuId());
        writeTxn(command, "ROLLBACK", after, command.getReason());
        return true;
    }

    private StockReservation getReservation(StockOperateCommandDTO command) {
        return stockReservationMapper.selectOne(new LambdaQueryWrapper<StockReservation>()
                .eq(StockReservation::getSubOrderNo, command.getSubOrderNo())
                .eq(StockReservation::getSkuId, command.getSkuId())
                .eq(StockReservation::getDeleted, 0)
                .last("LIMIT 1"));
    }

    private StockReservation requireReservation(StockOperateCommandDTO command) {
        StockReservation reservation = getReservation(command);
        if (reservation == null) {
            throw new BusinessException("stock reservation not found");
        }
        return reservation;
    }

    private StockLedger requireLedger(Long skuId) {
        StockLedger ledger = stockLedgerMapper.selectOne(new LambdaQueryWrapper<StockLedger>()
                .eq(StockLedger::getSkuId, skuId)
                .eq(StockLedger::getDeleted, 0)
                .last("LIMIT 1"));
        if (ledger == null) {
            throw new BusinessException("stock ledger not found for skuId=" + skuId);
        }
        return ledger;
    }

    private String buildIdempotencyKey(StockOperateCommandDTO command) {
        return command.getSubOrderNo() + ":" + command.getSkuId();
    }

    private void writeTxn(StockOperateCommandDTO command, String txnType, StockLedger after, String reason) {
        StockTxn txn = new StockTxn();
        txn.setSkuId(command.getSkuId());
        txn.setSubOrderNo(command.getSubOrderNo());
        txn.setTxnType(txnType);
        txn.setQuantity(command.getQuantity());
        txn.setAfterOnHand(after.getOnHandQty());
        txn.setAfterReserved(after.getReservedQty());
        txn.setAfterSalable(after.getSalableQty());
        txn.setRemark(reason);
        stockTxnMapper.insert(txn);
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
}
