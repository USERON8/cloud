package com.cloud.product.converter;

import com.cloud.product.module.dto.ShopRequestDTO;
import com.cloud.product.module.entity.Shop;
import com.cloud.product.module.vo.ShopVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;








@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface ShopConverter {
    ShopConverter INSTANCE = Mappers.getMapper(ShopConverter.class);

    

    





    Shop requestDTOToEntity(ShopRequestDTO requestDTO);

    





    ShopRequestDTO entityToRequestDTO(Shop shop);

    

    





    @Mapping(target = "statusDesc", expression = "java(getStatusDesc(shop.getStatus()))")
    @Mapping(target = "productCount", ignore = true)
    @Mapping(target = "isOwner", ignore = true)
    ShopVO toVO(Shop shop);

    





    List<ShopVO> toVOList(List<Shop> shops);

    

    





    @Mapping(target = "id", ignore = true)
    @Mapping(target = "statusDesc", expression = "java(getStatusDesc(requestDTO.getStatus()))")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "productCount", ignore = true)
    @Mapping(target = "isOwner", ignore = true)
    ShopVO dtoToVO(ShopRequestDTO requestDTO);

    





    List<ShopVO> dtoToVOList(List<ShopRequestDTO> requestDTOs);

    





    default String getStatusDesc(Integer status) {
        if (status == null) {
            return "鏈煡";
        }
        return status == 1 ? "钀ヤ笟涓? : "宸插叧闂?;
    }
}
