package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.stock.module.entity.StockOut;
import com.cloud.stock.service.StockOutService;
import com.cloud.stock.mapper.StockOutMapper;
import org.springframework.stereotype.Service;

/**
* @author what's up
* @description 针对表【stock_out(出库明细表)】的数据库操作Service实现
* @createDate 2025-08-20 13:09:40
*/
@Service
public class StockOutServiceImpl extends ServiceImpl<StockOutMapper, StockOut>
    implements StockOutService{

}




