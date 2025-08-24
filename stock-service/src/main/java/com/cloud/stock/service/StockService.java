package com.cloud.stock.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.stock.module.dto.StockPageQueryDTO;
import com.cloud.stock.module.entity.Stock;

/**
 * 库存服务接口
 * 提供库存相关的业务操作接口定义
 *
 * @author cloud
 * @since 1.0.0
 */
public interface StockService extends IService<Stock> {
    /**
     * 根据ID删除库存
     * @param id 库存ID
     */
    void removeById(Long id);

    /**
     * 保存库存
     * @param stock 库存实体
     * @return 是否保存成功
     */
    boolean save(Stock stock);

    /**
     * 根据ID更新库存
     * @param stock 库存实体
     * @return 是否更新成功
     */
    boolean updateById(Stock stock);

    /**
     * 根据商品ID获取库存
     * @param productId 商品ID
     * @return 库存实体
     */
    Stock getByProductId(Long productId);

    /**
     * 冻结库存数量
     * @param productId 商品ID
     * @param count 冻结数量
     * @param operator 操作员ID
     */
    void FrozenQuantity(Long productId, Integer count, String operator);

    /**
     * 解冻库存数量
     * @param productId 商品ID
     * @param count 解冻数量
     * @param operator 操作员ID
     */
    void unfreezeQuantity(Long productId, Integer count, String operator);

    /**
     * 扣减库存数量
     * @param productId 商品ID
     * @param count 扣减数量
     * @param operator 操作员ID
     */
    void reduceQuantity(Long productId, Integer count, String operator);

    /**
     * 增加库存数量
     * @param productId 商品ID
     * @param count 增加数量
     * @param operator 操作员ID
     */
    void increaseQuantity(Long productId, Integer count, String operator);
    
    /**
     * 分页查询库存列表
     *
     * @param queryDTO 查询参数
     * @return 库存分页结果
     */
    Page<Stock> pageQuery(StockPageQueryDTO queryDTO);
    
    /**
     * 扣减库存
     * 
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    boolean reduceStock(Long productId, Integer quantity);
    
    /**
     * 冻结库存
     * 
     * @param productId 商品ID
     * @param quantity 冻结数量
     * @return 是否冻结成功
     */
    boolean freezeStock(Long productId, Integer quantity);
    
    /**
     * 解冻库存
     * 
     * @param productId 商品ID
     * @param quantity 解冻数量
     * @return 是否解冻成功
     */
    boolean unfreezeStock(Long productId, Integer quantity);
    
    /**
     * 扣减库存（带操作员信息）
     * 
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @param operator 操作员
     * @return 是否扣减成功
     */
    boolean reduceStock(Long productId, Integer quantity, String operator);
    
    /**
     * 冻结库存（带操作员信息）
     * 
     * @param productId 商品ID
     * @param quantity 冻结数量
     * @param operator 操作员
     * @return 是否冻结成功
     */
    boolean freezeStock(Long productId, Integer quantity, String operator);
    
    /**
     * 解冻库存（带操作员信息）
     * 
     * @param productId 商品ID
     * @param quantity 解冻数量
     * @param operator 操作员
     * @return 是否解冻成功
     */
    boolean unfreezeStock(Long productId, Integer quantity, String operator);
    
    /**
     * 增加库存（带操作员信息）
     * 
     * @param productId 商品ID
     * @param quantity 增加数量
     * @param operator 操作员
     */
    void increaseStock(Long productId, Integer quantity, String operator);
    
    /**
     * 根据商品ID更新库存数量
     * @param productId 商品ID
     * @param quantity 库存数量
     * @return 是否更新成功
     */
    boolean updateByProductId(Long productId, Integer quantity);
}