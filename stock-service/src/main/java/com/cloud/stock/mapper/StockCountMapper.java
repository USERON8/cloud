package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.StockCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存盘点Mapper
 *
 * @author what's up
 */
@Mapper
public interface StockCountMapper extends BaseMapper<StockCount> {
    /**
     * 根据商品ID查询盘点记录
     *
     * @param productId 商品ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 盘点记录列表
     */
    List<StockCount> selectByProductId(@Param("productId") Long productId,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 根据盘点单号查询盘点记录
     *
     * @param countNo 盘点单号
     * @return 盘点记录
     */
    StockCount selectByCountNo(@Param("countNo") String countNo);

    /**
     * 根据状态查询盘点记录
     *
     * @param status    盘点状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 盘点记录列表
     */
    List<StockCount> selectByStatus(@Param("status") String status,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 查询待确认的盘点记录数量
     *
     * @return 待确认数量
     */
    int countPendingRecords();
}
