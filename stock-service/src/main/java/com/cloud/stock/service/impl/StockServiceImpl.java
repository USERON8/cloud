package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.stock.exception.FreezeStockException;
import com.cloud.stock.exception.InsufficientStockException;
import com.cloud.stock.exception.QueryStockException;
import com.cloud.stock.exception.ReduceStockException;
import com.cloud.stock.exception.StockNotFoundException;
import com.cloud.stock.exception.UnfreezeStockException;
import com.cloud.stock.exception.UpdateStockException;
import com.cloud.stock.mapper.StockFreezeMapper;
import com.cloud.stock.mapper.StockInMapper;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.mapper.StockOutMapper;
import com.cloud.stock.module.dto.StockPageQueryDTO;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.module.entity.StockFreeze;
import com.cloud.stock.module.entity.StockOut;
import com.cloud.stock.mq.producer.StockLogProducer;
import com.cloud.stock.service.StockOutService;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 库存服务实现类
 * 实现库存相关的业务操作
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock>
        implements StockService {
    private final StockMapper stockMapper;
    private final StockInMapper stockInMapper;
    private final StockOutMapper stockOutMapper;
    private final StockFreezeMapper stockFreezeMapper;
    private final StockLogProducer stockLogProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String STOCK_INFO = "stock:info:";
    private static final int STOCK_CACHE_TTL = 300;
    private final StockOutService stockOutService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Stock stock) {
        try {
            // 初始化库存状态
            updateStockStatus(stock);
            
            boolean saved = super.save(stock);
            if (saved) {
                // 缓存库存信息
                redisTemplate.opsForValue().set(STOCK_INFO + stock.getProductId(), stock, STOCK_CACHE_TTL, TimeUnit.SECONDS);
            }

            log.info("保存库存成功, stock: {}", stock);
            if (saved) {
                // 发送库存类型创建消息
                stockLogProducer.sendStockTypeChangeMessage(
                        stock.getId(),
                        stock.getProductId(),
                        stock.getProductName(),
                        1, // 1表示创建库存类型
                        "system"
                );
            }
            return saved;
        } catch (Exception e) {
            log.error("保存库存信息失败, stock: {}", stock, e);
            throw new UpdateStockException("保存库存信息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(Stock stock) {
        try {
            // 更新库存状态
            updateStockStatus(stock);
            
            boolean updated = super.updateById(stock);
            if (updated) {
                // 缓存库存信息
                redisTemplate.opsForValue().set(STOCK_INFO + stock.getProductId(), stock, STOCK_CACHE_TTL, TimeUnit.SECONDS);
            }
            if (updated) {
                // 发送库存类型更新消息
                stockLogProducer.sendStockTypeChangeMessage(
                        stock.getId(),
                        stock.getProductId(),
                        stock.getProductName(),
                        2, // 2表示更新库存类型
                        "system"
                );
            }
            return updated;
        } catch (Exception e) {
            log.error("更新库存信息失败, stockId: {}", stock.getId(), e);
            throw new UpdateStockException("更新库存信息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Long id) {
        try {
            Stock stock = this.getById(id);
            if (stock == null) {
                throw new StockNotFoundException(id);
            }
            
            super.removeById(id);
            redisTemplate.delete(STOCK_INFO + stock.getProductId());
            // 发送库存类型删除消息
            stockLogProducer.sendStockTypeChangeMessage(
                    id,
                    stock.getProductId(),
                    stock.getProductName(),
                    3, // 3表示删除库存类型
                    "system"
            );
        } catch (StockNotFoundException e) {
            log.warn("删除库存信息失败，库存记录不存在, stockId: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("删除库存信息失败, stockId: {}", id, e);
            throw new UpdateStockException("删除库存信息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Stock getByProductId(Long productId) {
        try {
            if (productId == null || productId <= 0) {
                log.warn("获取库存信息失败：商品ID无效, productId: {}", productId);
                throw new QueryStockException("商品ID无效");
            }
            // 先从缓存中获取
            String cacheKey = STOCK_INFO + productId;
            Stock cachedStock = (Stock) redisTemplate.opsForValue().get(cacheKey);
            if (cachedStock != null) {
                log.info("从缓存中获取库存信息, productId: {}", productId);
                return cachedStock;
            }

            Stock stock = stockMapper.getByProductId(productId);
            if (stock != null) {
                // 计算可用库存量
                stock.setAvailableQuantity(stock.getStockQuantity() - stock.getFrozenQuantity());
                // 将查询结果存入缓存
                redisTemplate.opsForValue().set(cacheKey, stock, STOCK_CACHE_TTL, TimeUnit.SECONDS);
            } else {
                log.warn("获取库存信息失败：库存记录不存在, productId: {}", productId);
            }

            return stock;
        } catch (QueryStockException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取库存信息失败, productId: {}", productId, e);
            throw new QueryStockException("获取库存信息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void FrozenQuantity(Long productId, Integer count, String operator) {
        try {
            if (productId == null || productId <= 0) {
                log.warn("冻结库存信息：商品ID无效, productId: {}", productId);
                throw new QueryStockException("商品ID无效");
            }
            
            if (count == null || count <= 0) {
                log.warn("冻结库存信息：冻结数量无效, count: {}", count);
                throw new FreezeStockException("冻结数量无效");
            }
            
            Stock stock = stockMapper.getByProductId(productId);
            if (stock == null) {
                log.warn("冻结库存信息：库存记录不存在, productId: {}", productId);
                throw new StockNotFoundException("库存记录不存在");
            }

            String cacheKey = STOCK_INFO + productId;
            redisTemplate.opsForValue().set(cacheKey, stock, STOCK_CACHE_TTL, TimeUnit.SECONDS);
            int beforeStockFrozenQuantity = stock.getFrozenQuantity();
            int afterStockFrozenQuantity = stock.getFrozenQuantity() + count;

            stock.setFrozenQuantity(afterStockFrozenQuantity);
            
            // 更新库存状态
            updateStockStatus(stock);

            int result = stockMapper.updateById(stock);
            if (result <= 0) {
                log.error("冻结库存失败：更新数据库失败, productId: {}, count: {}", productId, count);
                throw new FreezeStockException("更新数据库失败");
            }

            stockLogProducer.sendStockCountChangeMessage(
                    stock.getProductId(),
                    stock.getProductName(),
                    beforeStockFrozenQuantity, // 变更前数量
                    count, // 变更数量
                    afterStockFrozenQuantity, // 变更后数量
                    3, // 3表示冻结库存
                    operator // 操作人
            );
            log.info("冻结库存成功, productId: {}, count: {}, operator: {}", productId, count, operator);
            // 将结果存入缓存
        } catch (QueryStockException | StockNotFoundException | FreezeStockException e) {
            throw e;
        } catch (Exception e) {
            log.error("冻结库存异常, productId: {}, count: {}", productId, count, e);
            throw new FreezeStockException("冻结库存异常: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeQuantity(Long productId, Integer count, String operator) {
        try {
            if (productId == null || productId <= 0) {
                log.warn("解冻库存信息：商品ID无效, productId: {}", productId);
                throw new QueryStockException("商品ID无效");
            }
            
            if (count == null || count <= 0) {
                log.warn("解冻库存信息：解冻数量无效, count: {}", count);
                throw new UnfreezeStockException("解冻数量无效");
            }
            
            Stock stock = stockMapper.getByProductId(productId);
            if (stock == null) {
                log.warn("解冻库存信息：库存记录不存在, productId: {}", productId);
                throw new StockNotFoundException("库存记录不存在");
            }

            String cacheKey = STOCK_INFO + productId;
            redisTemplate.opsForValue().set(cacheKey, stock, STOCK_CACHE_TTL, TimeUnit.SECONDS);
            int beforeStockFrozenQuantity = stock.getFrozenQuantity();
            int afterStockFrozenQuantity = stock.getFrozenQuantity() - count;

            stock.setFrozenQuantity(afterStockFrozenQuantity);
            
            // 更新库存状态
            updateStockStatus(stock);

            int result = stockMapper.updateById(stock);
            if (result <= 0) {
                log.error("解冻库存失败：更新数据库失败, productId: {}, count: {}", productId, count);
                throw new UnfreezeStockException("更新数据库失败");
            }

            stockLogProducer.sendStockCountChangeMessage(
                    productId,
                    stock.getProductName(),
                    beforeStockFrozenQuantity,
                    count,
                    afterStockFrozenQuantity,
                    4,
                    operator
            );
            log.info("解冻库存成功, productId: {}, count: {}, operator: {}", productId, count, operator);
        } catch (QueryStockException | StockNotFoundException | UnfreezeStockException e) {
            throw e;
        } catch (Exception e) {
            log.error("解冻库存异常, productId: {}, count: {}", productId, count, e);
            throw new UnfreezeStockException("解冻库存异常: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reduceQuantity(Long productId, Integer count, String operator) {
        try {
            if (productId == null || productId <= 0) {
                log.warn("扣减库存：商品ID无效, productId: {}", productId);
                throw new QueryStockException("商品ID无效");
            }
            
            if (count == null || count <= 0) {
                log.warn("扣减库存：扣减数量无效, count: {}", count);
                throw new ReduceStockException("扣减数量无效");
            }
            
            Stock stock = stockMapper.getByProductId(productId);
            if (stock == null) {
                log.warn("扣减库存：库存记录不存在, productId: {}", productId);
                throw new StockNotFoundException("库存记录不存在");
            }
            
            if (stock.getStockQuantity() < count) {
                log.warn("扣减库存：库存不足, 当前库存: {}, 需要扣减: {}", stock.getStockQuantity(), count);
                throw new InsufficientStockException("库存不足");
            }
            
            stock.setStockQuantity(stock.getStockQuantity() - count);
            
            // 更新库存状态
            updateStockStatus(stock);

            String cacheKey = STOCK_INFO + productId;
            redisTemplate.opsForValue().set(cacheKey, stock, STOCK_CACHE_TTL, TimeUnit.SECONDS);
            int beforeStockQuantity = stock.getStockQuantity();
            int afterStockQuantity = stock.getStockQuantity() - count;

            int result = stockMapper.updateById(stock);
            if (result <= 0) {
                log.error("扣减库存失败：更新数据库失败, productId: {}, count: {}", productId, count);
                throw new ReduceStockException("更新数据库失败");
            }

            stockLogProducer.sendStockCountChangeMessage(
                    productId,
                    stock.getProductName(),
                    beforeStockQuantity,
                    count,
                    afterStockQuantity,
                    2,
                    operator
            );
            log.info("扣减库存成功, productId: {}, count: {}, operator: {}", productId, count, operator);
        } catch (QueryStockException | StockNotFoundException | InsufficientStockException | ReduceStockException e) {
            throw e;
        } catch (Exception e) {
            log.error("扣减库存异常, productId: {}, count: {}", productId, count, e);
            throw new ReduceStockException("扣减库存异常: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseQuantity(Long productId, Integer count, String operator) {
        try {
            if (productId == null || productId <= 0) {
                log.warn("增加库存失败：商品ID无效, productId: {}", productId);
                throw new QueryStockException("商品ID无效");
            }
            
            if (count == null || count <= 0) {
                log.warn("增加库存失败：增加数量无效, count: {}", count);
                throw new UpdateStockException("增加数量无效");
            }
            
            Stock stock = stockMapper.getByProductId(productId);
            if (stock == null) {
                log.warn("增加库存失败：库存记录不存在, productId: {}", productId);
                throw new StockNotFoundException("库存记录不存在");
            }

            stock.setStockQuantity(stock.getStockQuantity() + count);
            
            // 更新库存状态
            updateStockStatus(stock);

            String cacheKey = STOCK_INFO + productId;
            redisTemplate.opsForValue().set(cacheKey, stock, STOCK_CACHE_TTL, TimeUnit.SECONDS);
            int beforeStockQuantity = stock.getStockQuantity();
            int afterStockQuantity = stock.getStockQuantity() + count;

            int result = stockMapper.updateById(stock);
            if (result <= 0) {
                log.error("增加库存失败：更新数据库失败, productId: {}, count: {}", productId, count);
                throw new UpdateStockException("更新数据库失败");
            }
            
            stockLogProducer.sendStockCountChangeMessage(
                    productId,
                    stock.getProductName(),
                    beforeStockQuantity,
                    count,
                    afterStockQuantity,
                    1,
                    operator);
            log.info("增加库存成功, productId: {}, count: {}, operator: {}", productId, count, operator);
        } catch (QueryStockException | StockNotFoundException | UpdateStockException e) {
            throw e;
        } catch (Exception e) {
            log.error("增加库存异常, productId: {}, count: {}", productId, count, e);
            throw new UpdateStockException("增加库存异常: " + e.getMessage());
        }
    }
    
    /**
     * 增加库存（带操作员信息）
     * 
     * @param productId 商品ID
     * @param quantity 增加数量
     * @param operator 操作员
     */
    @Transactional(rollbackFor = Exception.class)
    public void increaseStock(Long productId, Integer quantity, String operator) {
        try {
            if (productId == null || productId <= 0) {
                log.warn("增加库存失败：商品ID无效, productId: {}", productId);
                throw new QueryStockException("商品ID无效");
            }
            
            if (quantity == null || quantity <= 0) {
                log.warn("增加库存失败：增加数量无效, quantity: {}", quantity);
                throw new UpdateStockException("增加数量无效");
            }
            
            // 从缓存或数据库获取库存信息
            Stock stock = getByProductId(productId);
            if (stock == null) {
                log.warn("增加库存失败：商品库存不存在, productId: {}", productId);
                throw new StockNotFoundException("商品库存不存在");
            }
            
            // 记录增加前的状态
            int beforeStockQuantity = stock.getStockQuantity();
            int afterStockQuantity = stock.getStockQuantity() + quantity;
            
            // 更新库存数量
            stock.setStockQuantity(afterStockQuantity);
            
            // 更新库存状态
            updateStockStatus(stock);
            
            // 更新数据库
            boolean updated = super.updateById(stock);
            if (!updated) {
                log.error("增加库存失败：更新数据库失败, productId: {}", productId);
                throw new UpdateStockException("更新数据库失败");
            }
            
            // 更新缓存
            String cacheKey = STOCK_INFO + productId;
            redisTemplate.opsForValue().set(cacheKey, stock, STOCK_CACHE_TTL, TimeUnit.SECONDS);
            
            // 发送库存变更日志消息
            stockLogProducer.sendStockCountChangeMessage(
                    productId,
                    stock.getProductName(),
                    beforeStockQuantity,   // 变更前数量
                    quantity,              // 变更数量
                    afterStockQuantity,    // 变更后数量
                    1,                     // 1表示增加库存
                    operator               // 操作人
            );
            
            log.info("增加库存成功, productId: {}, 增加数量: {}, 操作人: {}", productId, quantity, operator);
        } catch (QueryStockException | StockNotFoundException | UpdateStockException e) {
            throw e;
        } catch (Exception e) {
            log.error("增加库存异常, productId: {}, quantity: {}", productId, quantity, e);
            throw new UpdateStockException("增加库存异常: " + e.getMessage());
        }
    }

    @Override
    public Page<Stock> pageQuery(StockPageQueryDTO queryDTO) {
        try {
            // 创建分页对象
            Page<Stock> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
            
            // 执行分页查询
            Page<Stock> resultPage = stockMapper.pageQuery(page, queryDTO);
            
            // 计算可用库存量
            resultPage.getRecords().forEach(stock -> {
                stock.setAvailableQuantity(stock.getStockQuantity() - stock.getFrozenQuantity());
                updateStockStatus(stock);
            });
            
            return resultPage;
        } catch (Exception e) {
            log.error("分页查询库存列表失败, queryDTO: {}", queryDTO, e);
            throw new QueryStockException("分页查询库存列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新库存状态
     * @param stock 库存实体
     */
    private void updateStockStatus(Stock stock) {
        // 可用库存 = 总库存 - 冻结库存
        int availableQuantity = stock.getStockQuantity() - stock.getFrozenQuantity();
        
        // 根据可用库存更新状态
        if (availableQuantity <= 0) {
            stock.setStockStatus(2); // 缺货
        } else if (stock.getStockStatus() != null && stock.getStockStatus() == 3) {
            // 如果已经是下架状态，保持不变
            return;
        } else {
            stock.setStockStatus(1); // 正常
        }
    }
    
    @Override
    public boolean updateByProductId(Long productId, Integer quantity) {
        try {
            Stock stock = getByProductId(productId);
            if (stock == null) {
                log.warn("更新库存失败：库存记录不存在, productId: {}", productId);
                return false;
            }
            
            stock.setStockQuantity(quantity);
            boolean result = updateById(stock);
            
            // 如果更新成功且库存存在冻结状态，需要记录日志
            if (result && stock.getFrozenQuantity() > 0) {
                String cacheKey = STOCK_INFO + productId;
                redisTemplate.opsForValue().set(cacheKey, stock, STOCK_CACHE_TTL, TimeUnit.SECONDS);
                
                // 发送库存变更日志消息
                stockLogProducer.sendStockCountChangeMessage(
                        productId,
                        stock.getProductName(),
                        stock.getStockQuantity() + quantity,  // 变更前数量
                        quantity,                             // 变更数量
                        stock.getStockQuantity(),             // 变更后数量
                        5,                                    // 5表示更新库存
                        "system"                              // 操作人
                );
            }
            
            return result;
        } catch (Exception e) {
            log.error("更新库存失败, productId: {}, quantity: {}", productId, quantity, e);
            return false;
        }
    }
    
    /**
     * 扣减库存
     * 
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reduceStock(Long productId, Integer quantity) {
        return reduceStock(productId, quantity, "system");
    }
    
    /**
     * 冻结库存
     * 
     * @param productId 商品ID
     * @param quantity 冻结数量
     * @return 是否冻结成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean freezeStock(Long productId, Integer quantity) {
        return freezeStock(productId, quantity, "system");
    }
    
    /**
     * 解冻库存
     * 
     * @param productId 商品ID
     * @param quantity 解冻数量
     * @return 是否解冻成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfreezeStock(Long productId, Integer quantity) {
        return unfreezeStock(productId, quantity, "system");
    }

    /**
     * 扣减库存（带操作员信息）
     * 
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @param operator 操作员
     * @return 是否扣减成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reduceStock(Long productId, Integer quantity, String operator) {
        try {
            if (productId == null || productId <= 0) {
                log.warn("扣减库存失败：商品ID无效, productId: {}", productId);
                throw new QueryStockException("商品ID无效");
            }
            
            if (quantity == null || quantity <= 0) {
                log.warn("扣减库存失败：扣减数量无效, quantity: {}", quantity);
                throw new ReduceStockException("扣减数量无效");
            }
            
            // 从缓存或数据库获取库存信息
            Stock stock = getByProductId(productId);
            if (stock == null) {
                log.warn("扣减库存失败：商品库存不存在, productId: {}", productId);
                throw new StockNotFoundException("商品库存不存在");
            }
            
            // 检查冻结库存是否足够
            if (stock.getFrozenQuantity() < quantity) {
                log.warn("扣减库存失败：冻结库存不足, 当前冻结库存: {}, 需要扣减: {}", stock.getFrozenQuantity(), quantity);
                throw new InsufficientStockException("冻结库存不足");
            }
            
            // 记录扣减前的状态
            int beforeFrozenQuantity = stock.getFrozenQuantity();
            int afterFrozenQuantity = stock.getFrozenQuantity() - quantity;
            int beforeStockQuantity = stock.getStockQuantity();
            int afterStockQuantity = stock.getStockQuantity() - quantity;
            
            // 更新库存数量和冻结库存数量
            stock.setStockQuantity(afterStockQuantity);
            stock.setFrozenQuantity(afterFrozenQuantity);
            
            // 更新库存状态
            updateStockStatus(stock);
            
            // 更新数据库
            boolean updated = super.updateById(stock);
            if (!updated) {
                log.error("扣减库存失败：更新数据库失败, productId: {}", productId);
                throw new ReduceStockException("更新数据库失败");
            }
            
            // 更新缓存
            String cacheKey = STOCK_INFO + productId;
            redisTemplate.opsForValue().set(cacheKey, stock, STOCK_CACHE_TTL, TimeUnit.SECONDS);
            
            // 记录出库日志到MySQL
            StockOut stockOut = new StockOut();
            stockOut.setProductId(productId);
            stockOut.setQuantity(quantity);
            // 注意：在实际应用中，应该从上下文获取订单ID
            stockOut.setOrderId(0L); // 临时设置为0，实际应该从消息中获取
            stockOutMapper.insert(stockOut);
            
            // 发送库存变更日志消息
            stockLogProducer.sendStockCountChangeMessage(
                    productId,
                    stock.getProductName(),
                    beforeStockQuantity,  // 变更前数量
                    -quantity,            // 变更数量(负数表示减少)
                    afterStockQuantity,   // 变更后数量
                    2,                    // 2表示扣减库存
                    operator              // 操作人
            );
            
            log.info("扣减库存成功, productId: {}, 扣减数量: {}, 操作人: {}", productId, quantity, operator);
            return true;
        } catch (QueryStockException | StockNotFoundException | InsufficientStockException | ReduceStockException e) {
            throw e;
        } catch (Exception e) {
            log.error("扣减库存异常, productId: {}, quantity: {}", productId, quantity, e);
            throw new ReduceStockException("扣减库存异常: " + e.getMessage());
        }
    }

    /**
     * 冻结库存（带操作员信息）
     * 
     * @param productId 商品ID
     * @param quantity 冻结数量
     * @param operator 操作员
     * @return 是否冻结成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean freezeStock(Long productId, Integer quantity, String operator) {
        try {
            if (productId == null || productId <= 0) {
                log.warn("冻结库存失败：商品ID无效, productId: {}", productId);
                throw new QueryStockException("商品ID无效");
            }
            
            if (quantity == null || quantity <= 0) {
                log.warn("冻结库存失败：冻结数量无效, quantity: {}", quantity);
                throw new FreezeStockException("冻结数量无效");
            }
            
            // 从缓存或数据库获取库存信息
            Stock stock = getByProductId(productId);
            if (stock == null) {
                log.warn("冻结库存失败：商品库存不存在, productId: {}", productId);
                throw new StockNotFoundException("商品库存不存在");
            }
            
            // 检查库存是否足够
            if (stock.getStockQuantity() < quantity) {
                log.warn("冻结库存失败：库存不足, 当前库存: {}, 需要冻结: {}", stock.getStockQuantity(), quantity);
                throw new InsufficientStockException("库存不足");
            }
            
            // 记录冻结前的状态
            int beforeFrozenQuantity = stock.getFrozenQuantity();
            int afterFrozenQuantity = stock.getFrozenQuantity() + quantity;
            int beforeStockQuantity = stock.getStockQuantity();
            
            // 更新冻结库存数量
            stock.setFrozenQuantity(afterFrozenQuantity);
            
            // 更新库存状态
            updateStockStatus(stock);
            
            // 更新数据库
            boolean updated = super.updateById(stock);
            if (!updated) {
                log.error("冻结库存失败：更新数据库失败, productId: {}", productId);
                throw new FreezeStockException("更新数据库失败");
            }
            
            // 更新缓存
            String cacheKey = STOCK_INFO + productId;
            redisTemplate.opsForValue().set(cacheKey, stock, STOCK_CACHE_TTL, TimeUnit.SECONDS);
            
            // 记录冻结日志到MySQL
            StockFreeze stockFreeze = new StockFreeze();
            stockFreeze.setProductId(productId);
            stockFreeze.setQuantity(quantity);
            // 注意：在实际应用中，应该从上下文获取订单ID
            stockFreeze.setOrderId(0L); // 临时设置为0，实际应该从消息中获取
            stockFreeze.setStatus(1); // 1表示已冻结
            stockFreezeMapper.insert(stockFreeze);
            
            // 发送库存变更日志消息
            stockLogProducer.sendStockCountChangeMessage(
                    productId,
                    stock.getProductName(),
                    beforeStockQuantity,   // 变更前数量
                    quantity,              // 变更数量
                    beforeStockQuantity,   // 变更后数量(总库存不变)
                    3,                     // 3表示冻结库存
                    operator               // 操作人
            );
            
            log.info("冻结库存成功, productId: {}, 冻结数量: {}, 操作人: {}", productId, quantity, operator);
            return true;
        } catch (QueryStockException | StockNotFoundException | InsufficientStockException | FreezeStockException e) {
            throw e;
        } catch (Exception e) {
            log.error("冻结库存异常, productId: {}, quantity: {}", productId, quantity, e);
            throw new FreezeStockException("冻结库存异常: " + e.getMessage());
        }
    }
    
    /**
     * 解冻库存（带操作员信息）
     * 
     * @param productId 商品ID
     * @param quantity 解冻数量
     * @param operator 操作员
     * @return 是否解冻成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfreezeStock(Long productId, Integer quantity, String operator) {
        try {
            if (productId == null || productId <= 0) {
                log.warn("解冻库存失败：商品ID无效, productId: {}", productId);
                throw new QueryStockException("商品ID无效");
            }
            
            if (quantity == null || quantity <= 0) {
                log.warn("解冻库存失败：解冻数量无效, quantity: {}", quantity);
                throw new UnfreezeStockException("解冻数量无效");
            }
            
            // 从缓存或数据库获取库存信息
            Stock stock = getByProductId(productId);
            if (stock == null) {
                log.warn("解冻库存失败：商品库存不存在, productId: {}", productId);
                throw new StockNotFoundException("商品库存不存在");
            }
            
            // 检查冻结库存是否足够
            if (stock.getFrozenQuantity() < quantity) {
                log.warn("解冻库存失败：冻结库存不足, 当前冻结库存: {}, 需要解冻: {}", stock.getFrozenQuantity(), quantity);
                throw new InsufficientStockException("冻结库存不足");
            }
            
            // 记录解冻前的状态
            int beforeFrozenQuantity = stock.getFrozenQuantity();
            int afterFrozenQuantity = stock.getFrozenQuantity() - quantity;
            int beforeStockQuantity = stock.getStockQuantity();
            
            // 更新冻结库存数量
            stock.setFrozenQuantity(afterFrozenQuantity);
            
            // 更新库存状态
            updateStockStatus(stock);
            
            // 更新数据库
            boolean updated = super.updateById(stock);
            if (!updated) {
                log.error("解冻库存失败：更新数据库失败, productId: {}", productId);
                throw new UnfreezeStockException("更新数据库失败");
            }
            
            // 更新缓存
            String cacheKey = STOCK_INFO + productId;
            redisTemplate.opsForValue().set(cacheKey, stock, STOCK_CACHE_TTL, TimeUnit.SECONDS);
            
            // 发送库存变更日志消息
            stockLogProducer.sendStockCountChangeMessage(
                    productId,
                    stock.getProductName(),
                    beforeStockQuantity,   // 变更前数量
                    -quantity,             // 变更数量(负数表示减少冻结)
                    beforeStockQuantity,   // 变更后数量(总库存不变)
                    4,                     // 4表示解冻库存
                    operator               // 操作人
            );
            
            log.info("解冻库存成功, productId: {}, 解冻数量: {}, 操作人: {}", productId, quantity, operator);
            return true;
        } catch (QueryStockException | StockNotFoundException | InsufficientStockException | UnfreezeStockException e) {
            throw e;
        } catch (Exception e) {
            log.error("解冻库存异常, productId: {}, quantity: {}", productId, quantity, e);
            throw new UnfreezeStockException("解冻库存异常: " + e.getMessage());
        }
    }
}