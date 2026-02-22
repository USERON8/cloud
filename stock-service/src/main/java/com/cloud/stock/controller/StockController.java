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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;






@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Tag(name = "搴撳瓨鏈嶅姟", description = "搴撳瓨璧勬簮鐨凴ESTful API鎺ュ彛")
public class StockController {

    private final StockService stockService;
    private final StockAlertService stockAlertService;
    private final StockCountService stockCountService;
    private final StockLogService stockLogService;

    


    @PostMapping("/page")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鍒嗛〉鏌ヨ搴撳瓨", description = "鏍规嵁鏉′欢鍒嗛〉鏌ヨ搴撳瓨淇℃伅")
    public Result<PageResult<StockVO>> getStocksPage(
            @Parameter(description = "鍒嗛〉鏌ヨ鏉′欢") @RequestBody
            @Valid @NotNull(message = "鍒嗛〉鏌ヨ鏉′欢涓嶈兘涓虹┖") StockPageDTO pageDTO,
            Authentication authentication) {

        PageResult<StockVO> pageResult = stockService.pageQuery(pageDTO);
        

        return Result.success(pageResult);
    }

    


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鑾峰彇搴撳瓨璇︽儏", description = "鏍规嵁搴撳瓨ID鑾峰彇璇︾粏淇℃伅")
    public Result<StockDTO> getStockById(
            @Parameter(description = "搴撳瓨ID") @PathVariable
            @NotNull(message = "搴撳瓨ID涓嶈兘涓虹┖")
            @Positive(message = "搴撳瓨ID蹇呴』涓烘鏁存暟") Long id,
            Authentication authentication) {

        StockDTO stock = stockService.getStockById(id);
        if (stock == null) {
            log.warn("搴撳瓨璁板綍涓嶅瓨鍦? id={}", id);
            throw new ResourceNotFoundException("Stock", String.valueOf(id));
        }
        
        return Result.success("鏌ヨ鎴愬姛", stock);
    }

    


    @GetMapping("/product/{productId}")
    @Operation(summary = "鏍规嵁鍟嗗搧ID鑾峰彇搴撳瓨淇℃伅", description = "鏍规嵁鍟嗗搧ID鑾峰彇搴撳瓨璇︾粏淇℃伅")
    public Result<StockDTO> getByProductId(
            @Parameter(description = "鍟嗗搧ID") @PathVariable
            @NotNull(message = "鍟嗗搧ID涓嶈兘涓虹┖")
            @Positive(message = "鍟嗗搧ID蹇呴』涓烘鏁存暟") Long productId,
            Authentication authentication) {

        StockDTO stock = stockService.getStockByProductId(productId);
        if (stock == null) {
            log.warn("鍟嗗搧鏆傛棤搴撳瓨淇℃伅: productId={}", productId);
            throw new ResourceNotFoundException("Stock for Product", String.valueOf(productId));
        }
        
        return Result.success("鏌ヨ鎴愬姛", stock);
    }

    


