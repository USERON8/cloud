package com.cloud.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.common.result.PageResult;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.module.entity.Stock;

import java.util.Collection;
import java.util.List;

/**
 * @author what's up
 * @description 针对表【stock(库存主表)】的数据库操作Service
 * @createDate 2025-09-10 01:39:51
 */
public interface StockService extends IService<Stock> {


    /**
     * 根据ID获取库存信息
     *
     * @param id 库存ID
     * @return 库存DTO
     */
    StockDTO getStockById(Long id);

    /**
     * 根据商品ID获取库存信息
     *
     * @param productId 商品ID
     * @return 库存DTO
     */
    StockDTO getStockByProductId(Long productId);

    /**
     * 根据商品ID列表批量获取库存信息
     *
     * @param productIds 商品ID列表
     * @return 库存DTO列表
     */
    List<StockDTO> getStocksByProductIds(Collection<Long> productIds);

    /**
     * 分页查询库存信息
     *
     * @param pageDTO 分页查询参数
     * @return 分页结果
     */
    PageResult<StockVO> pageQuery(StockPageDTO pageDTO);


    /**
     * 删除库存信息
     *
     * @param id 库存ID
     * @return 是否成功
     */
    boolean deleteStock(Long id);

    /**
     * 批量删除库存信息
     *
     * @param ids 库存ID列表
     * @return 是否成功
     */
    boolean deleteStocksByIds(Collection<Long> ids);

    /**
     * 入库操作
     *
     * @param productId 商品ID
     * @param quantity  入库数量
     * @param remark    备注
     * @return 是否成功
     */
    boolean stockIn(Long productId, Integer quantity, String remark);

    /**
     * 出库操作
     *
     * @param productId 商品ID
     * @param quantity  出库数量
     * @param orderId   订单ID
     * @param orderNo   订单号
     * @param remark    备注
     * @return 是否成功
     */
    boolean stockOut(Long productId, Integer quantity, Long orderId, String orderNo, String remark);

    /**
     * 预留库存
     *
     * @param productId 商品ID
     * @param quantity  预留数量
     * @return 是否成功
     */
    boolean reserveStock(Long productId, Integer quantity);

    /**
     * 释放预留库存
     *
     * @param productId 商品ID
     * @param quantity  释放数量
     * @return 是否成功
     */
    boolean releaseReservedStock(Long productId, Integer quantity);

    /**
     * 检查库存是否充足
     *
     * @param productId 商品ID
     * @param quantity  所需数量
     * @return 是否充足
     */
    boolean checkStockSufficient(Long productId, Integer quantity);

    /**
     * 检查库存是否已扣减
     *
     * @param orderId 订单ID
     * @return 是否已扣减
     */
    boolean isStockDeducted(Long orderId);

    /**
     * 解冻并扣减库存
     *
     * @param event 订单完成事件
     * @return 是否成功
     */
    boolean unfreezeAndDeductStock(com.cloud.common.domain.event.OrderCompletedEvent event);

    /**
     * 检查库存是否已冻结
     *
     * @param orderId 订单ID
     * @return 是否已冻结
     */
    boolean isStockFrozen(Long orderId);

    /**
     * 冻结库存
     *
     * @param event 订单创建事件
     * @return 是否成功
     */
    boolean freezeStock(com.cloud.common.domain.event.OrderCreatedEvent event);
}

