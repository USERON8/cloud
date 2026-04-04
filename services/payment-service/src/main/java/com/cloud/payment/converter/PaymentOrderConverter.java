package com.cloud.payment.converter;

import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.module.entity.PaymentRefundEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface PaymentOrderConverter {

  PaymentOrderEntity toEntity(PaymentOrderCommandDTO command);

  PaymentRefundEntity toEntity(PaymentRefundCommandDTO command);

  PaymentOrderVO toVO(PaymentOrderEntity entity);

  PaymentRefundVO toVO(PaymentRefundEntity entity);
}
