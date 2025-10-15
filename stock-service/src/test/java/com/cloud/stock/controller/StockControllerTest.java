package com.cloud.stock.controller;

import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.vo.stock.StockVO;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StockController 单元测试
 *
 * @author Claude Code
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("库存控制器单元测试")
class StockControllerTest {

    @Mock
    private StockService stockService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private StockController stockController;

    private StockDTO testStockDTO;
    private StockVO testStockVO;
    private PageResult<StockVO> testPageResult;

    @BeforeEach
    void setUp() {
        testStockDTO = new StockDTO();
        testStockDTO.setId(1L);
        testStockDTO.setProductId(100L);
        testStockDTO.setStockQuantity(1000);

        testStockVO = new StockVO();
        testStockVO.setId(1L);
        testStockVO.setProductId(100L);
        testStockVO.setStockQuantity(1000);

        testPageResult = PageResult.of(1L, 10L, 1L, Arrays.asList(testStockVO));

        when(authentication.getName()).thenReturn("testuser");
    }

    @Test
    @DisplayName("分页查询库存-成功")
    void testGetStocksPage_Success() {
        // Given
        StockPageDTO pageDTO = new StockPageDTO();
        pageDTO.setCurrent(1L);
        pageDTO.setSize(10L);
        when(stockService.pageQuery(pageDTO)).thenReturn(testPageResult);

        // When
        Result<PageResult<StockVO>> result = stockController.getStocksPage(pageDTO, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().getTotal());
        verify(stockService).pageQuery(pageDTO);
    }

    @Test
    @DisplayName("根据ID获取库存-成功")
    void testGetStockById_Success() {
        // Given
        Long stockId = 1L;
        when(stockService.getStockById(stockId)).thenReturn(testStockDTO);

        // When
        Result<StockDTO> result = stockController.getStockById(stockId, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(testStockDTO, result.getData());
        verify(stockService).getStockById(stockId);
    }

    @Test
    @DisplayName("根据ID获取库存-库存不存在")
    void testGetStockById_NotFound() {
        // Given
        Long stockId = 999L;
        when(stockService.getStockById(stockId)).thenReturn(null);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> stockController.getStockById(stockId, authentication));
    }

    @Test
    @DisplayName("根据商品ID获取库存-成功")
    void testGetByProductId_Success() {
        // Given
        Long productId = 100L;
        when(stockService.getStockByProductId(productId)).thenReturn(testStockDTO);

        // When
        Result<StockDTO> result = stockController.getByProductId(productId, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(testStockDTO, result.getData());
    }

    @Test
    @DisplayName("根据商品ID获取库存-库存不存在")
    void testGetByProductId_NotFound() {
        // Given
        Long productId = 999L;
        when(stockService.getStockByProductId(productId)).thenReturn(null);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> stockController.getByProductId(productId, authentication));
    }

