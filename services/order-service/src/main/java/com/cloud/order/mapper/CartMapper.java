package com.cloud.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.order.entity.Cart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM cart FORCE INDEX (idx_cart_user_deleted)
            WHERE user_id = #{userId}
              AND cart_status = 'ACTIVE'
              AND deleted = 0
            LIMIT 1
            """)
  Cart selectActiveByUserId(@Param("userId") Long userId);

  @InterceptorIgnore(illegalSql = "1")
  @Delete(
      """
            DELETE FROM cart
            WHERE user_id = #{userId}
              AND cart_status = 'CHECKED_OUT'
            """)
  int deleteCheckedOutByUserId(@Param("userId") Long userId);

  @InterceptorIgnore(illegalSql = "1")
  @Update(
      """
            UPDATE cart FORCE INDEX (PRIMARY)
            SET cart_status = 'CHECKED_OUT',
                updated_at = NOW()
            WHERE id = #{cartId}
              AND user_id = #{userId}
              AND cart_status = 'ACTIVE'
              AND deleted = 0
            """)
  int markCheckedOutByIdAndUserId(@Param("cartId") Long cartId, @Param("userId") Long userId);
}
