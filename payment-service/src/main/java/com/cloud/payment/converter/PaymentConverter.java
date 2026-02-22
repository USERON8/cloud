package com.cloud.payment.converter;

import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.domain.vo.payment.PaymentVO;
import com.cloud.payment.module.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;




@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface PaymentConverter {
    PaymentConverter INSTANCE = Mappers.getMapper(PaymentConverter.class);

    





    PaymentDTO toDTO(Payment payment);

    





    Payment toEntity(PaymentDTO paymentDTO);

    





    List<PaymentDTO> toDTOList(List<Payment> payments);

    





    PaymentVO toVO(Payment payment);

    





    Payment toEntity(PaymentVO paymentVO);

    





    List<PaymentVO> toVOList(List<Payment> payments);
}
