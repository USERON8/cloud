package com.cloud.stock.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.stock.v2.entity.StockLedgerV2;
import com.cloud.stock.v2.entity.StockReservationV2;
import com.cloud.stock.v2.entity.StockTxnV2;
import com.cloud.stock.v2.mapper.StockLedgerV2Mapper;
import com.cloud.stock.v2.mapper.StockReservationV2Mapper;
import com.cloud.stock.v2.mapper.StockTxnV2Mapper;
import com.cloud.stock.v2.service.StockV2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StockV2ServiceImpl implements StockV2Service {

    private final StockLedgerV2Mapper stockLedgerV2Mapper;
    private final StockReservationV2Mapper stockReservationV2Mapper;
    private final StockTxnV2Mapper stockTxnV2Mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockLedgerV2 createLedger(StockLedgerV2 ledger) {
        if (ledger.getOnHandQty() == null) {
            ledger.setOnHandQty(0);
        }
        if (ledger.getReservedQty() == null) {
            ledger.setReservedQty(0);
        }
        ledger.setSalableQty(ledger.getOnHandQty() - ledger.getReservedQty());
        if (ledger.getStockStatus() == null) {
            ledger.setStockStatus("NORMAL");
        }
        stockLedgerV2Mapper.insert(ledger);
        return ledger;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockReservationV2 reserve(String mainOrderNo, String subOrderNo, Long skuId, Integer qty) {
        StockLedgerV2 ledger = findBySkuId(skuId);
        if (ledger.getSalableQty() < qty) {
            throw new BusinessException("insufficient salable stock");
        }
        int beforeOnHand = ledger.getOnHandQty();
        int beforeReserved = ledger.getReservedQty();
        int beforeSalable = ledger.getSalableQty();

        ledger.setReservedQty(beforeReserved + qty);
        ledger.setSalableQty(beforeSalable - qty);
        stockLedgerV2Mapper.updateById(ledger);

        StockReservationV2 reservation = new StockReservationV2();
        reservation.setReservationNo("RSV-" + System.currentTimeMillis());
        reservation.setMainOrderNo(mainOrderNo);
        reservation.setSubOrderNo(subOrderNo);
        reservation.setSkuId(skuId);
        reservation.setReservedQty(qty);
        reservation.setReservationStatus("RESERVED");
        reservation.setExpireAt(LocalDateTime.now().plusMinutes(30));
        stockReservationV2Mapper.insert(reservation);

        saveTxn("RESERVE", skuId, mainOrderNo, subOrderNo, qty, beforeOnHand, beforeReserved, beforeSalable,
                ledger.getOnHandQty(), ledger.getReservedQty(), ledger.getSalableQty(), "reserve");
        return reservation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockLedgerV2 confirm(String subOrderNo, Long skuId, Integer qty) {
        StockLedgerV2 ledger = findBySkuId(skuId);
        int beforeOnHand = ledger.getOnHandQty();
        int beforeReserved = ledger.getReservedQty();
        int beforeSalable = ledger.getSalableQty();

        if (beforeReserved < qty) {
            throw new BusinessException("reserved qty not enough for confirm");
        }
        if (beforeOnHand < qty) {
            throw new BusinessException("on-hand qty not enough for confirm");
        }
        ledger.setOnHandQty(beforeOnHand - qty);
        ledger.setReservedQty(beforeReserved - qty);
        ledger.setSalableQty(ledger.getOnHandQty() - ledger.getReservedQty());
        stockLedgerV2Mapper.updateById(ledger);
        saveTxn("DEDUCT", skuId, null, subOrderNo, qty, beforeOnHand, beforeReserved, beforeSalable,
                ledger.getOnHandQty(), ledger.getReservedQty(), ledger.getSalableQty(), "confirm deduct");
        return ledger;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockLedgerV2 release(String subOrderNo, Long skuId, Integer qty, String reason) {
        StockLedgerV2 ledger = findBySkuId(skuId);
        int beforeOnHand = ledger.getOnHandQty();
        int beforeReserved = ledger.getReservedQty();
        int beforeSalable = ledger.getSalableQty();
        if (beforeReserved < qty) {
            throw new BusinessException("reserved qty not enough for release");
        }
        ledger.setReservedQty(beforeReserved - qty);
        ledger.setSalableQty(beforeSalable + qty);
        stockLedgerV2Mapper.updateById(ledger);
        saveTxn("RELEASE", skuId, null, subOrderNo, qty, beforeOnHand, beforeReserved, beforeSalable,
                ledger.getOnHandQty(), ledger.getReservedQty(), ledger.getSalableQty(), reason);
        return ledger;
    }

    private StockLedgerV2 findBySkuId(Long skuId) {
        StockLedgerV2 ledger = stockLedgerV2Mapper.selectOne(
                new LambdaQueryWrapper<StockLedgerV2>()
                        .eq(StockLedgerV2::getSkuId, skuId)
                        .eq(StockLedgerV2::getDeleted, 0)
        );
        if (ledger == null) {
            throw new BusinessException("stock ledger not found for skuId=" + skuId);
        }
        return ledger;
    }

    private void saveTxn(String txnType, Long skuId, String mainOrderNo, String subOrderNo, Integer qty,
                         Integer beforeOnHand, Integer beforeReserved, Integer beforeSalable,
                         Integer afterOnHand, Integer afterReserved, Integer afterSalable, String remark) {
        StockTxnV2 txn = new StockTxnV2();
        txn.setTxnNo("TXN-" + System.currentTimeMillis());
        txn.setSkuId(skuId);
        txn.setMainOrderNo(mainOrderNo);
        txn.setSubOrderNo(subOrderNo);
        txn.setTxnType(txnType);
        txn.setQty(qty);
        txn.setBeforeOnHand(beforeOnHand);
        txn.setBeforeReserved(beforeReserved);
        txn.setBeforeSalable(beforeSalable);
        txn.setAfterOnHand(afterOnHand);
        txn.setAfterReserved(afterReserved);
        txn.setAfterSalable(afterSalable);
        txn.setRemark(remark);
        stockTxnV2Mapper.insert(txn);
    }
}

