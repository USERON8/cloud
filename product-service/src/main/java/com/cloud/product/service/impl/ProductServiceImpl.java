package com.cloud.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.product.module.entity.Product;
import com.cloud.product.service.ProductService;
import com.cloud.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;

/**
* @author what's up
* @description 针对表【products(商品表)】的数据库操作Service实现
* @createDate 2025-08-17 20:52:34
*/
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
    implements ProductService{

}




