package com.cloud.stock.service.impl;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.stock.mapper.StockLedgerMapper;
import com.cloud.stock.mapper.StockReservationMapper;
import com.cloud.stock.module.entity.StockReservation;
import com.cloud.stock.module.entity.StockTxn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockLedgerServiceImplTest {

    @Mock
    private StockLedgerMapper stockLedgerMapper;

    @Mock
    private StockReservationMapper stockReservationMapper;

    @Mock
    private StockTxnAsyncWriter stockTxnAsyncWriter;

    @InjectMocks
    private StockLedgerServiceImpl stockLedgerService;

    @Test
    void reserveShouldSkipDuplicateReservedReservation() {
        StockOperateCommandDTO command = command("S-1", 51001L, 1);
        StockReservation reservation = reservation("S-1", 51001L, 1, "RESERVED");

        when(stockReservationMapper.insert(any(StockReservation.class))).thenThrow(new org.springframework.dao.DuplicateKeyException("dup"));
        when(stockReservationMapper.selectActiveBySubOrderNoAndSkuId("S-1", 51001L)).thenReturn(reservation);

        assertThat(stockLedgerService.reserve(command)).isTrue();

        verify(stockLedgerMapper, never()).reserve(anyLong(), any());
        verify(stockReservationMapper, never()).updateById(any(StockReservation.class));
        verify(stockTxnAsyncWriter, never()).write(any(StockTxn.class));
    }

    @Test
    void reserveShouldAllowRetryAfterRollback() {
        StockOperateCommandDTO command = command("S-2", 51001L, 2);
        StockReservation reservation = reservation("S-2", 51001L, 2, "ROLLED_BACK");

        when(stockReservationMapper.insert(any(StockReservation.class))).thenThrow(new org.springframework.dao.DuplicateKeyException("dup"));
        when(stockReservationMapper.selectActiveBySubOrderNoAndSkuId("S-2", 51001L)).thenReturn(reservation);
        when(stockLedgerMapper.reserve(51001L, 2)).thenReturn(1);

        assertThat(stockLedgerService.reserve(command)).isTrue();

        verify(stockLedgerMapper).reserve(51001L, 2);
        verify(stockReservationMapper).updateById(reservation);
        verify(stockTxnAsyncWriter).write(any(StockTxn.class));
        assertThat(reservation.getStatus()).isEqualTo("RESERVED");
    }

    @Test
    void reserveShouldRejectQuantityMismatchOnDuplicateReservation() {
        StockOperateCommandDTO command = command("S-3", 51001L, 1);
        StockReservation reservation = reservation("S-3", 51001L, 3, "RESERVED");

        when(stockReservationMapper.insert(any(StockReservation.class))).thenThrow(new org.springframework.dao.DuplicateKeyException("dup"));
        when(stockReservationMapper.selectActiveBySubOrderNoAndSkuId("S-3", 51001L)).thenReturn(reservation);

        assertThatThrownBy(() -> stockLedgerService.reserve(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reservation quantity mismatch");
    }

    private StockOperateCommandDTO command(String subOrderNo, Long skuId, Integer quantity) {
        StockOperateCommandDTO command = new StockOperateCommandDTO();
        command.setSubOrderNo(subOrderNo);
        command.setSkuId(skuId);
        command.setQuantity(quantity);
        command.setReason("test");
        return command;
    }

    private StockReservation reservation(String subOrderNo, Long skuId, Integer reservedQty, String status) {
        StockReservation reservation = new StockReservation();
        reservation.setSubOrderNo(subOrderNo);
        reservation.setSkuId(skuId);
        reservation.setReservedQty(reservedQty);
        reservation.setStatus(status);
        return reservation;
    }
}
