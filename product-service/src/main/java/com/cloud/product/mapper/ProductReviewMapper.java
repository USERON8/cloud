package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;






@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {
    






    List<ProductReview> selectByProductId(@Param("productId") Long productId,
                                          @Param("rating") Integer rating);

    





    List<ProductReview> selectByUserId(@Param("userId") Long userId);

    





    ProductReview selectByOrderId(@Param("orderId") Long orderId);

    






    List<ProductReview> selectPendingReviews(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    





    Map<String, Object> selectReviewStatistics(@Param("productId") Long productId);

    





    int incrementLikeCount(@Param("reviewId") Long reviewId);
}
