package com.cloud.payment.converter;

import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.domain.vo.payment.PaymentVO;
import com.cloud.payment.module.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 支付转换器
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface PaymentConverter {
    PaymentConverter INSTANCE = Mappers.getMapper(PaymentConverter.class);

    /**
     * 转换支付实体为DTO
     *
     * @param payment 支付实体
     * @return 支付DTO
     */
    PaymentDTO toDTO(Payment payment);

    /**
     * 转换支付DTO为实体
     *
     * @param paymentDTO 支付DTO
     * @return 支付实体
     */
    Payment toEntity(PaymentDTO paymentDTO);

    /**
     * 转换支付实体列表为DTO列表
     *
     * @param payments 支付实体列表
     * @return 支付DTO列表
     */
    List<PaymentDTO> toDTOList(List<Payment> payments);

    /**
     * 转换支付实体为VO
     *
     * @param payment 支付实体
     * @return 支付VO
     */
    PaymentVO toVO(Payment payment);

    /**
     * 转换支付VO为实体
     *
     * @param paymentVO 支付VO
     * @return 支付实体
     */
    Payment toEntity(PaymentVO paymentVO);

    /**
     * 转换支付实体列表为VO列表
     *
     * @param payments 支付实体列表
     * @return 支付VO列表
     */
    List<PaymentVO> toVOList(List<Payment> payments);
}