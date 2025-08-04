package com.cloud.api.stock;

import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.dto.StockPageDTO;
import com.cloud.common.domain.vo.StockStatisticsVO;
import com.cloud.common.domain.vo.StockVO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 库存服务接口定义 (Dubbo服务接口)
 * 
 * 该接口定义了库存管理相关的所有服务方法，包括库存查询、分页查询、统计信息等。
 * 实现类需要提供同步和异步两种方式的接口实现，以满足不同业务场景的性能需求。
 */
public interface StockService {

    /**
     * 根据商品ID查询库存信息
     * 
     * 该方法用于根据指定的商品ID查询对应的库存信息，包括库存数量、冻结数量、可用数量等。
     * 如果指定商品ID的库存信息不存在，则返回null。
     *
     * @param productId 商品ID，不能为空
     * @return StockVO 库存信息展示对象，包含商品的库存详情
     * @see StockVO
     */
    StockVO getByProductId(Long productId);

    /**
     * 分页查询库存信息
     * 
     * 该方法支持按条件分页查询库存信息，可以通过商品ID、商品名称、库存状态等条件进行筛选，
     * 并支持按指定字段进行排序。查询结果以分页形式返回，包含当前页数据和分页信息。
     *
     * @param pageDTO 分页查询参数对象，包含查询条件和分页参数
     * @return PageResult<StockVO> 库存信息分页结果，包含当前页数据列表和分页信息
     * @see StockPageDTO
     * @see PageResult
     * @see StockVO
     */
    PageResult<StockVO> pageQuery(StockPageDTO pageDTO);

    /**
     * 异步根据商品ID查询库存信息
     * 
     * 该方法是{@link #getByProductId(Long)}的异步版本，适用于对性能要求较高的场景，
     * 可以避免阻塞调用线程，提高系统吞吐量。
     *
     * @param productId 商品ID，不能为空
     * @return CompletableFuture<StockVO> 异步结果，包含库存信息展示对象
     * @see StockVO
     */
    CompletableFuture<StockVO> getByProductIdAsync(Long productId);

    /**
     * 异步分页查询库存信息
     * 
     * 该方法是{@link #pageQuery(StockPageDTO)}的异步版本，适用于对性能要求较高的场景，
     * 可以避免阻塞调用线程，提高系统吞吐量。
     *
     * @param pageDTO 分页查询参数对象，包含查询条件和分页参数
     * @return CompletableFuture<PageResult<StockVO>> 异步结果，包含库存信息分页结果
     * @see StockPageDTO
     * @see PageResult
     * @see StockVO
     */
    CompletableFuture<PageResult<StockVO>> pageQueryAsync(StockPageDTO pageDTO);

    /**
     * 异步批量查询库存信息
     * 
     * 该方法用于根据多个商品ID批量查询对应的库存信息，适用于购物车、订单等需要同时
     * 查询多个商品库存的场景。采用异步方式实现，提高查询效率。
     *
     * @param productIds 商品ID列表，不能为空
     * @return CompletableFuture<List<StockVO>> 异步结果，包含库存信息展示对象列表
     * @see StockVO
     */
    CompletableFuture<List<StockVO>> batchQueryAsync(List<Long> productIds);

    /**
     * 异步查询库存统计信息
     * 
     * 该方法用于查询系统中的库存统计信息，包括商品总数、缺货商品数、库存不足商品数等统计指标。
     * 采用异步方式实现，避免阻塞调用线程。
     *
     * @return CompletableFuture<StockStatisticsVO> 异步结果，包含库存统计信息展示对象
     * @see StockStatisticsVO
     */
    CompletableFuture<StockStatisticsVO> getStatisticsAsync();
}