package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.MerchantSettlementAccountDTO;
import com.cloud.common.domain.vo.MerchantSettlementAccountVO;
import com.cloud.user.module.entity.MerchantSettlementAccount;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 商家结算账户转换器
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // 忽略未映射目标属性
        unmappedSourcePolicy = ReportingPolicy.IGNORE  // 忽略未映射源属性
)
public interface MerchantSettlementAccountConverter {
    MerchantSettlementAccountConverter INSTANCE = Mappers.getMapper(MerchantSettlementAccountConverter.class);

    /**
     * 转换商家结算账户实体为DTO
     *
     * @param merchantSettlementAccount 商家结算账户实体
     * @return 商家结算账户DTO
     */
    MerchantSettlementAccountDTO toDTO(MerchantSettlementAccount merchantSettlementAccount);

    /**
     * 转换商家结算账户DTO为实体
     *
     * @param merchantSettlementAccountDTO 商家结算账户DTO
     * @return 商家结算账户实体
     */
    MerchantSettlementAccount toEntity(MerchantSettlementAccountDTO merchantSettlementAccountDTO);

    /**
     * 转换商家结算账户实体为VO
     *
     * @param merchantSettlementAccount 商家结算账户实体
     * @return 商家结算账户VO
     */
    MerchantSettlementAccountVO toVO(MerchantSettlementAccount merchantSettlementAccount);

    /**
     * 转换商家结算账户实体列表为VO列表
     *
     * @param merchantSettlementAccounts 商家结算账户实体列表
     * @return 商家结算账户VO列表
     */
    List<MerchantSettlementAccountVO> toVOList(List<MerchantSettlementAccount> merchantSettlementAccounts);
}