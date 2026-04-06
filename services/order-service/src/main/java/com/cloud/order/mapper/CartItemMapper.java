package com.cloud.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.order.entity.CartItem;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

  @Delete(
      """
            DELETE FROM cart_item
            WHERE cart_id = #{cartId}
              AND user_id = #{userId}
            """)
  int deletePhysicalByCartIdAndUserId(@Param("cartId") Long cartId, @Param("userId") Long userId);
}
