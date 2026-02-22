package com.cloud.order.converter;

import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.vo.order.OrderVO;
import com.cloud.order.module.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;




@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface OrderConverter {
    OrderConverter INSTANCE = Mappers.getMapper(OrderConverter.class);

    





    OrderDTO toDTO(Order order);

    





    Order toEntity(OrderDTO orderDTO);

    





    List<OrderDTO> toDTOList(List<Order> orders);

    





    OrderVO toVO(Order order);

    





    Order toEntity(OrderVO orderVO);

    





    List<OrderVO> toVOList(List<Order> orders);
}
