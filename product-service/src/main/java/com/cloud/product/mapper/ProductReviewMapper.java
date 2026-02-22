package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商品评价 Mapper
 *
 * @author what's up
 */
@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {
    /**
     * 根据商品ID查询评价列表
     *
     * @param productId 商品ID
     * @param rating    评分筛选(可选)
     * @return 评价列表
     */
    List<ProductReview> selectByProductId(@Param("productId") Long productId,
                                          @Param("rating") Integer rating);

    /**
     * 根据用户ID查询评价列表
     *
     * @param userId 用户ID
     * @return 评价列表
     */
    List<ProductReview> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据订单ID查询评价
     *
     * @param orderId 订单ID
     * @return 评价记录
     */
    ProductReview selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 查询待审核评价
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 待审核评价列表
     */
    List<ProductReview> selectPendingReviews(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 统计商品评价数据
     *
     * @param productId 商品ID
     * @return 统计数据 (total_count, avg_rating, rating_1_count, rating_2_count, ...)
     */
    Map<String, Object> selectReviewStatistics(@Param("productId") Long productId);

    /**
     * 增加评价点赞数
     *
     * @param reviewId 评价ID
     * @return 更新数量
     */
    int incrementLikeCount(@Param("reviewId") Long reviewId);
}
