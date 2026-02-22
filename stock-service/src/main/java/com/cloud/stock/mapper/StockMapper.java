package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;








@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    






    int updateStockQuantity(Long stockId, Integer quantity);

    






    int freezeStock(Long stockId, Integer quantity);

    






    int unfreezeStock(Long stockId, Integer quantity);

    

    







    int stockOutWithCondition(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    







    int reserveStockWithCondition(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    







    int releaseReservedStockWithCondition(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    







    int confirmStockOutWithCondition(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    







    int stockInWithCondition(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    






    Stock selectByProductIdForUpdate(@Param("productId") Long productId);
}



