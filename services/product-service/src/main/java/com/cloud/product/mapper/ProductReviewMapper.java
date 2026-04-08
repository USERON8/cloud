package com.cloud.product.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.ProductReview;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {

  @InterceptorIgnore(illegalSql = "1")
  @Select({
    "<script>",
    "SELECT *",
    "FROM product_review FORCE INDEX (idx_review_spu_status_deleted)",
    "WHERE deleted = 0",
    "AND audit_status = #{auditStatus}",
    "AND is_visible = #{isVisible}",
    "AND spu_id IN",
    "<foreach collection='spuIds' item='spuId' open='(' separator=',' close=')'>",
    "#{spuId}",
    "</foreach>",
    "</script>"
  })
  List<ProductReview> selectVisibleApprovedBySpuIds(
      @Param("spuIds") List<Long> spuIds,
      @Param("auditStatus") String auditStatus,
      @Param("isVisible") Integer isVisible);
}
