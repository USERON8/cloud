package com.cloud.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.order.entity.CartItem;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM cart_item FORCE INDEX (idx_cart_item_cart_deleted)
            WHERE cart_id = #{cartId}
              AND user_id = #{userId}
              AND deleted = 0
              AND checked_out = 0
            ORDER BY id ASC
            """)
  List<CartItem> listActiveByCartIdAndUserId(
      @Param("cartId") Long cartId, @Param("userId") Long userId);

  @InterceptorIgnore(illegalSql = "1")
  @Delete(
      """
            DELETE FROM cart_item
            WHERE cart_id = #{cartId}
              AND user_id = #{userId}
            """)
  int deletePhysicalByCartIdAndUserId(@Param("cartId") Long cartId, @Param("userId") Long userId);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            <script>
            SELECT *
            FROM cart_item FORCE INDEX (uk_cart_item_user_sku)
            WHERE user_id = #{userId}
              AND deleted = 0
              AND sku_id IN
              <foreach collection="skuIds" item="skuId" open="(" separator="," close=")">
                #{skuId}
              </foreach>
            </script>
            """)
  List<CartItem> selectByUserIdAndSkuIds(
      @Param("userId") Long userId, @Param("skuIds") List<Long> skuIds);

  @Update(
      """
            UPDATE cart_item FORCE INDEX (idx_cart_item_cart_user_checked_deleted)
            SET checked_out = 1,
                deleted = 1
            WHERE cart_id = #{cartId}
              AND user_id = #{userId}
              AND checked_out = 0
              AND deleted = 0
            """)
  int markCheckedOutByCartIdAndUserId(@Param("cartId") Long cartId, @Param("userId") Long userId);
}
