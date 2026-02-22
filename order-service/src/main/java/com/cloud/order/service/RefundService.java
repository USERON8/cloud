package com.cloud.order.service;

import com.cloud.common.result.PageResult;
import com.cloud.order.dto.RefundCreateDTO;
import com.cloud.order.dto.RefundPageDTO;
import com.cloud.order.module.entity.Refund;
import com.cloud.order.vo.RefundVO;







public interface RefundService {

    






    Long createRefund(Long userId, RefundCreateDTO refundCreateDTO);

    








    Boolean auditRefund(Long refundId, Long merchantId, Boolean approved, String auditRemark);

    






    Boolean cancelRefund(Long refundId, Long userId);

    





    Boolean processRefund(Long refundId);

    





    Refund getRefundById(Long refundId);

    





    Refund getRefundByOrderId(Long orderId);

    





    PageResult<RefundVO> pageQuery(RefundPageDTO pageDTO);
}
