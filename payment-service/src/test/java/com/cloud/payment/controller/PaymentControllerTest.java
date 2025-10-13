package com.cloud.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.payment.service.PaymentService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PaymentController 单元测试
 *
 * @author Claude Code
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("支付控制器单元测试")
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PaymentController paymentController;

    private PaymentDTO testPaymentDTO;

    @BeforeEach
    void setUp() {
        testPaymentDTO = new PaymentDTO();
        testPaymentDTO.setId(1L);
        testPaymentDTO.setOrderId(100L);
        testPaymentDTO.setUserId(200L);
        testPaymentDTO.setAmount(new BigDecimal("299.99"));
        testPaymentDTO.setStatus(0);

        when(authentication.getName()).thenReturn("testuser");
    }

    @Test
    @DisplayName("获取支付列表-成功")
    void testGetPayments_Success() {
        // Given
        Page<PaymentDTO> paymentPage = new Page<>(1, 10);
        paymentPage.setRecords(Arrays.asList(testPaymentDTO));
        paymentPage.setTotal(1);

        when(paymentService.getPaymentsPage(anyInt(), anyInt(), anyLong(), anyInt(), anyInt()))
                .thenReturn(paymentPage);

        // When
        Result<PageResult<PaymentDTO>> result = paymentController.getPayments(
                1, 10, 200L, 0, 1, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getTotal());
        verify(paymentService).getPaymentsPage(1, 10, 200L, 0, 1);
    }

    @Test
    @DisplayName("获取支付列表-无查询条件")
    void testGetPayments_NoFilters() {
        // Given
        Page<PaymentDTO> paymentPage = new Page<>(1, 10);
        paymentPage.setRecords(Arrays.asList(testPaymentDTO, testPaymentDTO));
        paymentPage.setTotal(2);

        when(paymentService.getPaymentsPage(anyInt(), anyInt(), isNull(), isNull(), isNull()))
                .thenReturn(paymentPage);

        // When
        Result<PageResult<PaymentDTO>> result = paymentController.getPayments(
                1, 10, null, null, null, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(2, result.getData().getTotal());
    }

    @Test
    @DisplayName("根据ID获取支付详情-成功")
    void testGetPaymentById_Success() {
        // Given
        Long paymentId = 1L;
        when(paymentService.getPaymentById(paymentId)).thenReturn(testPaymentDTO);

        // When
        Result<PaymentDTO> result = paymentController.getPaymentById(paymentId, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(testPaymentDTO, result.getData());
        verify(paymentService).getPaymentById(paymentId);
    }

    @Test
    @DisplayName("根据ID获取支付详情-支付不存在")
    void testGetPaymentById_NotFound() {
        // Given
        Long paymentId = 999L;
        when(paymentService.getPaymentById(paymentId)).thenReturn(null);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> paymentController.getPaymentById(paymentId, authentication));
        verify(paymentService).getPaymentById(paymentId);
    }

    @Test
    @DisplayName("创建支付记录-成功")
    void testCreatePayment_Success() {
        // Given
        Long paymentId = 1L;
        when(paymentService.createPayment(testPaymentDTO)).thenReturn(paymentId);

        // When
        Result<Long> result = paymentController.createPayment(testPaymentDTO);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(paymentId, result.getData());
        verify(paymentService).createPayment(testPaymentDTO);
    }

    @Test
    @DisplayName("更新支付记录-成功")
    void testUpdatePayment_Success() {
        // Given
        Long paymentId = 1L;
        when(paymentService.updatePayment(testPaymentDTO)).thenReturn(true);

        // When
        Result<Boolean> result = paymentController.updatePayment(paymentId, testPaymentDTO, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        assertEquals(paymentId, testPaymentDTO.getId()); // 验证ID被设置
        verify(paymentService).updatePayment(testPaymentDTO);
    }

    @Test
    @DisplayName("删除支付记录-成功")
    void testDeletePayment_Success() {
        // Given
        Long paymentId = 1L;
        when(paymentService.deletePayment(paymentId)).thenReturn(true);

        // When
        Result<Boolean> result = paymentController.deletePayment(paymentId);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(paymentService).deletePayment(paymentId);
    }

    @Test
    @DisplayName("处理支付成功-成功")
    void testPaymentSuccess_Success() {
        // Given
        Long paymentId = 1L;
        when(paymentService.processPaymentSuccess(paymentId)).thenReturn(true);

        // When
        Result<Boolean> result = paymentController.paymentSuccess(paymentId, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(paymentService).processPaymentSuccess(paymentId);
    }

    @Test
    @DisplayName("处理支付成功-处理失败")
    void testPaymentSuccess_ProcessFailed() {
        // Given
        Long paymentId = 1L;
        when(paymentService.processPaymentSuccess(paymentId)).thenReturn(false);

        // When & Then
        assertThrows(com.cloud.common.exception.BusinessException.class,
                () -> paymentController.paymentSuccess(paymentId, authentication));
        verify(paymentService).processPaymentSuccess(paymentId);
    }

    @Test
    @DisplayName("处理支付失败-成功")
    void testPaymentFail_Success() {
        // Given
        Long paymentId = 1L;
        String failReason = "余额不足";
        when(paymentService.processPaymentFailed(paymentId, failReason)).thenReturn(true);

        // When
        Result<Boolean> result = paymentController.paymentFail(paymentId, failReason, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(paymentService).processPaymentFailed(paymentId, failReason);
    }

    @Test
    @DisplayName("处理支付失败-处理失败")
    void testPaymentFail_ProcessFailed() {
        // Given
        Long paymentId = 1L;
        String failReason = "余额不足";
        when(paymentService.processPaymentFailed(paymentId, failReason)).thenReturn(false);

        // When & Then
        assertThrows(com.cloud.common.exception.BusinessException.class,
                () -> paymentController.paymentFail(paymentId, failReason, authentication));
        verify(paymentService).processPaymentFailed(paymentId, failReason);
    }

    @Test
    @DisplayName("处理退款-成功")
    void testRefundPayment_Success() {
        // Given
        Long paymentId = 1L;
        BigDecimal refundAmount = new BigDecimal("100.00");
        String refundReason = "商品质量问题";
        when(paymentService.processRefund(paymentId, refundAmount, refundReason)).thenReturn(true);

        // When
        Result<Boolean> result = paymentController.refundPayment(
                paymentId, refundAmount, refundReason, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(paymentService).processRefund(paymentId, refundAmount, refundReason);
    }

    @Test
    @DisplayName("处理退款-处理失败")
    void testRefundPayment_ProcessFailed() {
        // Given
        Long paymentId = 1L;
        BigDecimal refundAmount = new BigDecimal("100.00");
        String refundReason = "商品质量问题";
        when(paymentService.processRefund(paymentId, refundAmount, refundReason)).thenReturn(false);

        // When & Then
        assertThrows(com.cloud.common.exception.BusinessException.class,
                () -> paymentController.refundPayment(paymentId, refundAmount, refundReason, authentication));
        verify(paymentService).processRefund(paymentId, refundAmount, refundReason);
    }

    @Test
    @DisplayName("根据订单ID查询支付信息-成功")
    void testGetPaymentByOrderId_Success() {
        // Given
        Long orderId = 100L;
        when(paymentService.getPaymentByOrderId(orderId)).thenReturn(testPaymentDTO);

        // When
        Result<PaymentDTO> result = paymentController.getPaymentByOrderId(orderId, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(testPaymentDTO, result.getData());
        verify(paymentService).getPaymentByOrderId(orderId);
    }

    @Test
    @DisplayName("根据订单ID查询支付信息-支付不存在")
    void testGetPaymentByOrderId_NotFound() {
        // Given
        Long orderId = 999L;
        when(paymentService.getPaymentByOrderId(orderId)).thenReturn(null);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> paymentController.getPaymentByOrderId(orderId, authentication));
        verify(paymentService).getPaymentByOrderId(orderId);
    }

    @Test
    @DisplayName("支付风控检查-通过")
    void testRiskCheck_Pass() {
        // Given
        Long userId = 200L;
        BigDecimal amount = new BigDecimal("500.00");
        String paymentMethod = "ALIPAY";
        when(paymentService.riskCheck(userId, amount, paymentMethod)).thenReturn(true);

        // When
        Result<Boolean> result = paymentController.riskCheck(
                userId, amount, paymentMethod, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(paymentService).riskCheck(userId, amount, paymentMethod);
    }

    @Test
    @DisplayName("支付风控检查-不通过")
    void testRiskCheck_Fail() {
        // Given
        Long userId = 200L;
        BigDecimal amount = new BigDecimal("10000.00");
        String paymentMethod = "ALIPAY";
        when(paymentService.riskCheck(userId, amount, paymentMethod)).thenReturn(false);

        // When
        Result<Boolean> result = paymentController.riskCheck(
                userId, amount, paymentMethod, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertFalse(result.getData());
        verify(paymentService).riskCheck(userId, amount, paymentMethod);
    }
}
