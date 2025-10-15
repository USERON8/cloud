package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.StockLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存操作日志Mapper
 *
 * @author what's up
 */
@Mapper
public interface StockLogMapper extends BaseMapper<StockLog> {
    /**
     * 根据商品ID查询日志
     *
     * @param productId 商品ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日志列表
     */
    List<StockLog> selectByProductId(@Param("productId") Long productId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 根据订单ID查询日志
     *
     * @param orderId 订单ID
     * @return 日志列表
     */
    List<StockLog> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据操作类型查询日志
     *
     * @param operationType 操作类型
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @return 日志列表
     */
    List<StockLog> selectByOperationType(@Param("operationType") String operationType,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
}
