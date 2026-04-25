package com.cloud.payment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrderEntity> {

  String BASE_COLUMNS =
      "id, payment_no, main_order_no, sub_order_no, user_id, amount, provider, provider_app_id, "
          + "provider_merchant_id, biz_type, biz_order_key, channel, status, provider_txn_no, "
          + "idempotency_key, paid_at, poll_count, next_poll_at, last_polled_at, last_poll_error, "
          + "created_at, updated_at, deleted, version";

  @Select(
      "SELECT "
          + BASE_COLUMNS
          + " "
          + "FROM payment_order FORCE INDEX (uk_payment_order_no) "
          + "WHERE payment_no = #{paymentNo} AND deleted = 0 "
          + "LIMIT 1")
  @InterceptorIgnore(illegalSql = "1")
  PaymentOrderEntity selectByPaymentNo(@Param("paymentNo") String paymentNo);

  @Select(
      "SELECT "
          + BASE_COLUMNS
          + " "
          + "FROM payment_order FORCE INDEX (uk_payment_order_idem) "
          + "WHERE idempotency_key = #{idempotencyKey} AND deleted = 0 "
          + "LIMIT 1")
  @InterceptorIgnore(illegalSql = "1")
  PaymentOrderEntity selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

  @Select(
      "SELECT "
          + BASE_COLUMNS
          + " "
          + "FROM payment_order FORCE INDEX (idx_payment_order_main_sub_deleted) "
          + "WHERE main_order_no = #{mainOrderNo} AND sub_order_no = #{subOrderNo} AND deleted = 0 "
          + "ORDER BY id DESC LIMIT 1")
  @InterceptorIgnore(illegalSql = "1")
  PaymentOrderEntity selectLatestByMainOrderNoAndSubOrderNo(
      @Param("mainOrderNo") String mainOrderNo, @Param("subOrderNo") String subOrderNo);
}
