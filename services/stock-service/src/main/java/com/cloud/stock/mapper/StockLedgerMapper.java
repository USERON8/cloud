package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.StockLedger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockLedgerMapper extends BaseMapper<StockLedger> {

    @InterceptorIgnore(illegalSql = "1")
    @Select("SELECT * FROM stock_ledger FORCE INDEX (idx_stock_ledger_sku_deleted) WHERE sku_id = #{skuId} AND deleted = 0 FOR UPDATE")
    StockLedger selectBySkuIdForUpdate(@Param("skuId") Long skuId);

    @InterceptorIgnore(illegalSql = "1")
    @Update("UPDATE stock_ledger FORCE INDEX (idx_stock_ledger_sku_deleted) SET reserved_qty = reserved_qty + #{qty}, salable_qty = salable_qty - #{qty}, updated_at = NOW() WHERE sku_id = #{skuId} AND deleted = 0 AND salable_qty >= #{qty}")
    int reserve(@Param("skuId") Long skuId, @Param("qty") Integer qty);

    @InterceptorIgnore(illegalSql = "1")
    @Update("UPDATE stock_ledger FORCE INDEX (idx_stock_ledger_sku_deleted) SET reserved_qty = reserved_qty - #{qty}, updated_at = NOW() WHERE sku_id = #{skuId} AND deleted = 0 AND reserved_qty >= #{qty}")
    int confirm(@Param("skuId") Long skuId, @Param("qty") Integer qty);

    @InterceptorIgnore(illegalSql = "1")
    @Update("UPDATE stock_ledger FORCE INDEX (idx_stock_ledger_sku_deleted) SET on_hand_qty = on_hand_qty - #{qty}, updated_at = NOW() WHERE sku_id = #{skuId} AND deleted = 0 AND on_hand_qty >= #{qty}")
    int deductOnHand(@Param("skuId") Long skuId, @Param("qty") Integer qty);

    @InterceptorIgnore(illegalSql = "1")
    @Update("UPDATE stock_ledger FORCE INDEX (idx_stock_ledger_sku_deleted) SET reserved_qty = reserved_qty - #{qty}, salable_qty = salable_qty + #{qty}, updated_at = NOW() WHERE sku_id = #{skuId} AND deleted = 0 AND reserved_qty >= #{qty}")
    int release(@Param("skuId") Long skuId, @Param("qty") Integer qty);

    @InterceptorIgnore(illegalSql = "1")
    @Update("UPDATE stock_ledger FORCE INDEX (idx_stock_ledger_sku_deleted) SET on_hand_qty = on_hand_qty + #{qty}, salable_qty = salable_qty + #{qty}, updated_at = NOW() WHERE sku_id = #{skuId} AND deleted = 0")
    int rollbackAfterConfirm(@Param("skuId") Long skuId, @Param("qty") Integer qty);

    @InterceptorIgnore(illegalSql = "1")
    @Select("SELECT * FROM stock_ledger FORCE INDEX (idx_stock_ledger_sku_deleted) WHERE sku_id = #{skuId} AND deleted = 0 LIMIT 1")
    StockLedger selectActiveBySkuId(@Param("skuId") Long skuId);
}
