package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.StockCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;






@Mapper
public interface StockCountMapper extends BaseMapper<StockCount> {
    







    List<StockCount> selectByProductId(@Param("productId") Long productId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    





    StockCount selectByCountNo(@Param("countNo") String countNo);

    







    List<StockCount> selectByStatus(@Param("status") String status,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    




    int countPendingRecords();
}
