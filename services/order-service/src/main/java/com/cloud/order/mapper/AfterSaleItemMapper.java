package com.cloud.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.order.entity.AfterSaleItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AfterSaleItemMapper extends BaseMapper<AfterSaleItem> {

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM after_sale_item
            WHERE after_sale_id = #{afterSaleId}
              AND deleted = 0
            ORDER BY id ASC
            """)
  List<AfterSaleItem> listActiveByAfterSaleId(@Param("afterSaleId") Long afterSaleId);
}
