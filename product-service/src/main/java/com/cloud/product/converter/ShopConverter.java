package com.cloud.product.converter;

import com.cloud.product.module.dto.ShopRequestDTO;
import com.cloud.product.module.entity.Shop;
import com.cloud.product.module.vo.ShopVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 店铺转换器
 * 提供店铺实体、DTO、VO之间的转换功能
 *
 * @author what's up
 * @since 1.0.0
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface ShopConverter {
    ShopConverter INSTANCE = Mappers.getMapper(ShopConverter.class);

    // ================= Entity <-> DTO 转换 =================

    /**
     * 转换店铺请求DTO为实体
     *
     * @param requestDTO 店铺请求DTO
     * @return 店铺实体
     */
    Shop requestDTOToEntity(ShopRequestDTO requestDTO);

    /**
     * 转换店铺实体为请求DTO
     *
     * @param shop 店铺实体
     * @return 店铺请求DTO
     */
    ShopRequestDTO entityToRequestDTO(Shop shop);

    // ================= Entity <-> VO 转换 =================

    /**
     * 转换店铺实体为VO
     *
     * @param shop 店铺实体
     * @return 店铺VO
     */
    @Mapping(target = "statusDesc", expression = "java(getStatusDesc(shop.getStatus()))")
    @Mapping(target = "productCount", ignore = true)
    @Mapping(target = "isOwner", ignore = true)
    ShopVO toVO(Shop shop);

    /**
     * 转换店铺实体列表为VO列表
     *
     * @param shops 店铺实体列表
     * @return 店铺VO列表
     */
    List<ShopVO> toVOList(List<Shop> shops);

    // ================= DTO <-> VO 转换 =================

    /**
     * 转换店铺请求DTO为VO
     *
     * @param requestDTO 店铺请求DTO
     * @return 店铺VO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "statusDesc", expression = "java(getStatusDesc(requestDTO.getStatus()))")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "productCount", ignore = true)
    @Mapping(target = "isOwner", ignore = true)
    ShopVO dtoToVO(ShopRequestDTO requestDTO);

    /**
     * 转换店铺请求DTO列表为VO列表
     *
     * @param requestDTOs 店铺请求DTO列表
     * @return 店铺VO列表
     */
    List<ShopVO> dtoToVOList(List<ShopRequestDTO> requestDTOs);

    /**
     * 获取店铺状态描述
     *
     * @param status 状态值
     * @return 状态描述
     */
    default String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "营业中" : "已关闭";
    }
}