    @Test
    @DisplayName("批量获取库存-成功")
    void testGetByProductIds_Success() {
        // Given
        List<Long> productIds = Arrays.asList(100L, 101L);
        List<StockDTO> stocks = Arrays.asList(testStockDTO, testStockDTO);
        when(stockService.getStocksByProductIds(productIds)).thenReturn(stocks);

        // When
        Result<List<StockDTO>> result = stockController.getByProductIds(productIds);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());
    }

    @Test
    @DisplayName("创建库存-成功")
    void testCreateStock_Success() {
        // Given
        when(stockService.createStock(testStockDTO)).thenReturn(testStockDTO);

        // When
        Result<StockDTO> result = stockController.createStock(testStockDTO);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(testStockDTO, result.getData());
        verify(stockService).createStock(testStockDTO);
    }

    @Test
    @DisplayName("更新库存-成功")
    void testUpdateStock_Success() {
        // Given
        Long stockId = 1L;
        when(stockService.updateStock(testStockDTO)).thenReturn(true);

        // When
        Result<Boolean> result = stockController.updateStock(stockId, testStockDTO, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(stockService).updateStock(testStockDTO);
    }

    @Test
    @DisplayName("删除库存-成功")
    void testDeleteStock_Success() {
        // Given
        Long stockId = 1L;
        when(stockService.deleteStock(stockId)).thenReturn(true);

        // When
        Result<Boolean> result = stockController.deleteStock(stockId);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(stockService).deleteStock(stockId);
    }

    @Test
    @DisplayName("批量删除库存-成功")
    void testDeleteBatch_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(stockService.deleteStocksByIds(ids)).thenReturn(true);

        // When
        Result<Boolean> result = stockController.deleteBatch(ids);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(stockService).deleteStocksByIds(ids);
    }

    @Test
    @DisplayName("库存入库-成功")
    void testStockIn_Success() {
        // Given
        Long productId = 100L;
        Integer quantity = 100;
        String remark = "采购入库";
        when(stockService.stockIn(productId, quantity, remark)).thenReturn(true);

        // When
        Result<Boolean> result = stockController.stockIn(productId, quantity, remark, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(stockService).stockIn(productId, quantity, remark);
    }

    @Test
    @DisplayName("库存出库-成功")
    void testStockOut_Success() {
        // Given
        Long productId = 100L;
        Integer quantity = 50;
        Long orderId = 1000L;
        String orderNo = "ORDER001";
        String remark = "订单出库";
        when(stockService.stockOut(productId, quantity, orderId, orderNo, remark)).thenReturn(true);

        // When
        Result<Boolean> result = stockController.stockOut(
                productId, quantity, orderId, orderNo, remark, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(stockService).stockOut(productId, quantity, orderId, orderNo, remark);
    }

    @Test
    @DisplayName("预留库存-成功")
    void testReserveStock_Success() {
        // Given
        Long productId = 100L;
        Integer quantity = 100;
        when(stockService.reserveStock(productId, quantity)).thenReturn(true);

        // When
        Result<Boolean> result = stockController.reserveStock(productId, quantity, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(stockService).reserveStock(productId, quantity);
    }

    @Test
    @DisplayName("释放预留库存-成功")
    void testReleaseReservedStock_Success() {
        // Given
        Long productId = 100L;
        Integer quantity = 100;
        when(stockService.releaseReservedStock(productId, quantity)).thenReturn(true);

        // When
        Result<Boolean> result = stockController.releaseReservedStock(productId, quantity, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(stockService).releaseReservedStock(productId, quantity);
    }

    @Test
    @DisplayName("检查库存是否充足-充足")
    void testCheckStockSufficient_Sufficient() {
        // Given
        Long productId = 100L;
        Integer quantity = 500;
        when(stockService.checkStockSufficient(productId, quantity)).thenReturn(true);

        // When
        Result<Boolean> result = stockController.checkStockSufficient(productId, quantity);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
    }

    @Test
    @DisplayName("检查库存是否充足-不足")
    void testCheckStockSufficient_Insufficient() {
        // Given
        Long productId = 100L;
        Integer quantity = 2000;
        when(stockService.checkStockSufficient(productId, quantity)).thenReturn(false);

        // When
        Result<Boolean> result = stockController.checkStockSufficient(productId, quantity);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertFalse(result.getData());
    }

    @Test
    @DisplayName("秒杀库存扣减-成功")
    void testSeckillStockOut_Success() {
        // Given
        Long productId = 100L;
        Integer quantity = 1;
        Long orderId = 1000L;
        String orderNo = "SECKILL001";
        when(stockService.checkStockSufficient(productId, quantity)).thenReturn(true);
        when(stockService.stockOut(productId, quantity, orderId, orderNo, "秒杀扣减")).thenReturn(true);

        // When
        Result<Boolean> result = stockController.seckillStockOut(productId, quantity, orderId, orderNo);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(stockService).checkStockSufficient(productId, quantity);
        verify(stockService).stockOut(productId, quantity, orderId, orderNo, "秒杀扣减");
    }

    @Test
    @DisplayName("批量创建库存-成功")
    void testCreateStockBatch_Success() {
        // Given
        List<StockDTO> stockDTOList = Arrays.asList(testStockDTO, testStockDTO);
        when(stockService.batchCreateStocks(stockDTOList)).thenReturn(2);

        // When
        Result<Integer> result = stockController.createStockBatch(stockDTOList);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getData());
        verify(stockService).batchCreateStocks(stockDTOList);
    }

    @Test
    @DisplayName("批量创建库存-列表为空")
    void testCreateStockBatch_EmptyList() {
        // Given
        List<StockDTO> emptyList = Arrays.asList();

        // When
        Result<Integer> result = stockController.createStockBatch(emptyList);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        verify(stockService, never()).batchCreateStocks(any());
    }

    @Test
    @DisplayName("批量更新库存-成功")
    void testUpdateStockBatch_Success() {
        // Given
        List<StockDTO> stockDTOList = Arrays.asList(testStockDTO, testStockDTO);
        when(stockService.batchUpdateStocks(stockDTOList)).thenReturn(2);

        // When
        Result<Integer> result = stockController.updateStockBatch(stockDTOList, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getData());
        verify(stockService).batchUpdateStocks(stockDTOList);
    }

    @Test
    @DisplayName("批量入库-成功")
    void testStockInBatch_Success() {
        // Given
        StockService.StockAdjustmentRequest request1 = new StockService.StockAdjustmentRequest();
        request1.setProductId(100L);
        request1.setQuantity(50);
        List<StockService.StockAdjustmentRequest> requests = Arrays.asList(request1);
        when(stockService.batchStockIn(requests)).thenReturn(1);

        // When
        Result<Integer> result = stockController.stockInBatch(requests, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData());
        verify(stockService).batchStockIn(requests);
    }

    @Test
    @DisplayName("批量出库-成功")
    void testStockOutBatch_Success() {
        // Given
        StockService.StockAdjustmentRequest request1 = new StockService.StockAdjustmentRequest();
        request1.setProductId(100L);
        request1.setQuantity(50);
        request1.setOrderId(1000L);
        request1.setOrderNo("ORDER001");
        List<StockService.StockAdjustmentRequest> requests = Arrays.asList(request1);
        when(stockService.batchStockOut(requests)).thenReturn(1);

        // When
        Result<Integer> result = stockController.stockOutBatch(requests, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData());
        verify(stockService).batchStockOut(requests);
    }

    @Test
    @DisplayName("批量预留-成功")
    void testReserveStockBatch_Success() {
        // Given
        StockService.StockAdjustmentRequest request1 = new StockService.StockAdjustmentRequest();
        request1.setProductId(100L);
        request1.setQuantity(50);
        List<StockService.StockAdjustmentRequest> requests = Arrays.asList(request1);
        when(stockService.batchReserveStock(requests)).thenReturn(1);

        // When
        Result<Integer> result = stockController.reserveStockBatch(requests, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData());
        verify(stockService).batchReserveStock(requests);
    }
}
