package com.cloud.order.converter;

import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.order.module.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 订单转换器
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface OrderConverter {
    OrderConverter INSTANCE = Mappers.getMapper(OrderConverter.class);

    /**
     * 转换订单实体为DTO
     *
     * @param order 订单实体
     * @return 订单DTO
     */
    OrderDTO toDTO(Order order);

    /**
     * 转换订单DTO为实体
     *
     * @param orderDTO 订单DTO
     * @return 订单实体
     */
    Order toEntity(OrderDTO orderDTO);

    /**
     * 转换订单实体列表为DTO列表
     *
     * @param orders 订单实体列表
     * @return 订单DTO列表
     */
    List<OrderDTO> toDTOList(List<Order> orders);
}