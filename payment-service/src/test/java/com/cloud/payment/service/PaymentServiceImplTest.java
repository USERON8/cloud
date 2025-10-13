package com.cloud.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.payment.mapper.PaymentMapper;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.impl.PaymentServiceImpl;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * PaymentService 单元测试
 *
 * @author Claude Code
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("支付服务单元测试")
class PaymentServiceImplTest {

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment testPayment;
    private PaymentDTO testPaymentDTO;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrderId(100L);
        testPayment.setUserId(200L);
        testPayment.setAmount(new BigDecimal("299.99"));
        testPayment.setStatus(0); // 待支付
        testPayment.setChannel(1); // 支付宝

        testPaymentDTO = new PaymentDTO();
        testPaymentDTO.setId(1L);
        testPaymentDTO.setOrderId(100L);
        testPaymentDTO.setUserId(200L);
        testPaymentDTO.setAmount(new BigDecimal("299.99"));
        testPaymentDTO.setStatus(0);
    }

    @Test
    @DisplayName("检查支付记录是否存在-存在")
    void testIsPaymentRecordExists_Exists() {
        // Given
        Long orderId = 100L;
        when(paymentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // When
        boolean result = paymentService.isPaymentRecordExists(orderId);

        // Then
        assertTrue(result);
        verify(paymentMapper).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("检查支付记录是否存在-不存在")
    void testIsPaymentRecordExists_NotExists() {
        // Given
        Long orderId = 999L;
        when(paymentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // When
        boolean result = paymentService.isPaymentRecordExists(orderId);

        // Then
        assertFalse(result);
        verify(paymentMapper).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("检查支付记录是否存在-异常情况")
    void testIsPaymentRecordExists_Exception() {
        // Given
        Long orderId = 100L;
        when(paymentMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("数据库异常"));

        // When
        boolean result = paymentService.isPaymentRecordExists(orderId);

        // Then
        assertFalse(result); // 异常时返回 false
    }

    @Test
    @DisplayName("分页查询支付列表-成功")
    void testGetPaymentsPage_Success() {
        // Given
        Integer page = 1;
        Integer size = 10;
        Long userId = 200L;
        Integer status = 0;
        Integer channel = 1;

        Page<Payment> paymentPage = new Page<>(page, size);
        paymentPage.setRecords(Arrays.asList(testPayment));
        paymentPage.setTotal(1);

        when(paymentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(paymentPage);

        // When
        Page<PaymentDTO> result = paymentService.getPaymentsPage(page, size, userId, status, channel);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    @DisplayName("分页查询支付列表-无查询条件")
    void testGetPaymentsPage_NoFilters() {
        // Given
        Integer page = 1;
        Integer size = 10;

        Page<Payment> paymentPage = new Page<>(page, size);
        paymentPage.setRecords(Arrays.asList(testPayment, testPayment));
        paymentPage.setTotal(2);

        when(paymentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(paymentPage);

        // When
        Page<PaymentDTO> result = paymentService.getPaymentsPage(page, size, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotal());
    }

    @Test
    @DisplayName("根据ID获取支付信息-成功")
    void testGetPaymentById_Success() {
        // Given
        Long paymentId = 1L;
        when(paymentMapper.selectById(paymentId)).thenReturn(testPayment);

        // When
        PaymentDTO result = paymentService.getPaymentById(paymentId);

        // Then
        assertNotNull(result);
        verify(paymentMapper).selectById(paymentId);
    }

    @Test
    @DisplayName("根据ID获取支付信息-支付不存在")
    void testGetPaymentById_NotFound() {
        // Given
        Long paymentId = 999L;
        when(paymentMapper.selectById(paymentId)).thenReturn(null);

        // When
        PaymentDTO result = paymentService.getPaymentById(paymentId);

        // Then
        assertNull(result);
        verify(paymentMapper).selectById(paymentId);
    }

    @Test
    @DisplayName("创建支付记录-成功")
    void testCreatePayment_Success() {
        // Given
        when(paymentMapper.insert(any(Payment.class))).thenReturn(1);

        // When
        Long result = paymentService.createPayment(testPaymentDTO);

        // Then
        assertNotNull(result);
        verify(paymentMapper).insert(any(Payment.class));
    }

    @Test
    @DisplayName("更新支付记录-成功")
    void testUpdatePayment_Success() {
        // Given
        when(paymentMapper.selectById(testPaymentDTO.getId())).thenReturn(testPayment);
        when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);

        // When
        Boolean result = paymentService.updatePayment(testPaymentDTO);

        // Then
        assertTrue(result);
        verify(paymentMapper).selectById(testPaymentDTO.getId());
        verify(paymentMapper).updateById(any(Payment.class));
    }

    @Test
    @DisplayName("更新支付记录-支付不存在")
    void testUpdatePayment_NotFound() {
        // Given
        when(paymentMapper.selectById(testPaymentDTO.getId())).thenReturn(null);

        // When
        Boolean result = paymentService.updatePayment(testPaymentDTO);

        // Then
        assertFalse(result);
        verify(paymentMapper).selectById(testPaymentDTO.getId());
        verify(paymentMapper, never()).updateById(any(Payment.class));
    }

    @Test
    @DisplayName("删除支付记录-成功")
    void testDeletePayment_Success() {
        // Given
        Long paymentId = 1L;
        when(paymentMapper.deleteById(paymentId)).thenReturn(1);

        // When
        Boolean result = paymentService.deletePayment(paymentId);

        // Then
        assertTrue(result);
        verify(paymentMapper).deleteById(paymentId);
    }

    @Test
    @DisplayName("处理支付成功-成功")
    void testProcessPaymentSuccess_Success() {
        // Given
        Long paymentId = 1L;
        when(paymentMapper.selectById(paymentId)).thenReturn(testPayment);
        when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);

        // When
        Boolean result = paymentService.processPaymentSuccess(paymentId);

        // Then
        assertTrue(result);
        assertEquals(2, testPayment.getStatus()); // 验证状态更新为已支付
        verify(paymentMapper).selectById(paymentId);
        verify(paymentMapper).updateById(any(Payment.class));
    }

    @Test
    @DisplayName("处理支付成功-支付不存在")
    void testProcessPaymentSuccess_NotFound() {
        // Given
        Long paymentId = 999L;
        when(paymentMapper.selectById(paymentId)).thenReturn(null);

        // When
        Boolean result = paymentService.processPaymentSuccess(paymentId);

        // Then
        assertFalse(result);
        verify(paymentMapper).selectById(paymentId);
        verify(paymentMapper, never()).updateById(any(Payment.class));
    }

    @Test
    @DisplayName("处理支付失败-成功")
    void testProcessPaymentFailed_Success() {
        // Given
        Long paymentId = 1L;
        String failReason = "余额不足";
        when(paymentMapper.selectById(paymentId)).thenReturn(testPayment);
        when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);

        // When
        Boolean result = paymentService.processPaymentFailed(paymentId, failReason);

        // Then
        assertTrue(result);
        assertEquals(3, testPayment.getStatus()); // 验证状态更新为支付失败
        verify(paymentMapper).selectById(paymentId);
        verify(paymentMapper).updateById(any(Payment.class));
    }

    @Test
    @DisplayName("处理支付失败-支付不存在")
    void testProcessPaymentFailed_NotFound() {
        // Given
        Long paymentId = 999L;
        when(paymentMapper.selectById(paymentId)).thenReturn(null);

        // When
        Boolean result = paymentService.processPaymentFailed(paymentId, "测试原因");

        // Then
        assertFalse(result);
        verify(paymentMapper).selectById(paymentId);
        verify(paymentMapper, never()).updateById(any(Payment.class));
    }

    @Test
    @DisplayName("处理退款-成功")
    void testProcessRefund_Success() {
        // Given
        Long paymentId = 1L;
        BigDecimal refundAmount = new BigDecimal("100.00");
        String refundReason = "商品质量问题";
        when(paymentMapper.selectById(paymentId)).thenReturn(testPayment);
        when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);

        // When
        Boolean result = paymentService.processRefund(paymentId, refundAmount, refundReason);

        // Then
        assertTrue(result);
        assertEquals(4, testPayment.getStatus()); // 验证状态更新为已退款
        verify(paymentMapper).selectById(paymentId);
        verify(paymentMapper).updateById(any(Payment.class));
    }

    @Test
    @DisplayName("处理退款-支付不存在")
    void testProcessRefund_NotFound() {
        // Given
        Long paymentId = 999L;
        BigDecimal refundAmount = new BigDecimal("100.00");
        when(paymentMapper.selectById(paymentId)).thenReturn(null);

        // When
        Boolean result = paymentService.processRefund(paymentId, refundAmount, "测试原因");

        // Then
        assertFalse(result);
        verify(paymentMapper).selectById(paymentId);
        verify(paymentMapper, never()).updateById(any(Payment.class));
    }

    @Test
    @DisplayName("根据订单ID获取支付信息-成功")
    void testGetPaymentByOrderId_Success() {
        // Given
        Long orderId = 100L;
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testPayment);

        // When
        PaymentDTO result = paymentService.getPaymentByOrderId(orderId);

        // Then
        assertNotNull(result);
        verify(paymentMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("根据订单ID获取支付信息-支付不存在")
    void testGetPaymentByOrderId_NotFound() {
        // Given
        Long orderId = 999L;
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When
        PaymentDTO result = paymentService.getPaymentByOrderId(orderId);

        // Then
        assertNull(result);
        verify(paymentMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("风控检查-通过")
    void testRiskCheck_Pass() {
        // Given
        Long userId = 200L;
        BigDecimal amount = new BigDecimal("500.00");
        String paymentMethod = "ALIPAY";

        // When
        Boolean result = paymentService.riskCheck(userId, amount, paymentMethod);

        // Then
        assertTrue(result); // 当前实现总是返回 true
    }

    @Test
    @DisplayName("更新支付状态-成功")
    void testUpdatePaymentStatus_Success() {
        // Given
        Long paymentId = 1L;
        Integer status = 2;
        String remark = "支付成功";
        when(paymentMapper.selectById(paymentId)).thenReturn(testPayment);
        when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);

        // When
        Boolean result = paymentService.updatePaymentStatus(paymentId, status, remark);

        // Then
        assertTrue(result);
        assertEquals(status, testPayment.getStatus());
        verify(paymentMapper).selectById(paymentId);
        verify(paymentMapper).updateById(any(Payment.class));
    }

    @Test
    @DisplayName("更新支付状态-支付不存在")
    void testUpdatePaymentStatus_NotFound() {
        // Given
        Long paymentId = 999L;
        when(paymentMapper.selectById(paymentId)).thenReturn(null);

        // When
        Boolean result = paymentService.updatePaymentStatus(paymentId, 2, "测试");

        // Then
        assertFalse(result);
        verify(paymentMapper).selectById(paymentId);
        verify(paymentMapper, never()).updateById(any(Payment.class));
    }

    @Test
    @DisplayName("获取支付状态-成功")
    void testGetPaymentStatus_Success() {
        // Given
        Long paymentId = 1L;
        when(paymentMapper.selectById(paymentId)).thenReturn(testPayment);

        // When
        Integer result = paymentService.getPaymentStatus(paymentId);

        // Then
        assertNotNull(result);
        assertEquals(0, result);
        verify(paymentMapper).selectById(paymentId);
    }

    @Test
    @DisplayName("获取支付状态-支付不存在")
    void testGetPaymentStatus_NotFound() {
        // Given
        Long paymentId = 999L;
        when(paymentMapper.selectById(paymentId)).thenReturn(null);

        // When
        Integer result = paymentService.getPaymentStatus(paymentId);

        // Then
        assertNull(result);
        verify(paymentMapper).selectById(paymentId);
    }

    @Test
    @DisplayName("验证支付金额-金额匹配")
    void testValidatePaymentAmount_Match() {
        // Given
        Long paymentId = 1L;
        BigDecimal amount = new BigDecimal("299.99");
        when(paymentMapper.selectById(paymentId)).thenReturn(testPayment);

        // When
        Boolean result = paymentService.validatePaymentAmount(paymentId, amount);

        // Then
        assertTrue(result);
        verify(paymentMapper).selectById(paymentId);
    }

    @Test
    @DisplayName("验证支付金额-金额不匹配")
    void testValidatePaymentAmount_Mismatch() {
        // Given
        Long paymentId = 1L;
        BigDecimal wrongAmount = new BigDecimal("199.99");
        when(paymentMapper.selectById(paymentId)).thenReturn(testPayment);

        // When
        Boolean result = paymentService.validatePaymentAmount(paymentId, wrongAmount);

        // Then
        assertFalse(result);
        verify(paymentMapper).selectById(paymentId);
    }

    @Test
    @DisplayName("验证支付金额-支付不存在")
    void testValidatePaymentAmount_NotFound() {
        // Given
        Long paymentId = 999L;
        BigDecimal amount = new BigDecimal("299.99");
        when(paymentMapper.selectById(paymentId)).thenReturn(null);

        // When
        Boolean result = paymentService.validatePaymentAmount(paymentId, amount);

        // Then
        assertFalse(result);
        verify(paymentMapper).selectById(paymentId);
    }

    @Test
    @DisplayName("获取用户支付统计-成功")
    void testGetUserPaymentStats_Success() {
        // Given
        Long userId = 200L;
        when(paymentMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(10L)  // 总数
                .thenReturn(8L);  // 成功数

        // When
        Map<String, Object> result = paymentService.getUserPaymentStats(userId);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.get("totalCount"));
        assertEquals(8L, result.get("successCount"));
        verify(paymentMapper, times(2)).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取用户支付统计-无支付记录")
    void testGetUserPaymentStats_NoRecords() {
        // Given
        Long userId = 999L;
        when(paymentMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L)
                .thenReturn(0L);

        // When
        Map<String, Object> result = paymentService.getUserPaymentStats(userId);

        // Then
        assertNotNull(result);
        assertEquals(0L, result.get("totalCount"));
        assertEquals(0L, result.get("successCount"));
    }
}
