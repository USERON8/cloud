package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.StockLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;






@Mapper
public interface StockLogMapper extends BaseMapper<StockLog> {
    







    List<StockLog> selectByProductId(@Param("productId") Long productId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    





    List<StockLog> selectByOrderId(@Param("orderId") Long orderId);

    







    List<StockLog> selectByOperationType(@Param("operationType") String operationType,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
}
