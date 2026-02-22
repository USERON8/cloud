package com.cloud.stock.controller;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.vo.stock.StockVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.module.entity.StockCount;
import com.cloud.stock.module.entity.StockLog;
import com.cloud.stock.service.StockAlertService;
import com.cloud.stock.service.StockCountService;
import com.cloud.stock.service.StockLogService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Tag(name = "Stock Management", description = "Stock REST APIs")
public class StockController {

    private final StockService stockService;
    private final StockAlertService stockAlertService;
    private final StockCountService stockCountService;
    private final StockLogService stockLogService;

    @PostMapping("/page")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Page query stocks", description = "Query stock records by page")
    public Result<PageResult<StockVO>> getStocksPage(
            @Parameter(description = "Page query payload") @RequestBody
            @Valid @NotNull(message = "page payload is required") StockPageDTO pageDTO,
            Authentication authentication) {

        PageResult<StockVO> pageResult = stockService.pageQuery(pageDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Get stock by ID", description = "Get stock detail by stock ID")
    public Result<StockDTO> getStockById(
            @Parameter(description = "Stock ID") @PathVariable
            @NotNull(message = "stock id is required")
            @Positive(message = "stock id must be positive") Long id,
            Authentication authentication) {

        StockDTO stock = stockService.getStockById(id);
        if (stock == null) {
            throw new ResourceNotFoundException("Stock", String.valueOf(id));
        }
        return Result.success("query successful", stock);
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get stock by product ID", description = "Get stock detail by product ID")
    public Result<StockDTO> getByProductId(
            @Parameter(description = "Product ID") @PathVariable
            @NotNull(message = "product id is required")
            @Positive(message = "product id must be positive") Long productId,
            Authentication authentication) {

        StockDTO stock = stockService.getStockByProductId(productId);
        if (stock == null) {
            throw new ResourceNotFoundException("Stock for Product", String.valueOf(productId));
        }
        return Result.success("query successful", stock);
    }

    @PostMapping("/batch/query")
    @Operation(summary = "Batch query by product IDs", description = "Batch query stock records by product IDs")
    public Result<List<StockDTO>> getByProductIds(
            @Parameter(description = "Product ID list") @RequestBody
            @NotNull(message = "product ids are required")
            @NotEmpty(message = "product ids cannot be empty") List<Long> productIds) {

        List<StockDTO> stocks = stockService.getStocksByProductIds(productIds);
        return Result.success("query successful", stocks);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Create stock", description = "Create one stock record")
    public Result<StockDTO> createStock(
            @Parameter(description = "Stock payload") @RequestBody
            @Valid @NotNull(message = "stock payload is required") StockDTO stockDTO) {

        StockDTO createdStock = stockService.createStock(stockDTO);
        return Result.success("stock created", createdStock);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Update stock", description = "Update one stock record")
    public Result<Boolean> updateStock(
            @Parameter(description = "Stock ID") @PathVariable Long id,
            @Parameter(description = "Stock payload") @RequestBody
            @Valid @NotNull(message = "stock payload is required") StockDTO stockDTO,
            Authentication authentication) {

        stockDTO.setId(id);
        boolean result = stockService.updateStock(stockDTO);
        if (!result) {
            throw new BusinessException("update stock failed");
        }
        return Result.success("stock updated", true);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete stock", description = "Delete stock by stock ID")
    public Result<Boolean> deleteStock(
            @Parameter(description = "Stock ID") @PathVariable
            @NotNull(message = "stock id is required") Long id) {

        boolean result = stockService.deleteStock(id);
        if (!result) {
            throw new BusinessException("delete stock failed");
        }
        return Result.success("stock deleted", true);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Batch delete stocks", description = "Delete stocks by ID list")
    public Result<Boolean> deleteBatch(
            @Parameter(description = "Stock ID list") @RequestParam("ids")
            @Valid @NotNull(message = "stock ids are required") Collection<Long> ids) {

        boolean result = stockService.deleteStocksByIds(ids);
        if (!result) {
            throw new BusinessException("batch delete stocks failed");
        }
        return Result.success("stocks deleted", true);
    }

    @PostMapping("/stock-in")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:in:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire stock in lock failed"
    )
    @Operation(summary = "Stock in", description = "Increase stock quantity")
    public Result<Boolean> stockIn(
            @Parameter(description = "Product ID") @RequestParam("productId")
            @NotNull(message = "product id is required") Long productId,
            @Parameter(description = "Quantity") @RequestParam("quantity")
            @NotNull(message = "quantity is required")
            @Min(value = 1, message = "quantity must be at least 1") Integer quantity,
            @Parameter(description = "Remark") @RequestParam(value = "remark", required = false) String remark,
            Authentication authentication) {

        boolean result = stockService.stockIn(productId, quantity, remark);
        if (!result) {
            throw new BusinessException("stock in failed");
        }
        return Result.success("stock in successful", true);
    }

    @PostMapping("/stock-out")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:out:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire stock out lock failed"
    )
    @Operation(summary = "Stock out", description = "Decrease stock quantity")
    public Result<Boolean> stockOut(
            @Parameter(description = "Product ID") @RequestParam("productId")
            @NotNull(message = "product id is required") Long productId,
            @Parameter(description = "Quantity") @RequestParam("quantity")
            @NotNull(message = "quantity is required")
            @Min(value = 1, message = "quantity must be at least 1") Integer quantity,
            @Parameter(description = "Order ID") @RequestParam(value = "orderId", required = false) Long orderId,
            @Parameter(description = "Order number") @RequestParam(value = "orderNo", required = false) String orderNo,
            @Parameter(description = "Remark") @RequestParam(value = "remark", required = false) String remark,
            Authentication authentication) {

        boolean result = stockService.stockOut(productId, quantity, orderId, orderNo, remark);
        if (!result) {
            throw new BusinessException("stock out failed");
        }
        return Result.success("stock out successful", true);
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:reserve:' + #productId",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire reserve stock lock failed"
    )
    @Operation(summary = "Reserve stock", description = "Reserve stock quantity")
    public Result<Boolean> reserveStock(
            @Parameter(description = "Product ID") @RequestParam("productId")
            @NotNull(message = "product id is required") Long productId,
            @Parameter(description = "Reserve quantity") @RequestParam("quantity")
            @NotNull(message = "quantity is required")
            @Min(value = 1, message = "quantity must be at least 1") Integer quantity,
            Authentication authentication) {

        boolean result = stockService.reserveStock(productId, quantity);
        if (!result) {
            throw new BusinessException("reserve stock failed");
        }
        return Result.success("stock reserved", true);
    }

    @PostMapping("/release")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:release:' + #productId",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "Acquire release stock lock failed"
    )
    @Operation(summary = "Release reserved stock", description = "Release reserved stock quantity")
    public Result<Boolean> releaseReservedStock(
            @Parameter(description = "Product ID") @RequestParam("productId")
            @NotNull(message = "product id is required") Long productId,
            @Parameter(description = "Release quantity") @RequestParam("quantity")
            @NotNull(message = "quantity is required")
            @Min(value = 1, message = "quantity must be at least 1") Integer quantity,
            Authentication authentication) {

        boolean result = stockService.releaseReservedStock(productId, quantity);
        if (!result) {
            throw new BusinessException("release reserved stock failed");
        }
        return Result.success("reserved stock released", true);
    }

    @GetMapping("/check/{productId}/{quantity}")
    @Operation(summary = "Check stock sufficient", description = "Check whether stock is sufficient")
    public Result<Boolean> checkStockSufficient(
            @Parameter(description = "Product ID") @PathVariable
            @NotNull(message = "product id is required")
            @Positive(message = "product id must be positive") Long productId,
            @Parameter(description = "Quantity") @PathVariable
            @NotNull(message = "quantity is required")
            @Positive(message = "quantity must be positive") Integer quantity) {

        boolean sufficient = stockService.checkStockSufficient(productId, quantity);
        return Result.success("query successful", sufficient);
    }

    @PostMapping("/seckill/{productId}")
    @Operation(summary = "Seckill stock out", description = "High-concurrency stock out for flash sale")
    @DistributedLock(
            key = "'seckill:stock:' + #productId",
            lockType = DistributedLock.LockType.FAIR,
            waitTime = 1,
            leaseTime = 3,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.FAIL_FAST,
            failMessage = "Acquire seckill stock lock failed"
    )
    public Result<Boolean> seckillStockOut(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Parameter(description = "Quantity") @RequestParam(defaultValue = "1") Integer quantity,
            @Parameter(description = "Order ID") @RequestParam Long orderId,
            @Parameter(description = "Order number") @RequestParam String orderNo) {

        boolean sufficient = stockService.checkStockSufficient(productId, quantity);
        if (!sufficient) {
            throw new BusinessException("stock insufficient");
        }

        boolean result = stockService.stockOut(productId, quantity, orderId, orderNo, "seckill stock out");
        if (!result) {
            throw new BusinessException("seckill stock out failed");
        }
        return Result.success("seckill stock out successful", true);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Batch create stock", description = "Batch create stock records")
    public Result<Integer> createStockBatch(
            @Parameter(description = "Stock payload list") @RequestBody
            @Valid @NotNull(message = "stock payload list is required") List<StockDTO> stockDTOList) {

        if (stockDTOList.isEmpty()) {
            return Result.badRequest("stock payload list cannot be empty");
        }
        if (stockDTOList.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }

        Integer successCount = stockService.batchCreateStocks(stockDTOList);
        return Result.success(String.format("batch create stock completed: %d/%d", successCount, stockDTOList.size()), successCount);
    }

    @PutMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Batch update stock", description = "Batch update stock records")
    public Result<Integer> updateStockBatch(
            @Parameter(description = "Stock payload list") @RequestBody
            @Valid @NotNull(message = "stock payload list is required") List<StockDTO> stockDTOList,
            Authentication authentication) {

        if (stockDTOList.isEmpty()) {
            return Result.badRequest("stock payload list cannot be empty");
        }
        if (stockDTOList.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }

        Integer successCount = stockService.batchUpdateStocks(stockDTOList);
        return Result.success(String.format("batch update stock completed: %d/%d", successCount, stockDTOList.size()), successCount);
    }

    @PostMapping("/stock-in/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Batch stock in", description = "Batch increase stock")
    public Result<Integer> stockInBatch(
            @Parameter(description = "Batch stock-in requests") @RequestBody
            @NotNull(message = "requests are required") List<StockService.StockAdjustmentRequest> requests,
            Authentication authentication) {

        if (requests.isEmpty()) {
            return Result.badRequest("requests cannot be empty");
        }
        if (requests.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }

        Integer successCount = stockService.batchStockIn(requests);
        return Result.success(String.format("batch stock-in completed: %d/%d", successCount, requests.size()), successCount);
    }

    @PostMapping("/stock-out/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Batch stock out", description = "Batch decrease stock")
    public Result<Integer> stockOutBatch(
            @Parameter(description = "Batch stock-out requests") @RequestBody
            @NotNull(message = "requests are required") List<StockService.StockAdjustmentRequest> requests,
            Authentication authentication) {

        if (requests.isEmpty()) {
            return Result.badRequest("requests cannot be empty");
        }
        if (requests.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }

        Integer successCount = stockService.batchStockOut(requests);
        return Result.success(String.format("batch stock-out completed: %d/%d", successCount, requests.size()), successCount);
    }

    @PostMapping("/reserve/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Batch reserve stock", description = "Batch reserve stock quantity")
    public Result<Integer> reserveStockBatch(
            @Parameter(description = "Batch reserve requests") @RequestBody
            @NotNull(message = "requests are required") List<StockService.StockAdjustmentRequest> requests,
            Authentication authentication) {

        if (requests.isEmpty()) {
            return Result.badRequest("requests cannot be empty");
        }
        if (requests.size() > 100) {
            return Result.badRequest("batch size cannot exceed 100");
        }

        Integer successCount = stockService.batchReserveStock(requests);
        return Result.success(String.format("batch reserve completed: %d/%d", successCount, requests.size()), successCount);
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Get low stock alerts", description = "Get all low stock products")
    public Result<List<Stock>> getLowStockAlerts(Authentication authentication) {
        List<Stock> lowStockProducts = stockAlertService.getLowStockProducts();
        return Result.success("query successful", lowStockProducts);
    }

    @GetMapping("/alerts/threshold/{threshold}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Get low stock alerts by threshold", description = "Get low stock products by threshold")
    public Result<List<Stock>> getLowStockByThreshold(
            @Parameter(description = "Threshold") @PathVariable
            @NotNull(message = "threshold is required")
            @Min(value = 0, message = "threshold must be >= 0") Integer threshold,
            Authentication authentication) {

        List<Stock> lowStockProducts = stockAlertService.getLowStockProductsByThreshold(threshold);
        return Result.success("query successful", lowStockProducts);
    }

    @PutMapping("/{productId}/threshold")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Update low stock threshold", description = "Update threshold for one product")
    public Result<Boolean> updateLowStockThreshold(
            @Parameter(description = "Product ID") @PathVariable
            @NotNull(message = "product id is required") Long productId,
            @Parameter(description = "Threshold") @RequestParam("threshold")
            @NotNull(message = "threshold is required")
            @Min(value = 0, message = "threshold must be >= 0") Integer threshold,
            Authentication authentication) {

        boolean result = stockAlertService.updateLowStockThreshold(productId, threshold);
        return Result.success("threshold updated", result);
    }

    @PutMapping("/threshold/batch")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Batch update thresholds", description = "Batch update low stock threshold")
    public Result<Integer> batchUpdateLowStockThreshold(
            @Parameter(description = "Product ID list") @RequestParam("productIds")
            @NotNull(message = "product ids are required") List<Long> productIds,
            @Parameter(description = "Threshold") @RequestParam("threshold")
            @NotNull(message = "threshold is required")
            @Min(value = 0, message = "threshold must be >= 0") Integer threshold) {

        int count = stockAlertService.batchUpdateLowStockThreshold(productIds, threshold);
        return Result.success("batch threshold update successful", count);
    }

    @PostMapping("/count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Create stock count", description = "Create stock counting record")
    public Result<Long> createStockCount(
            @Parameter(description = "Product ID") @RequestParam("productId")
            @NotNull(message = "product id is required") Long productId,
            @Parameter(description = "Actual quantity") @RequestParam("actualQuantity")
            @NotNull(message = "actual quantity is required")
            @Min(value = 0, message = "actual quantity must be >= 0") Integer actualQuantity,
            @Parameter(description = "Remark") @RequestParam(value = "remark", required = false) String remark,
            Authentication authentication) {

        Long operatorId = extractUserId(authentication);
        String operatorName = authentication != null ? authentication.getName() : "system";

        Long countId = stockCountService.createStockCount(
                productId,
                actualQuantity,
                operatorId,
                operatorName,
                remark
        );
        return Result.success("stock count created", countId);
    }

    @PutMapping("/count/{countId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Confirm stock count", description = "Confirm one stock counting record")
    public Result<Boolean> confirmStockCount(
            @Parameter(description = "Count ID") @PathVariable
            @NotNull(message = "count id is required") Long countId,
            Authentication authentication) {

        Long confirmUserId = extractUserId(authentication);
        String confirmUserName = authentication != null ? authentication.getName() : "system";

        boolean result = stockCountService.confirmStockCount(countId, confirmUserId, confirmUserName);
        return Result.success("stock count confirmed", result);
    }

    @DeleteMapping("/count/{countId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Cancel stock count", description = "Cancel one stock counting record")
    public Result<Boolean> cancelStockCount(
            @Parameter(description = "Count ID") @PathVariable
            @NotNull(message = "count id is required") Long countId,
            Authentication authentication) {

        boolean result = stockCountService.cancelStockCount(countId);
        return Result.success("stock count cancelled", result);
    }

    @GetMapping("/count/{countId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Get stock count by ID", description = "Get stock counting detail by ID")
    public Result<StockCount> getStockCountById(
            @Parameter(description = "Count ID") @PathVariable
            @NotNull(message = "count id is required") Long countId,
            Authentication authentication) {

        StockCount stockCount = stockCountService.getStockCountById(countId);
        if (stockCount == null) {
            throw new ResourceNotFoundException("StockCount", String.valueOf(countId));
        }
        return Result.success("query successful", stockCount);
    }

    @GetMapping("/count/product/{productId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Get stock counts by product", description = "Get stock count history by product")
    public Result<List<StockCount>> getStockCountsByProductId(
            @Parameter(description = "Product ID") @PathVariable
            @NotNull(message = "product id is required") Long productId,
            @Parameter(description = "Start time") @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            Authentication authentication) {

        List<StockCount> counts = stockCountService.getStockCountsByProductId(productId, startTime, endTime);
        return Result.success("query successful", counts);
    }

    @GetMapping("/count/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get stock counts by status", description = "Get stock count records by status")
    public Result<List<StockCount>> getStockCountsByStatus(
            @Parameter(description = "Count status") @PathVariable String status,
            @Parameter(description = "Start time") @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        List<StockCount> counts = stockCountService.getStockCountsByStatus(status, startTime, endTime);
        return Result.success("query successful", counts);
    }

    @GetMapping("/count/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending stock count number", description = "Get number of pending stock count records")
    public Result<Integer> countPendingRecords() {
        int count = stockCountService.countPendingRecords();
        return Result.success("query successful", count);
    }

    @GetMapping("/logs/product/{productId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Get stock logs by product", description = "Get stock operation logs by product")
    public Result<List<StockLog>> getLogsByProductId(
            @Parameter(description = "Product ID") @PathVariable
            @NotNull(message = "product id is required") Long productId,
            @Parameter(description = "Start time") @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            Authentication authentication) {

        List<StockLog> logs = stockLogService.getLogsByProductId(productId, startTime, endTime);
        return Result.success("query successful", logs);
    }

    @GetMapping("/logs/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "Get stock logs by order", description = "Get stock operation logs by order")
    public Result<List<StockLog>> getLogsByOrderId(
            @Parameter(description = "Order ID") @PathVariable
            @NotNull(message = "order id is required") Long orderId,
            Authentication authentication) {

        List<StockLog> logs = stockLogService.getLogsByOrderId(orderId);
        return Result.success("query successful", logs);
    }

    @GetMapping("/logs/type/{operationType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get stock logs by operation type", description = "Get stock operation logs by type")
    public Result<List<StockLog>> getLogsByOperationType(
            @Parameter(description = "Operation type") @PathVariable String operationType,
            @Parameter(description = "Start time") @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        List<StockLog> logs = stockLogService.getLogsByOperationType(operationType, startTime, endTime);
        return Result.success("query successful", logs);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return 0L;
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
