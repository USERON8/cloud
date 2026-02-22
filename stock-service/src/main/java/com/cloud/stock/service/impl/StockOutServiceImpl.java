package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.stock.mapper.StockOutMapper;
import com.cloud.stock.module.entity.StockOut;
import com.cloud.stock.service.StockOutService;
import org.springframework.stereotype.Service;






@Service
public class StockOutServiceImpl extends ServiceImpl<StockOutMapper, StockOut>
        implements StockOutService {

}




