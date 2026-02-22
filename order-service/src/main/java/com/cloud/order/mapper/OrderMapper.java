package com.cloud.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.order.dto.OrderPageQueryDTO;
import com.cloud.order.module.entity.Order;
import org.apache.ibatis.annotations.Param;









public interface OrderMapper extends BaseMapper<Order> {

    






    Page<Order> pageQuery(@Param("page") Page<Order> page, @Param("query") OrderPageQueryDTO queryDTO);

    

    






    int updateStatusToPaid(@Param("orderId") Long orderId);

    






    int updateStatusToShipped(@Param("orderId") Long orderId);

    






    int updateStatusToCompleted(@Param("orderId") Long orderId);

    






    int cancelOrderFromPending(@Param("orderId") Long orderId);

    






    int cancelOrderFromPaid(@Param("orderId") Long orderId);

    






    Order selectByIdForUpdate(@Param("orderId") Long orderId);

    








    int batchUpdateStatus(@Param("orderIds") java.util.List<Long> orderIds,
                          @Param("fromStatus") Integer fromStatus,
                          @Param("toStatus") Integer toStatus);
}
