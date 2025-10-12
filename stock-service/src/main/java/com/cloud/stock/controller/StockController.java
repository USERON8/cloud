package com.cloud.stock.controller;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.vo.stock.StockVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 库存RESTful API控制器
 * 提供库存资源的CRUD操作，参考User服务标准架构
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
@Tag(name = "库存服务", description = "库存资源的RESTful API接口")
public class StockController {

    private final StockService stockService;

    /**
     * 分页查询库存
     */
    @PostMapping("/page")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "分页查询库存", description = "根据条件分页查询库存信息")
    public Result<PageResult<StockVO>> getStocksPage(
            @Parameter(description = "分页查询条件") @RequestBody
            @Valid @NotNull(message = "分页查询条件不能为空") StockPageDTO pageDTO,
            Authentication authentication) {

        PageResult<StockVO> pageResult = stockService.pageQuery(pageDTO);
        log.info("分页查询库存成功: page={}, size={}, total={}",
                pageDTO.getCurrent(), pageDTO.getSize(), pageResult.getTotal());
        return Result.success(pageResult);
    }

    /**
     * 根据ID获取库存详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "获取库存详情", description = "根据库存ID获取详细信息")
    public Result<StockDTO> getStockById(
            @Parameter(description = "库存ID") @PathVariable
            @NotNull(message = "库存ID不能为空")
            @Positive(message = "库存ID必须为正整数") Long id,
            Authentication authentication) {

        StockDTO stock = stockService.getStockById(id);
        if (stock == null) {
            log.warn("库存记录不存在: id={}", id);
            throw new ResourceNotFoundException("Stock", String.valueOf(id));
        }
        log.info("查询库存成功: stockId={}", id);
        return Result.success("查询成功", stock);
    }

    /**
     * 根据商品ID获取库存信息
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "根据商品ID获取库存信息", description = "根据商品ID获取库存详细信息")
    public Result<StockDTO> getByProductId(
            @Parameter(description = "商品ID") @PathVariable
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long productId,
            Authentication authentication) {

        StockDTO stock = stockService.getStockByProductId(productId);
        if (stock == null) {
            log.warn("商品暂无库存信息: productId={}", productId);
            throw new ResourceNotFoundException("Stock for Product", String.valueOf(productId));
        }
        log.info("根据商品ID查询库存成功: productId={}", productId);
        return Result.success("查询成功", stock);
    }

    /**
     * 批量获取库存信息
     */
    @PostMapping("/batch/query")
    @Operation(summary = "批量获取库存信息", description = "根据商品ID列表批量获取库存信息")
    public Result<List<StockDTO>> getByProductIds(
            @Parameter(description = "商品ID列表") @RequestBody
            @NotNull(message = "商品ID列表不能为空")
            @NotEmpty(message = "商品ID列表不能为空") List<Long> productIds) {

        List<StockDTO> stocks = stockService.getStocksByProductIds(productIds);
        log.info("批量获取库存信息成功: count={}", stocks.size());
        return Result.success("查询成功", stocks);
    }

    /**
     * 创建库存记录
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "创建库存记录", description = "创建新的库存记录")
    public Result<StockDTO> createStock(
            @Parameter(description = "库存信息") @RequestBody
            @Valid @NotNull(message = "库存信息不能为空") StockDTO stockDTO) {

        StockDTO createdStock = stockService.createStock(stockDTO);
        log.info("库存创建成功: stockId={}, productId={}", createdStock.getId(), createdStock.getProductId());
        return Result.success("库存创建成功", createdStock);
    }

    /**
     * 更新库存信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "更新库存信息", description = "更新库存信息")
    public Result<Boolean> updateStock(
            @Parameter(description = "库存ID") @PathVariable Long id,
            @Parameter(description = "库存信息") @RequestBody
            @Valid @NotNull(message = "库存信息不能为空") StockDTO stockDTO,
            Authentication authentication) {

        // 确保路径参数与请求体中的ID一致
        stockDTO.setId(id);

        boolean result = stockService.updateStock(stockDTO);
        if (!result) {
            log.warn("库存更新失败: stockId={}", id);
            throw new BusinessException("库存更新失败");
        }
        log.info("库存更新成功: stockId={}", id);
        return Result.success("库存更新成功", result);
    }

    /**
     * 删除库存信息
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除库存信息", description = "根据ID删除库存信息")
    public Result<Boolean> deleteStock(
            @Parameter(description = "库存ID") @PathVariable
            @NotNull(message = "库存ID不能为空") Long id) {

        boolean result = stockService.deleteStock(id);
        if (!result) {
            log.warn("删除库存失败: stockId={}", id);
            throw new BusinessException("删除库存失败");
        }
        log.info("删除库存成功: stockId={}", id);
        return Result.success("删除成功", result);
    }

    /**
     * 批量删除库存信息
     */
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "批量删除库存信息", description = "根据ID列表批量删除库存信息")
    public Result<Boolean> deleteBatch(
            @Parameter(description = "库存ID列表") @RequestParam("ids")
            @Valid @NotNull(message = "库存ID列表不能为空") Collection<Long> ids) {

        boolean result = stockService.deleteStocksByIds(ids);
        if (!result) {
            log.warn("批量删除库存失败: count={}", ids.size());
            throw new BusinessException("批量删除库存失败");
        }
        log.info("批量删除库存成功: count={}", ids.size());
        return Result.success("批量删除成功", result);
    }

    // ==================== 业务操作接口 ====================

    /**
     * 库存入库
     */
    @PostMapping("/stock-in")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:in:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "库存入库操作获取锁失败"
    )
    @Operation(summary = "库存入库", description = "对指定商品进行入库操作")
    public Result<Boolean> stockIn(
            @Parameter(description = "商品ID") @RequestParam("productId")
            @NotNull(message = "商品ID不能为空") Long productId,
            @Parameter(description = "入库数量") @RequestParam("quantity")
            @NotNull(message = "入库数量不能为空")
            @Min(value = 1, message = "入库数量必须大于0") Integer quantity,
            @Parameter(description = "备注") @RequestParam(value = "remark", required = false) String remark,
            Authentication authentication) {

        log.info("📦 库存入库 - 商品ID: {}, 数量: {}, 备注: {}", productId, quantity, remark);
        boolean result = stockService.stockIn(productId, quantity, remark);

        if (!result) {
            log.warn("⚠️ 库存入库失败 - 商品ID: {}", productId);
            throw new BusinessException("入库失败，请检查库存信息");
        }
        log.info("✅ 库存入库成功 - 商品ID: {}, 数量: {}", productId, quantity);
        return Result.success("入库成功", result);
    }

    /**
     * 库存出库
     */
    @PostMapping("/stock-out")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:out:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "库存出库操作获取锁失败"
    )
    @Operation(summary = "库存出库", description = "对指定商品进行出库操作")
    public Result<Boolean> stockOut(
            @Parameter(description = "商品ID") @RequestParam("productId")
            @NotNull(message = "商品ID不能为空") Long productId,
            @Parameter(description = "出库数量") @RequestParam("quantity")
            @NotNull(message = "出库数量不能为空")
            @Min(value = 1, message = "出库数量必须大于0") Integer quantity,
            @Parameter(description = "订单ID") @RequestParam(value = "orderId", required = false) Long orderId,
            @Parameter(description = "订单号") @RequestParam(value = "orderNo", required = false) String orderNo,
            @Parameter(description = "备注") @RequestParam(value = "remark", required = false) String remark,
            Authentication authentication) {

        log.info("📤 库存出库 - 商品ID: {}, 数量: {}, 订单: {}/{}, 备注: {}",
                productId, quantity, orderId, orderNo, remark);
        boolean result = stockService.stockOut(productId, quantity, orderId, orderNo, remark);

        if (!result) {
            log.warn("⚠️ 库存出库失败 - 商品ID: {}, 可能库存不足", productId);
            throw new BusinessException("出库失败，库存可能不足");
        }
        log.info("✅ 库存出库成功 - 商品ID: {}, 数量: {}", productId, quantity);
        return Result.success("出库成功", result);
    }

    /**
     * 预留库存
     */
    @PostMapping("/reserve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:reserve:' + #productId",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "库存预留操作获取锁失败"
    )
    @Operation(summary = "预留库存", description = "对指定商品进行库存预留")
    public Result<Boolean> reserveStock(
            @Parameter(description = "商品ID") @RequestParam("productId")
            @NotNull(message = "商品ID不能为空") Long productId,
            @Parameter(description = "预留数量") @RequestParam("quantity")
            @NotNull(message = "预留数量不能为空")
            @Min(value = 1, message = "预留数量必须大于0") Integer quantity,
            Authentication authentication) {

        log.info("🔒 库存预留 - 商品ID: {}, 数量: {}", productId, quantity);
        boolean result = stockService.reserveStock(productId, quantity);

        if (!result) {
            log.warn("⚠️ 库存预留失败 - 商品ID: {}, 可能库存不足", productId);
            throw new BusinessException("预留失败，库存可能不足");
        }
        log.info("✅ 库存预留成功 - 商品ID: {}, 数量: {}", productId, quantity);
        return Result.success("预留成功", result);
    }

    /**
     * 释放预留库存
     */
    @PostMapping("/release")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:release:' + #productId",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "库存释放操作获取锁失败"
    )
    @Operation(summary = "释放预留库存", description = "释放指定商品的预留库存")
    public Result<Boolean> releaseReservedStock(
            @Parameter(description = "商品ID") @RequestParam("productId")
            @NotNull(message = "商品ID不能为空") Long productId,
            @Parameter(description = "释放数量") @RequestParam("quantity")
            @NotNull(message = "释放数量不能为空")
            @Min(value = 1, message = "释放数量必须大于0") Integer quantity,
            Authentication authentication) {

        log.info("🔓 库存释放 - 商品ID: {}, 数量: {}", productId, quantity);
        boolean result = stockService.releaseReservedStock(productId, quantity);

        if (!result) {
            log.warn("⚠️ 库存释放失败 - 商品ID: {}", productId);
            throw new BusinessException("释放失败，请检查预留库存");
        }
        log.info("✅ 库存释放成功 - 商品ID: {}, 数量: {}", productId, quantity);
        return Result.success("释放成功", result);
    }

    /**
     * 检查库存是否充足
     */
    @GetMapping("/check/{productId}/{quantity}")
    @Operation(summary = "检查库存是否充足", description = "检查指定商品的库存是否充足")
    public Result<Boolean> checkStockSufficient(
            @Parameter(description = "商品ID") @PathVariable
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long productId,
            @Parameter(description = "所需数量") @PathVariable
            @NotNull(message = "所需数量不能为空")
            @Positive(message = "所需数量必须为正整数") Integer quantity) {

        boolean sufficient = stockService.checkStockSufficient(productId, quantity);
        log.info("检查库存是否充足: productId={}, quantity={}, sufficient={}", productId, quantity, sufficient);
        return Result.success("检查完成", sufficient);
    }

    // ==================== 高级业务功能 ====================

    /**
     * 秒杀商品库存扣减 - 使用公平锁确保公平性
     */
    @PostMapping("/seckill/{productId}")
    @Operation(summary = "秒杀库存扣减", description = "秒杀场景下的库存扣减，使用公平锁确保公平性")
    @DistributedLock(
            key = "'seckill:stock:' + #productId",
            lockType = DistributedLock.LockType.FAIR,
            waitTime = 1,
            leaseTime = 3,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.FAIL_FAST,
            failMessage = "秒杀商品库存不足或系统繁忙"
    )
    public Result<Boolean> seckillStockOut(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "扣减数量") @RequestParam(defaultValue = "1") Integer quantity,
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "订单号") @RequestParam String orderNo) {

        log.info("⚡ 秒杀库存扣减 - 商品ID: {}, 数量: {}, 订单: {}", productId, quantity, orderNo);

        // 检查库存是否充足
        boolean sufficient = stockService.checkStockSufficient(productId, quantity);
        if (!sufficient) {
            log.warn("❌ 秒杀商品库存不足 - 商品ID: {}, 需要数量: {}", productId, quantity);
            throw new BusinessException("商品库存不足");
        }

        // 执行库存扣减
        boolean result = stockService.stockOut(productId, quantity, orderId, orderNo, "秒杀扣减");

        if (!result) {
            log.warn("❌ 秒杀库存扣减失败 - 商品ID: {}, 订单: {}", productId, orderNo);
            throw new BusinessException("秒杀失败，库存不足");
        }
        log.info("✅ 秒杀库存扣减成功 - 商品ID: {}, 订单: {}", productId, orderNo);
        return Result.success("秒杀成功", true);
    }

    // ==================== 批量管理接口 ====================

    /**
     * 批量创建库存记录
     */
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "批量创建库存记录", description = "批量创建新的库存记录")
    public Result<Integer> createStockBatch(
            @Parameter(description = "库存信息列表") @RequestBody
            @Valid @NotNull(message = "库存信息列表不能为空") List<StockDTO> stockDTOList) {

        if (stockDTOList == null || stockDTOList.isEmpty()) {
            return Result.badRequest("库存信息列表不能为空");
        }

        if (stockDTOList.size() > 100) {
            return Result.badRequest("批量创建数量不能超过100个");
        }

        log.info("批量创建库存记录, count: {}", stockDTOList.size());

        int successCount = 0;
        for (StockDTO stockDTO : stockDTOList) {
            try {
                stockService.createStock(stockDTO);
                successCount++;
            } catch (Exception e) {
                log.error("创建库存记录失败, productId: {}", stockDTO.getProductId(), e);
            }
        }

        log.info("批量创建库存记录完成, 成功: {}/{}", successCount, stockDTOList.size());
        return Result.success(String.format("批量创建库存记录成功: %d/%d", successCount, stockDTOList.size()), successCount);
    }

    /**
     * 批量更新库存信息
     */
    @PutMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "批量更新库存信息", description = "批量更新库存信息")
    public Result<Integer> updateStockBatch(
            @Parameter(description = "库存信息列表") @RequestBody
            @Valid @NotNull(message = "库存信息列表不能为空") List<StockDTO> stockDTOList,
            Authentication authentication) {

        if (stockDTOList == null || stockDTOList.isEmpty()) {
            return Result.badRequest("库存信息列表不能为空");
        }

        if (stockDTOList.size() > 100) {
            return Result.badRequest("批量更新数量不能超过100个");
        }

        log.info("批量更新库存信息, count: {}", stockDTOList.size());

        int successCount = 0;
        for (StockDTO stockDTO : stockDTOList) {
            try {
                if (stockService.updateStock(stockDTO)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("更新库存信息失败, stockId: {}", stockDTO.getId(), e);
            }
        }

        log.info("批量更新库存信息完成, 成功: {}/{}", successCount, stockDTOList.size());
        return Result.success(String.format("批量更新库存信息成功: %d/%d", successCount, stockDTOList.size()), successCount);
    }

    /**
     * 批量库存入库
     */
    @PostMapping("/stock-in/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "批量库存入库", description = "批量对多个商品进行入库操作")
    public Result<Integer> stockInBatch(
            @Parameter(description = "入库请求列表") @RequestBody
            @NotNull(message = "入库请求列表不能为空") List<StockAdjustmentRequest> requests,
            Authentication authentication) {

        if (requests == null || requests.isEmpty()) {
            return Result.badRequest("入库请求列表不能为空");
        }

        if (requests.size() > 100) {
            return Result.badRequest("批量入库数量不能超过100个");
        }

        log.info("📦 批量库存入库 - 数量: {}", requests.size());

        int successCount = 0;
        for (StockAdjustmentRequest request : requests) {
            try {
                if (stockService.stockIn(request.getProductId(), request.getQuantity(), request.getRemark())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("入库失败, productId: {}", request.getProductId(), e);
            }
        }

        log.info("✅ 批量库存入库完成, 成功: {}/{}", successCount, requests.size());
        return Result.success(String.format("批量入库成功: %d/%d", successCount, requests.size()), successCount);
    }

    /**
     * 批量库存出库
     */
    @PostMapping("/stock-out/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "批量库存出库", description = "批量对多个商品进行出库操作")
    public Result<Integer> stockOutBatch(
            @Parameter(description = "出库请求列表") @RequestBody
            @NotNull(message = "出库请求列表不能为空") List<StockAdjustmentRequest> requests,
            Authentication authentication) {

        if (requests == null || requests.isEmpty()) {
            return Result.badRequest("出库请求列表不能为空");
        }

        if (requests.size() > 100) {
            return Result.badRequest("批量出库数量不能超过100个");
        }

        log.info("📤 批量库存出库 - 数量: {}", requests.size());

        int successCount = 0;
        for (StockAdjustmentRequest request : requests) {
            try {
                if (stockService.stockOut(request.getProductId(), request.getQuantity(),
                        request.getOrderId(), request.getOrderNo(), request.getRemark())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("出库失败, productId: {}", request.getProductId(), e);
            }
        }

        log.info("✅ 批量库存出库完成, 成功: {}/{}", successCount, requests.size());
        return Result.success(String.format("批量出库成功: %d/%d", successCount, requests.size()), successCount);
    }

    /**
     * 批量预留库存
     */
    @PostMapping("/reserve/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "批量预留库存", description = "批量预留多个商品的库存")
    public Result<Integer> reserveStockBatch(
            @Parameter(description = "预留请求列表") @RequestBody
            @NotNull(message = "预留请求列表不能为空") List<StockAdjustmentRequest> requests,
            Authentication authentication) {

        if (requests == null || requests.isEmpty()) {
            return Result.badRequest("预留请求列表不能为空");
        }

        if (requests.size() > 100) {
            return Result.badRequest("批量预留数量不能超过100个");
        }

        log.info("🔒 批量库存预留 - 数量: {}", requests.size());

        int successCount = 0;
        for (StockAdjustmentRequest request : requests) {
            try {
                if (stockService.reserveStock(request.getProductId(), request.getQuantity())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("预留失败, productId: {}", request.getProductId(), e);
            }
        }

        log.info("✅ 批量库存预留完成, 成功: {}/{}", successCount, requests.size());
        return Result.success(String.format("批量预留成功: %d/%d", successCount, requests.size()), successCount);
    }

    // ==================== 内部类 ====================

    /**
     * 库存调整请求DTO
     */
    public static class StockAdjustment {
        private Long productId;
        private String type; // IN, OUT, RESERVE, RELEASE
        private Integer quantity;
        private String remark;

        // Getters and Setters
        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    /**
     * 批量调整请求
     */
    public static class StockAdjustmentRequest {
        private Long productId;
        private Integer quantity;
        private Long orderId;
        private String orderNo;
        private String remark;

        // Getters and Setters
        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