    @PostMapping("/batch/query")
    @Operation(summary = "鎵归噺鑾峰彇搴撳瓨淇℃伅", description = "鏍规嵁鍟嗗搧ID鍒楄〃鎵归噺鑾峰彇搴撳瓨淇℃伅")
    public Result<List<StockDTO>> getByProductIds(
            @Parameter(description = "鍟嗗搧ID鍒楄〃") @RequestBody
            @NotNull(message = "鍟嗗搧ID鍒楄〃涓嶈兘涓虹┖")
            @NotEmpty(message = "鍟嗗搧ID鍒楄〃涓嶈兘涓虹┖") List<Long> productIds) {

        List<StockDTO> stocks = stockService.getStocksByProductIds(productIds);
        
        return Result.success("鏌ヨ鎴愬姛", stocks);
    }

    


    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "鍒涘缓搴撳瓨璁板綍", description = "鍒涘缓鏂扮殑搴撳瓨璁板綍")
    public Result<StockDTO> createStock(
            @Parameter(description = "搴撳瓨淇℃伅") @RequestBody
            @Valid @NotNull(message = "搴撳瓨淇℃伅涓嶈兘涓虹┖") StockDTO stockDTO) {

        StockDTO createdStock = stockService.createStock(stockDTO);
        
        return Result.success("搴撳瓨鍒涘缓鎴愬姛", createdStock);
    }

    


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鏇存柊搴撳瓨淇℃伅", description = "鏇存柊搴撳瓨淇℃伅")
    public Result<Boolean> updateStock(
            @Parameter(description = "搴撳瓨ID") @PathVariable Long id,
            @Parameter(description = "搴撳瓨淇℃伅") @RequestBody
            @Valid @NotNull(message = "搴撳瓨淇℃伅涓嶈兘涓虹┖") StockDTO stockDTO,
            Authentication authentication) {

        

        boolean result = stockService.updateStock(stockDTO);
        if (!result) {
            log.warn("搴撳瓨鏇存柊澶辫触: stockId={}", id);
            throw new BusinessException("搴撳瓨鏇存柊澶辫触");
        }
        
        return Result.success("搴撳瓨鏇存柊鎴愬姛", result);
    }

    


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "鍒犻櫎搴撳瓨淇℃伅", description = "鏍规嵁ID鍒犻櫎搴撳瓨淇℃伅")
    public Result<Boolean> deleteStock(
            @Parameter(description = "搴撳瓨ID") @PathVariable
            @NotNull(message = "搴撳瓨ID涓嶈兘涓虹┖") Long id) {

        boolean result = stockService.deleteStock(id);
        if (!result) {
            log.warn("鍒犻櫎搴撳瓨澶辫触: stockId={}", id);
            throw new BusinessException("鍒犻櫎搴撳瓨澶辫触");
        }
        
        return Result.success("鍒犻櫎鎴愬姛", result);
    }

    


    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "鎵归噺鍒犻櫎搴撳瓨淇℃伅", description = "鏍规嵁ID鍒楄〃鎵归噺鍒犻櫎搴撳瓨淇℃伅")
    public Result<Boolean> deleteBatch(
            @Parameter(description = "搴撳瓨ID鍒楄〃") @RequestParam("ids")
            @Valid @NotNull(message = "搴撳瓨ID鍒楄〃涓嶈兘涓虹┖") Collection<Long> ids) {

        boolean result = stockService.deleteStocksByIds(ids);
        if (!result) {
            log.warn("鎵归噺鍒犻櫎搴撳瓨澶辫触: count={}", ids.size());
            throw new BusinessException("鎵归噺鍒犻櫎搴撳瓨澶辫触");
        }
        
        return Result.success("鎵归噺鍒犻櫎鎴愬姛", result);
    }

    

    


    @PostMapping("/stock-in")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:in:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "搴撳瓨鍏ュ簱鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Operation(summary = "搴撳瓨鍏ュ簱", description = "瀵规寚瀹氬晢鍝佽繘琛屽叆搴撴搷浣?)
    public Result<Boolean> stockIn(
            @Parameter(description = "鍟嗗搧ID") @RequestParam("productId")
            @NotNull(message = "鍟嗗搧ID涓嶈兘涓虹┖") Long productId,
            @Parameter(description = "鍏ュ簱鏁伴噺") @RequestParam("quantity")
            @NotNull(message = "鍏ュ簱鏁伴噺涓嶈兘涓虹┖")
            @Min(value = 1, message = "鍏ュ簱鏁伴噺蹇呴』澶т簬0") Integer quantity,
            @Parameter(description = "澶囨敞") @RequestParam(value = "remark", required = false) String remark,
            Authentication authentication) {

        
        boolean result = stockService.stockIn(productId, quantity, remark);

        if (!result) {
            log.warn("鈿狅笍 搴撳瓨鍏ュ簱澶辫触 - 鍟嗗搧ID: {}", productId);
            throw new BusinessException("鍏ュ簱澶辫触锛岃妫€鏌ュ簱瀛樹俊鎭?);
        }
        
        return Result.success("鍏ュ簱鎴愬姛", result);
    }

    


    @PostMapping("/stock-out")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:out:' + #productId",
            waitTime = 5,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "搴撳瓨鍑哄簱鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Operation(summary = "搴撳瓨鍑哄簱", description = "瀵规寚瀹氬晢鍝佽繘琛屽嚭搴撴搷浣?)
    public Result<Boolean> stockOut(
            @Parameter(description = "鍟嗗搧ID") @RequestParam("productId")
            @NotNull(message = "鍟嗗搧ID涓嶈兘涓虹┖") Long productId,
            @Parameter(description = "鍑哄簱鏁伴噺") @RequestParam("quantity")
            @NotNull(message = "鍑哄簱鏁伴噺涓嶈兘涓虹┖")
            @Min(value = 1, message = "鍑哄簱鏁伴噺蹇呴』澶т簬0") Integer quantity,
            @Parameter(description = "璁㈠崟ID") @RequestParam(value = "orderId", required = false) Long orderId,
            @Parameter(description = "璁㈠崟鍙?) @RequestParam(value = "orderNo", required = false) String orderNo,
            @Parameter(description = "澶囨敞") @RequestParam(value = "remark", required = false) String remark,
            Authentication authentication) {

        

        boolean result = stockService.stockOut(productId, quantity, orderId, orderNo, remark);

        if (!result) {
            log.warn("鈿狅笍 搴撳瓨鍑哄簱澶辫触 - 鍟嗗搧ID: {}, 鍙兘搴撳瓨涓嶈冻", productId);
            throw new BusinessException("鍑哄簱澶辫触锛屽簱瀛樺彲鑳戒笉瓒?);
        }
        
        return Result.success("鍑哄簱鎴愬姛", result);
    }

    


    @PostMapping("/reserve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:reserve:' + #productId",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "搴撳瓨棰勭暀鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Operation(summary = "棰勭暀搴撳瓨", description = "瀵规寚瀹氬晢鍝佽繘琛屽簱瀛橀鐣?)
    public Result<Boolean> reserveStock(
            @Parameter(description = "鍟嗗搧ID") @RequestParam("productId")
            @NotNull(message = "鍟嗗搧ID涓嶈兘涓虹┖") Long productId,
            @Parameter(description = "棰勭暀鏁伴噺") @RequestParam("quantity")
            @NotNull(message = "棰勭暀鏁伴噺涓嶈兘涓虹┖")
            @Min(value = 1, message = "棰勭暀鏁伴噺蹇呴』澶т簬0") Integer quantity,
            Authentication authentication) {

        
        boolean result = stockService.reserveStock(productId, quantity);

        if (!result) {
            log.warn("鈿狅笍 搴撳瓨棰勭暀澶辫触 - 鍟嗗搧ID: {}, 鍙兘搴撳瓨涓嶈冻", productId);
            throw new BusinessException("棰勭暀澶辫触锛屽簱瀛樺彲鑳戒笉瓒?);
        }
        
        return Result.success("棰勭暀鎴愬姛", result);
    }

    


    @PostMapping("/release")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @DistributedLock(
            key = "'stock:release:' + #productId",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "搴撳瓨閲婃斁鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Operation(summary = "閲婃斁棰勭暀搴撳瓨", description = "閲婃斁鎸囧畾鍟嗗搧鐨勯鐣欏簱瀛?)
    public Result<Boolean> releaseReservedStock(
            @Parameter(description = "鍟嗗搧ID") @RequestParam("productId")
            @NotNull(message = "鍟嗗搧ID涓嶈兘涓虹┖") Long productId,
            @Parameter(description = "閲婃斁鏁伴噺") @RequestParam("quantity")
            @NotNull(message = "閲婃斁鏁伴噺涓嶈兘涓虹┖")
            @Min(value = 1, message = "閲婃斁鏁伴噺蹇呴』澶т簬0") Integer quantity,
            Authentication authentication) {

        
        boolean result = stockService.releaseReservedStock(productId, quantity);

        if (!result) {
            log.warn("鈿狅笍 搴撳瓨閲婃斁澶辫触 - 鍟嗗搧ID: {}", productId);
            throw new BusinessException("閲婃斁澶辫触锛岃妫€鏌ラ鐣欏簱瀛?);
        }
        
        return Result.success("閲婃斁鎴愬姛", result);
    }

    

    @GetMapping("/check/{productId}/{quantity}")
    @Operation(summary = "妫€鏌ュ簱瀛樻槸鍚﹀厖瓒?, description = "妫€鏌ユ寚瀹氬晢鍝佺殑搴撳瓨鏄惁鍏呰冻")
    public Result<Boolean> checkStockSufficient(
            @Parameter(description = "鍟嗗搧ID") @PathVariable
            @NotNull(message = "鍟嗗搧ID涓嶈兘涓虹┖")
            @Positive(message = "鍟嗗搧ID蹇呴』涓烘鏁存暟") Long productId,
            @Parameter(description = "鎵€闇€鏁伴噺") @PathVariable
            @NotNull(message = "鎵€闇€鏁伴噺涓嶈兘涓虹┖")
            @Positive(message = "鎵€闇€鏁伴噺蹇呴』涓烘鏁存暟") Integer quantity) {

        boolean sufficient = stockService.checkStockSufficient(productId, quantity);
        
        return Result.success("妫€鏌ュ畬鎴?, sufficient);
    }

    

    

    @PostMapping("/seckill/{productId}")
    @Operation(summary = "绉掓潃搴撳瓨鎵ｅ噺", description = "绉掓潃鍦烘櫙涓嬬殑搴撳瓨鎵ｅ噺锛屼娇鐢ㄥ叕骞抽攣纭繚鍏钩鎬?)
    @DistributedLock(
            key = "'seckill:stock:' + #productId",
            lockType = DistributedLock.LockType.FAIR,
            waitTime = 1,
            leaseTime = 3,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.FAIL_FAST,
            failMessage = "绉掓潃鍟嗗搧搴撳瓨涓嶈冻鎴栫郴缁熺箒蹇?
    )
    public Result<Boolean> seckillStockOut(
            @Parameter(description = "鍟嗗搧ID") @PathVariable Long productId,
            @Parameter(description = "鎵ｅ噺鏁伴噺") @RequestParam(defaultValue = "1") Integer quantity,
            @Parameter(description = "璁㈠崟ID") @RequestParam Long orderId,
            @Parameter(description = "璁㈠崟鍙?) @RequestParam String orderNo) {

        

        
        if (!sufficient) {
            log.warn("鉂?绉掓潃鍟嗗搧搴撳瓨涓嶈冻 - 鍟嗗搧ID: {}, 闇€瑕佹暟閲? {}", productId, quantity);
            throw new BusinessException("鍟嗗搧搴撳瓨涓嶈冻");
        }

        
        boolean result = stockService.stockOut(productId, quantity, orderId, orderNo, "绉掓潃鎵ｅ噺");

        if (!result) {
            log.warn("鉂?绉掓潃搴撳瓨鎵ｅ噺澶辫触 - 鍟嗗搧ID: {}, 璁㈠崟: {}", productId, orderNo);
            throw new BusinessException("绉掓潃澶辫触锛屽簱瀛樹笉瓒?);
        }
        
        return Result.success("绉掓潃鎴愬姛", true);
    }

    

    


    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "鎵归噺鍒涘缓搴撳瓨璁板綍", description = "鎵归噺鍒涘缓鏂扮殑搴撳瓨璁板綍")
    public Result<Integer> createStockBatch(
            @Parameter(description = "搴撳瓨淇℃伅鍒楄〃") @RequestBody
            @Valid @NotNull(message = "搴撳瓨淇℃伅鍒楄〃涓嶈兘涓虹┖") List<StockDTO> stockDTOList) {

        if (stockDTOList == null || stockDTOList.isEmpty()) {
            return Result.badRequest("搴撳瓨淇℃伅鍒楄〃涓嶈兘涓虹┖");
        }

        if (stockDTOList.size() > 100) {
            return Result.badRequest("鎵归噺鍒涘缓鏁伴噺涓嶈兘瓒呰繃100涓?);
        }

        

        
        Integer successCount = stockService.batchCreateStocks(stockDTOList);

        
        return Result.success(String.format("鎵归噺鍒涘缓搴撳瓨璁板綍鎴愬姛: %d/%d", successCount, stockDTOList.size()), successCount);
    }

    


    @PutMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鎵归噺鏇存柊搴撳瓨淇℃伅", description = "鎵归噺鏇存柊搴撳瓨淇℃伅")
    public Result<Integer> updateStockBatch(
            @Parameter(description = "搴撳瓨淇℃伅鍒楄〃") @RequestBody
            @Valid @NotNull(message = "搴撳瓨淇℃伅鍒楄〃涓嶈兘涓虹┖") List<StockDTO> stockDTOList,
            Authentication authentication) {

        if (stockDTOList == null || stockDTOList.isEmpty()) {
            return Result.badRequest("搴撳瓨淇℃伅鍒楄〃涓嶈兘涓虹┖");
        }

        if (stockDTOList.size() > 100) {
            return Result.badRequest("鎵归噺鏇存柊鏁伴噺涓嶈兘瓒呰繃100涓?);
        }

        

        
        Integer successCount = stockService.batchUpdateStocks(stockDTOList);

        
        return Result.success(String.format("鎵归噺鏇存柊搴撳瓨淇℃伅鎴愬姛: %d/%d", successCount, stockDTOList.size()), successCount);
    }

    


    @PostMapping("/stock-in/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鎵归噺搴撳瓨鍏ュ簱", description = "鎵归噺瀵瑰涓晢鍝佽繘琛屽叆搴撴搷浣?)
    public Result<Integer> stockInBatch(
            @Parameter(description = "鍏ュ簱璇锋眰鍒楄〃") @RequestBody
            @NotNull(message = "鍏ュ簱璇锋眰鍒楄〃涓嶈兘涓虹┖") List<StockService.StockAdjustmentRequest> requests,
            Authentication authentication) {

        if (requests == null || requests.isEmpty()) {
            return Result.badRequest("鍏ュ簱璇锋眰鍒楄〃涓嶈兘涓虹┖");
        }

        if (requests.size() > 100) {
            return Result.badRequest("鎵归噺鍏ュ簱鏁伴噺涓嶈兘瓒呰繃100涓?);
        }

        

        
        Integer successCount = stockService.batchStockIn(requests);

        
        return Result.success(String.format("鎵归噺鍏ュ簱鎴愬姛: %d/%d", successCount, requests.size()), successCount);
    }

    


    @PostMapping("/stock-out/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鎵归噺搴撳瓨鍑哄簱", description = "鎵归噺瀵瑰涓晢鍝佽繘琛屽嚭搴撴搷浣?)
    public Result<Integer> stockOutBatch(
            @Parameter(description = "鍑哄簱璇锋眰鍒楄〃") @RequestBody
            @NotNull(message = "鍑哄簱璇锋眰鍒楄〃涓嶈兘涓虹┖") List<StockService.StockAdjustmentRequest> requests,
            Authentication authentication) {

        if (requests == null || requests.isEmpty()) {
            return Result.badRequest("鍑哄簱璇锋眰鍒楄〃涓嶈兘涓虹┖");
        }

        if (requests.size() > 100) {
            return Result.badRequest("鎵归噺鍑哄簱鏁伴噺涓嶈兘瓒呰繃100涓?);
        }

        

        
        Integer successCount = stockService.batchStockOut(requests);

        
        return Result.success(String.format("鎵归噺鍑哄簱鎴愬姛: %d/%d", successCount, requests.size()), successCount);
    }

    


    @PostMapping("/reserve/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鎵归噺棰勭暀搴撳瓨", description = "鎵归噺棰勭暀澶氫釜鍟嗗搧鐨勫簱瀛?)
    public Result<Integer> reserveStockBatch(
            @Parameter(description = "棰勭暀璇锋眰鍒楄〃") @RequestBody
            @NotNull(message = "棰勭暀璇锋眰鍒楄〃涓嶈兘涓虹┖") List<StockService.StockAdjustmentRequest> requests,
            Authentication authentication) {

        if (requests == null || requests.isEmpty()) {
            return Result.badRequest("棰勭暀璇锋眰鍒楄〃涓嶈兘涓虹┖");
        }

        if (requests.size() > 100) {
            return Result.badRequest("鎵归噺棰勭暀鏁伴噺涓嶈兘瓒呰繃100涓?);
        }

        

        
        Integer successCount = stockService.batchReserveStock(requests);

        
        return Result.success(String.format("鎵归噺棰勭暀鎴愬姛: %d/%d", successCount, requests.size()), successCount);
    }

    

    

    @GetMapping("/alerts")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鑾峰彇浣庡簱瀛樺晢鍝佸垪琛?, description = "鏌ヨ鎵€鏈変綆浜庨璀﹂槇鍊肩殑鍟嗗搧")
    public Result<List<Stock>> getLowStockAlerts(Authentication authentication) {
        List<Stock> lowStockProducts = stockAlertService.getLowStockProducts();
        return Result.success("鏌ヨ鎴愬姛", lowStockProducts);
    }

    


    @GetMapping("/alerts/threshold/{threshold}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鏍规嵁闃堝€兼煡璇綆搴撳瓨鍟嗗搧", description = "鏌ヨ搴撳瓨浣庝簬鎸囧畾闃堝€肩殑鍟嗗搧")
    public Result<List<Stock>> getLowStockByThreshold(
            @Parameter(description = "搴撳瓨闃堝€?) @PathVariable
            @NotNull(message = "闃堝€间笉鑳戒负绌?)
            @Min(value = 0, message = "闃堝€煎繀椤诲ぇ浜庣瓑浜?") Integer threshold,
            Authentication authentication) {
        List<Stock> lowStockProducts = stockAlertService.getLowStockProductsByThreshold(threshold);
        return Result.success("鏌ヨ鎴愬姛", lowStockProducts);
    }

    

    

    @PutMapping("/{productId}/threshold")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鏇存柊搴撳瓨棰勮闃堝€?, description = "璁剧疆鍟嗗搧鐨勪綆搴撳瓨棰勮闃堝€?)
    public Result<Boolean> updateLowStockThreshold(
            @Parameter(description = "鍟嗗搧ID") @PathVariable
            @NotNull(message = "鍟嗗搧ID涓嶈兘涓虹┖") Long productId,
            @Parameter(description = "棰勮闃堝€?) @RequestParam("threshold")
            @NotNull(message = "闃堝€间笉鑳戒负绌?)
            @Min(value = 0, message = "闃堝€煎繀椤诲ぇ浜庣瓑浜?") Integer threshold,
            Authentication authentication) {
        
        boolean result = stockAlertService.updateLowStockThreshold(productId, threshold);
        return Result.success("鏇存柊鎴愬姛", result);
    }

    

    @PutMapping("/threshold/batch")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "鎵归噺鏇存柊搴撳瓨棰勮闃堝€?, description = "鎵归噺璁剧疆鍟嗗搧鐨勪綆搴撳瓨棰勮闃堝€?)
    public Result<Integer> batchUpdateLowStockThreshold(
            @Parameter(description = "鍟嗗搧ID鍒楄〃") @RequestParam("productIds")
            @NotNull(message = "鍟嗗搧ID鍒楄〃涓嶈兘涓虹┖") List<Long> productIds,
            @Parameter(description = "棰勮闃堝€?) @RequestParam("threshold")
            @NotNull(message = "闃堝€间笉鑳戒负绌?)
            @Min(value = 0, message = "闃堝€煎繀椤诲ぇ浜庣瓑浜?") Integer threshold) {
        
        int count = stockAlertService.batchUpdateLowStockThreshold(productIds, threshold);
        return Result.success("鎵归噺鏇存柊鎴愬姛", count);
    }

    


    @PostMapping("/count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鍒涘缓搴撳瓨鐩樼偣璁板綍", description = "瀵规寚瀹氬晢鍝佽繘琛屽簱瀛樼洏鐐?)
    public Result<Long> createStockCount(
            @Parameter(description = "鍟嗗搧ID") @RequestParam("productId")
            @NotNull(message = "鍟嗗搧ID涓嶈兘涓虹┖") Long productId,
            @Parameter(description = "瀹為檯鐩樼偣鏁伴噺") @RequestParam("actualQuantity")
            @NotNull(message = "瀹為檯鏁伴噺涓嶈兘涓虹┖")
            @Min(value = 0, message = "瀹為檯鏁伴噺蹇呴』澶т簬绛変簬0") Integer actualQuantity,
            @Parameter(description = "澶囨敞") @RequestParam(value = "remark", required = false) String remark,
            Authentication authentication) {
        

        
        Long operatorId = 1L; 
        String operatorName = authentication.getName();

        Long countId = stockCountService.createStockCount(productId, actualQuantity,
                operatorId, operatorName, remark);
        return Result.success("鐩樼偣璁板綍鍒涘缓鎴愬姛", countId);
    }

    

    @PutMapping("/count/{countId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "纭搴撳瓨鐩樼偣", description = "纭鐩樼偣璁板綍骞惰皟鏁村簱瀛?)
    public Result<Boolean> confirmStockCount(
            @Parameter(description = "鐩樼偣璁板綍ID") @PathVariable
            @NotNull(message = "鐩樼偣璁板綍ID涓嶈兘涓虹┖") Long countId,
            Authentication authentication) {
        

        
        Long confirmUserId = 1L; 
        String confirmUserName = authentication.getName();

        boolean result = stockCountService.confirmStockCount(countId, confirmUserId, confirmUserName);
        return Result.success("鐩樼偣纭鎴愬姛", result);
    }

    

    


    @DeleteMapping("/count/{countId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鍙栨秷搴撳瓨鐩樼偣", description = "鍙栨秷寰呯‘璁ょ殑鐩樼偣璁板綍")
    public Result<Boolean> cancelStockCount(
            @Parameter(description = "鐩樼偣璁板綍ID") @PathVariable
            @NotNull(message = "鐩樼偣璁板綍ID涓嶈兘涓虹┖") Long countId,
            Authentication authentication) {
        
        boolean result = stockCountService.cancelStockCount(countId);
        return Result.success("鐩樼偣璁板綍宸插彇娑?, result);
    }

    


    @GetMapping("/count/{countId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鏌ヨ鐩樼偣璁板綍", description = "鏍规嵁ID鏌ヨ鐩樼偣璁板綍璇︽儏")
    public Result<StockCount> getStockCountById(
            @Parameter(description = "鐩樼偣璁板綍ID") @PathVariable
            @NotNull(message = "鐩樼偣璁板綍ID涓嶈兘涓虹┖") Long countId,
            Authentication authentication) {
        
        StockCount stockCount = stockCountService.getStockCountById(countId);
        if (stockCount == null) {
            throw new ResourceNotFoundException("StockCount", String.valueOf(countId));
        }
        return Result.success("鏌ヨ鎴愬姛", stockCount);
    }

    


    @GetMapping("/count/product/{productId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鏍规嵁鍟嗗搧鏌ヨ鐩樼偣璁板綍", description = "鏌ヨ鎸囧畾鍟嗗搧鐨勭洏鐐硅褰曞垪琛?)
    public Result<List<StockCount>> getStockCountsByProductId(
            @Parameter(description = "鍟嗗搧ID") @PathVariable
            @NotNull(message = "鍟嗗搧ID涓嶈兘涓虹┖") Long productId,
            @Parameter(description = "寮€濮嬫椂闂?) @RequestParam(value = "startTime", required = false)
            @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            java.time.LocalDateTime startTime,
            @Parameter(description = "缁撴潫鏃堕棿") @RequestParam(value = "endTime", required = false)
            @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            java.time.LocalDateTime endTime,
            Authentication authentication) {
        
        List<StockCount> counts = stockCountService.getStockCountsByProductId(productId, startTime, endTime);
        return Result.success("鏌ヨ鎴愬姛", counts);
    }

    

    @GetMapping("/count/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "鏍规嵁鐘舵€佹煡璇㈢洏鐐硅褰?, description = "鏌ヨ鎸囧畾鐘舵€佺殑鐩樼偣璁板綍")
    public Result<List<StockCount>> getStockCountsByStatus(
            @Parameter(description = "鐩樼偣鐘舵€?) @PathVariable String status,
            @Parameter(description = "寮€濮嬫椂闂?) @RequestParam(value = "startTime", required = false)
            @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            java.time.LocalDateTime startTime,
            @Parameter(description = "缁撴潫鏃堕棿") @RequestParam(value = "endTime", required = false)
            @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            java.time.LocalDateTime endTime) {
        
        List<StockCount> counts = stockCountService.getStockCountsByStatus(status, startTime, endTime);
        return Result.success("鏌ヨ鎴愬姛", counts);
    }

    


    @GetMapping("/count/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "鏌ヨ寰呯‘璁ょ洏鐐规暟閲?, description = "鏌ヨ寰呯‘璁ょ殑鐩樼偣璁板綍鏁伴噺")
    public Result<Integer> countPendingRecords() {
        int count = stockCountService.countPendingRecords();
        return Result.success("鏌ヨ鎴愬姛", count);
    }

    


    @GetMapping("/logs/product/{productId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鏍规嵁鍟嗗搧鏌ヨ搴撳瓨鏃ュ織", description = "鏌ヨ鎸囧畾鍟嗗搧鐨勫簱瀛樻搷浣滄棩蹇?)
    public Result<List<StockLog>> getLogsByProductId(
            @Parameter(description = "鍟嗗搧ID") @PathVariable
            @NotNull(message = "鍟嗗搧ID涓嶈兘涓虹┖") Long productId,
            @Parameter(description = "寮€濮嬫椂闂?) @RequestParam(value = "startTime", required = false)
            @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            java.time.LocalDateTime startTime,
            @Parameter(description = "缁撴潫鏃堕棿") @RequestParam(value = "endTime", required = false)
            @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            java.time.LocalDateTime endTime,
            Authentication authentication) {
        
        List<StockLog> logs = stockLogService.getLogsByProductId(productId, startTime, endTime);
        return Result.success("鏌ヨ鎴愬姛", logs);
    }

    


    @GetMapping("/logs/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "鏍规嵁璁㈠崟鏌ヨ搴撳瓨鏃ュ織", description = "鏌ヨ鎸囧畾璁㈠崟鐨勫簱瀛樻搷浣滄棩蹇?)
    public Result<List<StockLog>> getLogsByOrderId(
            @Parameter(description = "璁㈠崟ID") @PathVariable
            @NotNull(message = "璁㈠崟ID涓嶈兘涓虹┖") Long orderId,
            Authentication authentication) {
        
        List<StockLog> logs = stockLogService.getLogsByOrderId(orderId);
        return Result.success("鏌ヨ鎴愬姛", logs);
    }

    

    


    @GetMapping("/logs/type/{operationType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "鏍规嵁鎿嶄綔绫诲瀷鏌ヨ搴撳瓨鏃ュ織", description = "鏌ヨ鎸囧畾鎿嶄綔绫诲瀷鐨勫簱瀛樻棩蹇?)
    public Result<List<StockLog>> getLogsByOperationType(
            @Parameter(description = "鎿嶄綔绫诲瀷") @PathVariable String operationType,
            @Parameter(description = "寮€濮嬫椂闂?) @RequestParam(value = "startTime", required = false)
            @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            java.time.LocalDateTime startTime,
            @Parameter(description = "缁撴潫鏃堕棿") @RequestParam(value = "endTime", required = false)
            @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            java.time.LocalDateTime endTime) {
        
        List<StockLog> logs = stockLogService.getLogsByOperationType(operationType, startTime, endTime);
        return Result.success("鏌ヨ鎴愬姛", logs);
    }

    


    public static class StockAdjustment {
        private Long productId;
        private String type; 
        private Integer quantity;
        private String remark;

        
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

    


    public static class StockAdjustmentRequest {
        private Long productId;
        private Integer quantity;
        private Long orderId;
        private String orderNo;
        private String remark;

        
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
