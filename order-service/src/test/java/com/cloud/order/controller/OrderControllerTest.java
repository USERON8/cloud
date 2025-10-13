package com.cloud.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.order.OrderVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * OrderController 单元测试
 *
 * @author Claude Code
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单控制器单元测试")
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OrderController orderController;

    private OrderDTO testOrderDTO;
    private OrderVO testOrderVO;

    @BeforeEach
    void setUp() {
        testOrderDTO = new OrderDTO();
        testOrderDTO.setId(1L);
        testOrderDTO.setUserId(100L);
        testOrderDTO.setOrderNo("ORDER2024001");
        testOrderDTO.setTotalAmount(new BigDecimal("299.99"));
        testOrderDTO.setStatus(0);

        testOrderVO = new OrderVO();
        testOrderVO.setId(1L);
        testOrderVO.setUserId(100L);
        testOrderVO.setOrderNo("ORDER2024001");
        testOrderVO.setTotalAmount(new BigDecimal("299.99"));
        testOrderVO.setStatus(0);

        when(authentication.getName()).thenReturn("testuser");
    }

    @Test
    @DisplayName("获取订单列表-成功")
    void testGetOrders_Success() {
        // Given
        Page<OrderVO> orderPage = new Page<>(1, 20);
        orderPage.setRecords(Arrays.asList(testOrderVO));
        orderPage.setTotal(1);

        when(orderService.pageQuery(any(OrderPageQueryDTO.class))).thenReturn(orderPage);

        // When
        Result<PageResult<OrderVO>> result = orderController.getOrders(1, 20, null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getTotal());
        verify(orderService).pageQuery(any(OrderPageQueryDTO.class));
    }

    @Test
    @DisplayName("根据ID获取订单详情-成功")
    void testGetOrderById_Success() {
        // Given
        Long orderId = 1L;
        when(orderService.getByOrderEntityId(orderId)).thenReturn(testOrderDTO);

        // When
        Result<OrderDTO> result = orderController.getOrderById(orderId, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(testOrderDTO, result.getData());
        verify(orderService).getByOrderEntityId(orderId);
    }

    @Test
    @DisplayName("更新订单信息-成功")
    void testUpdateOrder_Success() {
        // Given
        Long orderId = 1L;
        when(orderService.updateOrder(any(OrderDTO.class))).thenReturn(true);

        // When
        Result<Boolean> result = orderController.updateOrder(orderId, testOrderDTO, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(orderService).updateOrder(any(OrderDTO.class));
    }

    @Test
    @DisplayName("支付订单-成功")
    void testPayOrder_Success() {
        // Given
        Long orderId = 1L;
        when(orderService.payOrder(orderId)).thenReturn(true);

        // When
        Result<Boolean> result = orderController.payOrder(orderId, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(orderService).payOrder(orderId);
    }

    @Test
    @DisplayName("发货订单-成功")
    void testShipOrder_Success() {
        // Given
        Long orderId = 1L;
        when(orderService.shipOrder(orderId)).thenReturn(true);

        // When
        Result<Boolean> result = orderController.shipOrder(orderId, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(orderService).shipOrder(orderId);
    }

    @Test
    @DisplayName("完成订单-成功")
    void testCompleteOrder_Success() {
        // Given
        Long orderId = 1L;
        when(orderService.completeOrder(orderId)).thenReturn(true);

        // When
        Result<Boolean> result = orderController.completeOrder(orderId, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(orderService).completeOrder(orderId);
    }

    @Test
    @DisplayName("取消订单-成功")
    void testCancelOrder_Success() {
        // Given
        Long orderId = 1L;
        when(orderService.cancelOrder(orderId)).thenReturn(true);

        // When
        Result<Boolean> result = orderController.cancelOrder(orderId, "用户申请取消", authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(orderService).cancelOrder(orderId);
    }

    @Test
    @DisplayName("获取用户订单列表-成功")
    void testGetOrdersByUserId_Success() {
        // Given
        Long userId = 100L;
        List<OrderDTO> orders = Arrays.asList(testOrderDTO);
        when(orderService.getOrdersByUserId(userId)).thenReturn(orders);

        // When
        Result<List<OrderDTO>> result = orderController.getOrdersByUserId(userId, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(1, result.getData().size());
        verify(orderService).getOrdersByUserId(userId);
    }

    @Test
    @DisplayName("批量删除订单-成功")
    void testDeleteOrdersBatch_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(orderService.batchDeleteOrders(ids)).thenReturn(3);

        // When
        Result<Integer> result = orderController.deleteOrdersBatch(ids);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(3, result.getData());
        verify(orderService).batchDeleteOrders(ids);
    }

    @Test
    @DisplayName("批量删除订单-ID列表为空")
    void testDeleteOrdersBatch_EmptyIds() {
        // Given
        List<Long> ids = Arrays.asList();

        // When
        Result<Integer> result = orderController.deleteOrdersBatch(ids);

        // Then
        assertNotNull(result);
        assertFalse(result.getSuccess());
        verify(orderService, never()).batchDeleteOrders(any());
    }

    @Test
    @DisplayName("批量删除订单-超过最大数量限制")
    void testDeleteOrdersBatch_ExceedMaxLimit() {
        // Given
        List<Long> ids = Arrays.asList(new Long[101]); // 超过100个

        // When
        Result<Integer> result = orderController.deleteOrdersBatch(ids);

        // Then
        assertNotNull(result);
        assertFalse(result.getSuccess());
        verify(orderService, never()).batchDeleteOrders(any());
    }

    @Test
    @DisplayName("批量取消订单-成功")
    void testCancelOrdersBatch_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(orderService.batchUpdateOrderStatus(ids, 4)).thenReturn(3);

        // When
        Result<Integer> result = orderController.cancelOrdersBatch(ids, "批量取消", authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(3, result.getData());
        verify(orderService).batchUpdateOrderStatus(ids, 4);
    }

    @Test
    @DisplayName("批量发货-成功")
    void testShipOrdersBatch_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(orderService.batchUpdateOrderStatus(ids, 2)).thenReturn(3);

        // When
        Result<Integer> result = orderController.shipOrdersBatch(ids, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(3, result.getData());
        verify(orderService).batchUpdateOrderStatus(ids, 2);
    }

    @Test
    @DisplayName("批量完成订单-成功")
    void testCompleteOrdersBatch_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(orderService.batchUpdateOrderStatus(ids, 3)).thenReturn(3);

        // When
        Result<Integer> result = orderController.completeOrdersBatch(ids, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(3, result.getData());
        verify(orderService).batchUpdateOrderStatus(ids, 3);
    }

    @Test
    @DisplayName("批量支付订单-成功")
    void testPayOrdersBatch_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(orderService.batchUpdateOrderStatus(ids, 1)).thenReturn(3);

        // When
        Result<Integer> result = orderController.payOrdersBatch(ids, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(3, result.getData());
        verify(orderService).batchUpdateOrderStatus(ids, 1);
    }
}
