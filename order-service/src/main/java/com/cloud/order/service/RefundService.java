package com.cloud.order.service;

import com.cloud.common.result.PageResult;
import com.cloud.order.dto.RefundCreateDTO;
import com.cloud.order.dto.RefundPageDTO;
import com.cloud.order.module.entity.Refund;
import com.cloud.order.vo.RefundVO;

/**
 * 退款服务接口
 *
 * @author CloudDevAgent
 * @since 2025-01-15
 */
public interface RefundService {

    /**
     * 创建退款申请
     *
     * @param userId          用户ID
     * @param refundCreateDTO 退款创建DTO
     * @return 退款单ID
     */
    Long createRefund(Long userId, RefundCreateDTO refundCreateDTO);

    /**
     * 审核退款申请
     *
     * @param refundId    退款单ID
     * @param merchantId  商家ID
     * @param approved    是否通过
     * @param auditRemark 审核备注
     * @return 是否成功
     */
    Boolean auditRefund(Long refundId, Long merchantId, Boolean approved, String auditRemark);

    /**
     * 用户取消退款
     *
     * @param refundId 退款单ID
     * @param userId   用户ID
     * @return 是否成功
     */
    Boolean cancelRefund(Long refundId, Long userId);

    /**
     * 处理退款(调用支付服务)
     *
     * @param refundId 退款单ID
     * @return 是否成功
     */
    Boolean processRefund(Long refundId);

    /**
     * 根据ID查询退款单
     *
     * @param refundId 退款单ID
     * @return 退款单
     */
    Refund getRefundById(Long refundId);

    /**
     * 根据订单ID查询退款单
     *
     * @param orderId 订单ID
     * @return 退款单
     */
    Refund getRefundByOrderId(Long orderId);

    /**
     * 分页查询退款列表
     *
     * @param pageDTO 查询参数
     * @return 分页结果
     */
    PageResult<RefundVO> pageQuery(RefundPageDTO pageDTO);
}
