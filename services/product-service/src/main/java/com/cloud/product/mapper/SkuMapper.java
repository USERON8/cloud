package com.cloud.product.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.Sku;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SkuMapper extends BaseMapper<Sku> {

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM sku FORCE INDEX (idx_sku_spu_deleted)
            WHERE spu_id = #{spuId}
              AND deleted = 0
            """)
  List<Sku> selectActiveBySpuId(@Param("spuId") Long spuId);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            <script>
            SELECT *
            FROM sku FORCE INDEX (idx_sku_spu_deleted)
            WHERE deleted = 0
              AND spu_id IN
              <foreach collection="spuIds" item="spuId" open="(" separator="," close=")">
                #{spuId}
              </foreach>
            </script>
            """)
  List<Sku> selectActiveBySpuIds(@Param("spuIds") List<Long> spuIds);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            <script>
            SELECT *
            FROM sku FORCE INDEX (PRIMARY)
            WHERE deleted = 0
              AND id IN
              <foreach collection="skuIds" item="skuId" open="(" separator="," close=")">
                #{skuId}
              </foreach>
            </script>
            """)
  List<Sku> selectActiveByIds(@Param("skuIds") List<Long> skuIds);
}
