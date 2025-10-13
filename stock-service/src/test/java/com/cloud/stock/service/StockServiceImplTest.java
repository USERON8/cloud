package com.cloud.stock.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.vo.stock.StockVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.exception.StockInsufficientException;
import com.cloud.stock.mapper.StockInMapper;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.mapper.StockOutMapper;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.module.entity.StockIn;
import com.cloud.stock.module.entity.StockOut;
import com.cloud.stock.service.impl.StockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * StockService 单元测试
 *
 * @author Claude Code
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("库存服务单元测试")
class StockServiceImplTest {

    @Mock
    private StockMapper stockMapper;

    @Mock
    private StockInMapper stockInMapper;

    @Mock
    private StockOutMapper stockOutMapper;

    @Mock
    private StockConverter stockConverter;

    @InjectMocks
    private StockServiceImpl stockService;

    private Stock testStock;
    private StockDTO testStockDTO;
    private StockVO testStockVO;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testStock = new Stock();
        testStock.setId(1L);
        testStock.setProductId(100L);
        testStock.setStockQuantity(1000);
        testStock.setFrozenQuantity(100);

        testStockDTO = new StockDTO();
        testStockDTO.setId(1L);
        testStockDTO.setProductId(100L);
        testStockDTO.setStockQuantity(1000);

