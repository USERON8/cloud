package com.cloud.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.payment.module.entity.PaymentRefundEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentRefundMapper extends BaseMapper<PaymentRefundEntity> {
}
