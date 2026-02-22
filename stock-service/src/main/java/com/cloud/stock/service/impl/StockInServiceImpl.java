package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.stock.mapper.StockInMapper;
import com.cloud.stock.module.entity.StockIn;
import com.cloud.stock.service.StockInService;
import org.springframework.stereotype.Service;






@Service
public class StockInServiceImpl extends ServiceImpl<StockInMapper, StockIn>
        implements StockInService {

}




