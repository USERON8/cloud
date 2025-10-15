package com.cloud.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.order.module.entity.Refund;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款Mapper
 *
 * @author CloudDevAgent
 * @since 2025-01-15
 */
@Mapper
public interface RefundMapper extends BaseMapper<Refund> {
}
