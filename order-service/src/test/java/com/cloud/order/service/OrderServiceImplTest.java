package com.cloud.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.order.OrderVO;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.exception.InvalidStatusException;
import com.cloud.order.converter.OrderConverter;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.order.exception.OrderServiceException;
import com.cloud.order.mapper.OrderMapper;
import com.cloud.order.module.entity.Order;
import com.cloud.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * OrderService 单元测试
 *
 * @author Claude Code
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单服务单元测试")
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderConverter orderConverter;

    @Mock
    private OrderItemService orderItemService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderDTO testOrderDTO;
    private OrderVO testOrderVO;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(100L);
        testOrder.setOrderNo("ORDER2024001");
        testOrder.setTotalAmount(new BigDecimal("299.99"));
        testOrder.setStatus(0); // 待支付

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
    }

    @Test
    @DisplayName("根据ID查询订单-成功")
    void testGetByOrderEntityId_Success() {
        // Given
        Long orderId = 1L;
        when(orderMapper.selectById(orderId)).thenReturn(testOrder);
        when(orderConverter.toDTO(testOrder)).thenReturn(testOrderDTO);

        // When
        OrderDTO result = orderService.getByOrderEntityId(orderId);

        // Then
        assertNotNull(result);
        assertEquals(orderId, result.getId());
        verify(orderMapper).selectById(orderId);
        verify(orderConverter).toDTO(testOrder);
    }

    @Test
    @DisplayName("根据ID查询订单-订单不存在")
    void testGetByOrderEntityId_NotFound() {
        // Given
        Long orderId = 999L;
        when(orderMapper.selectById(orderId)).thenReturn(null);

        // When & Then
        assertThrows(EntityNotFoundException.class,
                () -> orderService.getByOrderEntityId(orderId));
    }

    @Test
    @DisplayName("保存订单-成功")
    void testSaveOrder_Success() {
        // Given
        when(orderConverter.toEntity(testOrderDTO)).thenReturn(testOrder);
        when(orderMapper.insert(any(Order.class))).thenReturn(1);

        // When
        Boolean result = orderService.saveOrder(testOrderDTO);

        // Then
        assertTrue(result);
        verify(orderMapper).insert(any(Order.class));
    }

    @Test
    @DisplayName("更新订单-成功")
    void testUpdateOrder_Success() {
        // Given
        when(orderMapper.selectById(testOrderDTO.getId())).thenReturn(testOrder);
        when(orderConverter.toEntity(testOrderDTO)).thenReturn(testOrder);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        // When
        Boolean result = orderService.updateOrder(testOrderDTO);

        // Then
        assertTrue(result);
        verify(orderMapper).selectById(testOrderDTO.getId());
        verify(orderMapper).updateById(any(Order.class));
    }

    @Test
    @DisplayName("更新订单-订单不存在")
    void testUpdateOrder_NotFound() {
        // Given
        when(orderMapper.selectById(testOrderDTO.getId())).thenReturn(null);

        // When & Then
        assertThrows(EntityNotFoundException.class,
                () -> orderService.updateOrder(testOrderDTO));
    }

    @Test
    @DisplayName("支付订单-成功")
    void testPayOrder_Success() {
        // Given
        Long orderId = 1L;
        testOrder.setStatus(0); // 待支付状态
        when(orderMapper.selectById(orderId)).thenReturn(testOrder);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        // When
        Boolean result = orderService.payOrder(orderId);

        // Then
        assertTrue(result);
        assertEquals(1, testOrder.getStatus()); // 验证状态更新为已支付
        verify(orderMapper).selectById(orderId);
        verify(orderMapper).updateById(any(Order.class));
    }

    @Test
    @DisplayName("支付订单-订单状态不正确")
    void testPayOrder_InvalidStatus() {
        // Given
        Long orderId = 1L;
        testOrder.setStatus(1); // 已支付状态
        when(orderMapper.selectById(orderId)).thenReturn(testOrder);

        // When & Then
        assertThrows(InvalidStatusException.class,
                () -> orderService.payOrder(orderId));
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    @DisplayName("发货订单-成功")
    void testShipOrder_Success() {
        // Given
        Long orderId = 1L;
        testOrder.setStatus(1); // 已支付状态
        when(orderMapper.selectById(orderId)).thenReturn(testOrder);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        // When
        Boolean result = orderService.shipOrder(orderId);

        // Then
        assertTrue(result);
        assertEquals(2, testOrder.getStatus()); // 验证状态更新为已发货
        verify(orderMapper).selectById(orderId);
        verify(orderMapper).updateById(any(Order.class));
    }

    @Test
    @DisplayName("发货订单-订单状态不正确")
    void testShipOrder_InvalidStatus() {
        // Given
        Long orderId = 1L;
        testOrder.setStatus(0); // 待支付状态
        when(orderMapper.selectById(orderId)).thenReturn(testOrder);

        // When & Then
        assertThrows(InvalidStatusException.class,
                () -> orderService.shipOrder(orderId));
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    @DisplayName("完成订单-成功")
    void testCompleteOrder_Success() {
        // Given
        Long orderId = 1L;
        testOrder.setStatus(2); // 已发货状态
        when(orderMapper.selectById(orderId)).thenReturn(testOrder);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        // When
        Boolean result = orderService.completeOrder(orderId);

        // Then
        assertTrue(result);
        assertEquals(3, testOrder.getStatus()); // 验证状态更新为已完成
        verify(orderMapper).selectById(orderId);
        verify(orderMapper).updateById(any(Order.class));
    }

    @Test
    @DisplayName("完成订单-订单状态不正确")
    void testCompleteOrder_InvalidStatus() {
        // Given
        Long orderId = 1L;
        testOrder.setStatus(1); // 已支付状态（不是已发货）
        when(orderMapper.selectById(orderId)).thenReturn(testOrder);

        // When & Then
        assertThrows(InvalidStatusException.class,
                () -> orderService.completeOrder(orderId));
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    @DisplayName("批量更新订单状态-成功")
    void testBatchUpdateOrderStatus_Success() {
        // Given
        List<Long> orderIds = Arrays.asList(1L, 2L, 3L);
        Integer status = 1;

        // 模拟 MyBatis Plus 的 update 方法返回成功
        when(orderMapper.update(any(Order.class), any())).thenReturn(3);

        // When
        Integer result = orderService.batchUpdateOrderStatus(orderIds, status);

        // Then
        assertEquals(3, result);
        verify(orderMapper).update(any(Order.class), any());
    }

    @Test
    @DisplayName("批量更新订单状态-订单ID集合为空")
    void testBatchUpdateOrderStatus_EmptyIds() {
        // Given
        List<Long> orderIds = Arrays.asList();
        Integer status = 1;

        // When & Then
        assertThrows(OrderServiceException.class,
                () -> orderService.batchUpdateOrderStatus(orderIds, status));
    }

    @Test
    @DisplayName("批量更新订单状态-状态值为空")
    void testBatchUpdateOrderStatus_NullStatus() {
        // Given
        List<Long> orderIds = Arrays.asList(1L, 2L);
        Integer status = null;

        // When & Then
        assertThrows(OrderServiceException.class,
                () -> orderService.batchUpdateOrderStatus(orderIds, status));
    }

    @Test
    @DisplayName("批量删除订单-成功")
    void testBatchDeleteOrders_Success() {
        // Given
        List<Long> orderIds = Arrays.asList(1L, 2L, 3L);

        // 模拟 MyBatis Plus 的 removeByIds 方法返回成功
        when(orderMapper.deleteBatchIds(orderIds)).thenReturn(3);

        // When
        Integer result = orderService.batchDeleteOrders(orderIds);

        // Then
        assertEquals(3, result);
        verify(orderMapper).deleteBatchIds(orderIds);
    }

    @Test
    @DisplayName("批量删除订单-订单ID集合为空")
    void testBatchDeleteOrders_EmptyIds() {
        // Given
        List<Long> orderIds = Arrays.asList();

        // When & Then
        assertThrows(OrderServiceException.class,
                () -> orderService.batchDeleteOrders(orderIds));
    }

    @Test
    @DisplayName("分页查询订单-成功")
    void testPageQuery_Success() {
        // Given
        OrderPageQueryDTO queryDTO = new OrderPageQueryDTO();
        queryDTO.setCurrent(1L);
        queryDTO.setSize(10L);
        queryDTO.setUserId(100L);

        Page<Order> orderPage = new Page<>(1, 10);
        orderPage.setRecords(Arrays.asList(testOrder));
        orderPage.setTotal(1);

        when(orderMapper.pageQuery(any(Page.class), any(OrderPageQueryDTO.class))).thenReturn(orderPage);
        when(orderConverter.toVO(any(Order.class))).thenReturn(testOrderVO);

        // When
        Page<OrderVO> result = orderService.pageQuery(queryDTO);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        verify(orderMapper).pageQuery(any(Page.class), any(OrderPageQueryDTO.class));
    }
}
