package com.cloud.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrderEntity> {}