        testStockVO = new StockVO();
        testStockVO.setId(1L);
        testStockVO.setProductId(100L);
        testStockVO.setStockQuantity(1000);
    }

    @Test
    @DisplayName("创建库存-成功")
    void testCreateStock_Success() {
        // Given
        when(stockConverter.toEntity(testStockDTO)).thenReturn(testStock);
        when(stockMapper.insert(any(Stock.class))).thenReturn(1);
        when(stockConverter.toDTO(testStock)).thenReturn(testStockDTO);

        // When
        StockDTO result = stockService.createStock(testStockDTO);

        // Then
        assertNotNull(result);
        verify(stockMapper).insert(any(Stock.class));
    }

    @Test
    @DisplayName("创建库存-库存信息为空")
    void testCreateStock_NullDTO() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> stockService.createStock(null));
        verify(stockMapper, never()).insert(any(Stock.class));
    }

    @Test
    @DisplayName("更新库存-成功")
    void testUpdateStock_Success() {
        // Given
        when(stockConverter.toEntity(testStockDTO)).thenReturn(testStock);
        when(stockMapper.updateById(any(Stock.class))).thenReturn(1);

        // When
        boolean result = stockService.updateStock(testStockDTO);

        // Then
        assertTrue(result);
        verify(stockMapper).updateById(any(Stock.class));
    }

    @Test
    @DisplayName("更新库存-库存信息为空")
    void testUpdateStock_NullDTO() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> stockService.updateStock(null));
        verify(stockMapper, never()).updateById(any(Stock.class));
    }

    @Test
    @DisplayName("根据ID获取库存-成功")
    void testGetStockById_Success() {
        // Given
        Long stockId = 1L;
        when(stockMapper.selectById(stockId)).thenReturn(testStock);
        when(stockConverter.toDTO(testStock)).thenReturn(testStockDTO);

        // When
        StockDTO result = stockService.getStockById(stockId);

        // Then
        assertNotNull(result);
        assertEquals(stockId, result.getId());
        verify(stockMapper).selectById(stockId);
    }

    @Test
    @DisplayName("根据ID获取库存-库存不存在")
    void testGetStockById_NotFound() {
        // Given
        Long stockId = 999L;
        when(stockMapper.selectById(stockId)).thenReturn(null);

        // When & Then
        assertThrows(EntityNotFoundException.class,
                () -> stockService.getStockById(stockId));
    }

    @Test
    @DisplayName("根据商品ID获取库存-成功")
    void testGetStockByProductId_Success() {
        // Given
        Long productId = 100L;
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testStock);
        when(stockConverter.toDTO(testStock)).thenReturn(testStockDTO);

        // When
        StockDTO result = stockService.getStockByProductId(productId);

        // Then
        assertNotNull(result);
        assertEquals(productId, result.getProductId());
    }

    @Test
    @DisplayName("根据商品ID获取库存-库存不存在")
    void testGetStockByProductId_NotFound() {
        // Given
        Long productId = 999L;
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When
        StockDTO result = stockService.getStockByProductId(productId);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("批量获取库存-成功")
    void testGetStocksByProductIds_Success() {
        // Given
        List<Long> productIds = Arrays.asList(100L, 101L, 102L);
        List<Stock> stocks = Arrays.asList(testStock, testStock, testStock);
        when(stockMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(stocks);
        when(stockConverter.toDTOList(stocks)).thenReturn(Arrays.asList(testStockDTO, testStockDTO, testStockDTO));

        // When
        List<StockDTO> result = stockService.getStocksByProductIds(productIds);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("批量获取库存-空列表")
    void testGetStocksByProductIds_EmptyList() {
        // Given
        List<Long> emptyIds = Arrays.asList();

        // When
        List<StockDTO> result = stockService.getStocksByProductIds(emptyIds);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("删除库存-成功")
    void testDeleteStock_Success() {
        // Given
        Long stockId = 1L;
        when(stockMapper.selectById(stockId)).thenReturn(testStock);
        when(stockMapper.deleteById(stockId)).thenReturn(1);

        // When
        boolean result = stockService.deleteStock(stockId);

        // Then
        assertTrue(result);
        verify(stockMapper).deleteById(stockId);
    }

    @Test
    @DisplayName("删除库存-库存不存在")
    void testDeleteStock_NotFound() {
        // Given
        Long stockId = 999L;
        when(stockMapper.selectById(stockId)).thenReturn(null);

        // When & Then
        assertThrows(EntityNotFoundException.class,
                () -> stockService.deleteStock(stockId));
    }

    @Test
    @DisplayName("批量删除库存-成功")
    void testDeleteStocksByIds_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(stockMapper.deleteBatchIds(ids)).thenReturn(3);

        // When
        boolean result = stockService.deleteStocksByIds(ids);

        // Then
        assertTrue(result);
        verify(stockMapper).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("批量删除库存-空列表")
    void testDeleteStocksByIds_EmptyList() {
        // Given
        List<Long> emptyIds = Arrays.asList();

        // When
        boolean result = stockService.deleteStocksByIds(emptyIds);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("库存入库-成功")
    void testStockIn_Success() {
        // Given
        Long productId = 100L;
        Integer quantity = 100;
        String remark = "采购入库";
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testStock);
        when(stockMapper.updateStockQuantity(testStock.getId(), quantity)).thenReturn(1);
        when(stockInMapper.insert(any(StockIn.class))).thenReturn(1);

        // When
        boolean result = stockService.stockIn(productId, quantity, remark);

        // Then
        assertTrue(result);
        verify(stockMapper).updateStockQuantity(testStock.getId(), quantity);
        verify(stockInMapper).insert(any(StockIn.class));
    }

    @Test
    @DisplayName("库存入库-库存不存在")
    void testStockIn_StockNotFound() {
        // Given
        Long productId = 999L;
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When & Then
        assertThrows(EntityNotFoundException.class,
                () -> stockService.stockIn(productId, 100, "测试"));
    }

    @Test
    @DisplayName("库存出库-成功")
    void testStockOut_Success() {
        // Given
        Long productId = 100L;
        Integer quantity = 50;
        Long orderId = 1000L;
        String orderNo = "ORDER001";
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testStock);
        when(stockMapper.updateStockQuantity(testStock.getId(), -quantity)).thenReturn(1);
        when(stockOutMapper.insert(any(StockOut.class))).thenReturn(1);

        // When
        boolean result = stockService.stockOut(productId, quantity, orderId, orderNo, "订单出库");

        // Then
        assertTrue(result);
        verify(stockMapper).updateStockQuantity(testStock.getId(), -quantity);
        verify(stockOutMapper).insert(any(StockOut.class));
    }

    @Test
    @DisplayName("库存出库-库存不足")
    void testStockOut_InsufficientStock() {
        // Given
        Long productId = 100L;
        Integer quantity = 2000; // 超过可用库存
        testStock.setStockQuantity(1000);
        testStock.setFrozenQuantity(100);
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testStock);

        // When & Then
        assertThrows(StockInsufficientException.class,
                () -> stockService.stockOut(productId, quantity, 1000L, "ORDER001", "测试"));
    }

    @Test
    @DisplayName("预留库存-成功")
    void testReserveStock_Success() {
        // Given
        Long productId = 100L;
        Integer quantity = 100;
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testStock);
        when(stockMapper.freezeStock(testStock.getId(), quantity)).thenReturn(1);

        // When
        boolean result = stockService.reserveStock(productId, quantity);

        // Then
        assertTrue(result);
        verify(stockMapper).freezeStock(testStock.getId(), quantity);
    }

    @Test
    @DisplayName("预留库存-库存不存在")
    void testReserveStock_StockNotFound() {
        // Given
        Long productId = 999L;
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When & Then
        assertThrows(EntityNotFoundException.class,
                () -> stockService.reserveStock(productId, 100));
    }

    @Test
    @DisplayName("释放预留库存-成功")
    void testReleaseReservedStock_Success() {
        // Given
        Long productId = 100L;
        Integer quantity = 100;
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testStock);
        when(stockMapper.unfreezeStock(testStock.getId(), quantity)).thenReturn(1);

        // When
        boolean result = stockService.releaseReservedStock(productId, quantity);

        // Then
        assertTrue(result);
        verify(stockMapper).unfreezeStock(testStock.getId(), quantity);
    }

    @Test
    @DisplayName("检查库存是否充足-充足")
    void testCheckStockSufficient_Sufficient() {
        // Given
        Long productId = 100L;
        Integer quantity = 500;
        testStock.setStockQuantity(1000);
        testStock.setFrozenQuantity(100);
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testStock);

        // When
        boolean result = stockService.checkStockSufficient(productId, quantity);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("检查库存是否充足-不足")
    void testCheckStockSufficient_Insufficient() {
        // Given
        Long productId = 100L;
        Integer quantity = 1000;
        testStock.setStockQuantity(1000);
        testStock.setFrozenQuantity(100);
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testStock);

        // When
        boolean result = stockService.checkStockSufficient(productId, quantity);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("检查库存是否已扣减-已扣减")
    void testIsStockDeducted_Yes() {
        // Given
        Long orderId = 1000L;
        when(stockOutMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // When
        boolean result = stockService.isStockDeducted(orderId);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("检查库存是否已扣减-未扣减")
    void testIsStockDeducted_No() {
        // Given
        Long orderId = 9999L;
        when(stockOutMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // When
        boolean result = stockService.isStockDeducted(orderId);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("批量创建库存-成功")
    void testBatchCreateStocks_Success() {
        // Given
        List<StockDTO> stockDTOList = Arrays.asList(testStockDTO, testStockDTO);
        when(stockConverter.toEntity(any(StockDTO.class))).thenReturn(testStock);
        when(stockMapper.insert(any(Stock.class))).thenReturn(1);
        when(stockConverter.toDTO(any(Stock.class))).thenReturn(testStockDTO);

        // When
        Integer result = stockService.batchCreateStocks(stockDTOList);

        // Then
        assertEquals(2, result);
        verify(stockMapper, times(2)).insert(any(Stock.class));
    }

    @Test
    @DisplayName("批量创建库存-空列表")
    void testBatchCreateStocks_EmptyList() {
        // Given
        List<StockDTO> emptyList = Arrays.asList();

        // When & Then
        assertThrows(BusinessException.class,
                () -> stockService.batchCreateStocks(emptyList));
    }

    @Test
    @DisplayName("批量更新库存-成功")
    void testBatchUpdateStocks_Success() {
        // Given
        List<StockDTO> stockDTOList = Arrays.asList(testStockDTO, testStockDTO);
        when(stockConverter.toEntity(any(StockDTO.class))).thenReturn(testStock);
        when(stockMapper.updateById(any(Stock.class))).thenReturn(1);

        // When
        Integer result = stockService.batchUpdateStocks(stockDTOList);

        // Then
        assertEquals(2, result);
        verify(stockMapper, times(2)).updateById(any(Stock.class));
    }

    @Test
    @DisplayName("批量入库-成功")
    void testBatchStockIn_Success() {
        // Given
        StockService.StockAdjustmentRequest request1 = new StockService.StockAdjustmentRequest();
        request1.setProductId(100L);
        request1.setQuantity(50);
        request1.setRemark("批量入库1");

        StockService.StockAdjustmentRequest request2 = new StockService.StockAdjustmentRequest();
        request2.setProductId(101L);
        request2.setQuantity(60);
        request2.setRemark("批量入库2");

        List<StockService.StockAdjustmentRequest> requests = Arrays.asList(request1, request2);
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testStock);
        when(stockMapper.updateStockQuantity(anyLong(), anyInt())).thenReturn(1);
        when(stockInMapper.insert(any(StockIn.class))).thenReturn(1);

        // When
        Integer result = stockService.batchStockIn(requests);

        // Then
        assertEquals(2, result);
        verify(stockInMapper, times(2)).insert(any(StockIn.class));
    }

    @Test
    @DisplayName("批量出库-成功")
    void testBatchStockOut_Success() {
        // Given
        StockService.StockAdjustmentRequest request1 = new StockService.StockAdjustmentRequest();
        request1.setProductId(100L);
        request1.setQuantity(50);
        request1.setOrderId(1000L);
        request1.setOrderNo("ORDER001");

        List<StockService.StockAdjustmentRequest> requests = Arrays.asList(request1);
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testStock);
        when(stockMapper.updateStockQuantity(anyLong(), anyInt())).thenReturn(1);
        when(stockOutMapper.insert(any(StockOut.class))).thenReturn(1);

        // When
        Integer result = stockService.batchStockOut(requests);

        // Then
        assertEquals(1, result);
        verify(stockOutMapper).insert(any(StockOut.class));
    }

    @Test
    @DisplayName("批量预留-成功")
    void testBatchReserveStock_Success() {
        // Given
        StockService.StockAdjustmentRequest request1 = new StockService.StockAdjustmentRequest();
        request1.setProductId(100L);
        request1.setQuantity(50);

        List<StockService.StockAdjustmentRequest> requests = Arrays.asList(request1);
        when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testStock);
        when(stockMapper.freezeStock(anyLong(), anyInt())).thenReturn(1);

        // When
        Integer result = stockService.batchReserveStock(requests);

        // Then
        assertEquals(1, result);
        verify(stockMapper).freezeStock(anyLong(), anyInt());
    }
}
