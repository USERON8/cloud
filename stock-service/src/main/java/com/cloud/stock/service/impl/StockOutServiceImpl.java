package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.stock.mapper.StockOutMapper;
import com.cloud.stock.module.entity.StockOut;
import com.cloud.stock.service.StockOutService;
import org.springframework.stereotype.Service;

/**
 * @author what's up
 * @description 针对表【stock_out(出库记录表)】的数据库操作Service实现
 * @createDate 2025-09-10 01:39:52
 */
@Service
public class StockOutServiceImpl extends ServiceImpl<StockOutMapper, StockOut>
        implements StockOutService {

}




