package com.cloud.search.controller;

import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.Result;
import com.cloud.search.document.ShopDocument;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.dto.ShopSearchRequest;
import com.cloud.search.service.ShopSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;








@Slf4j
@RestController
@RequestMapping("/api/search/shops")
@RequiredArgsConstructor
@Tag(name = "搴楅摵鎼滅储", description = "搴楅摵鎼滅储鐩稿叧鎺ュ彛")
@Validated
public class ShopSearchController {

    private final ShopSearchService shopSearchService;

    @Operation(summary = "澶嶆潅搴楅摵鎼滅储", description = "鏀寔澶氭潯浠剁粍鍚堢殑澶嶆潅搴楅摵鎼滅储锛屽寘鍚仛鍚堛€侀珮浜€佹帓搴忕瓑鍔熻兘")
    @PostMapping("/complex-search")
    public Result<SearchResult<ShopDocument>> complexSearch(@Valid @RequestBody ShopSearchRequest request) {
        


        SearchResult<ShopDocument> result = shopSearchService.searchShops(request);

        
        return Result.success("鎼滅储鎴愬姛", result);
    }

    @Operation(summary = "鑾峰彇搴楅摵绛涢€夎仛鍚堜俊鎭?, description = "鑾峰彇搴楅摵鎼滅储鐨勭瓫閫夎仛鍚堜俊鎭紝鐢ㄤ簬鏋勫缓绛涢€夋潯浠?)
    @PostMapping("/filters")
    public Result<SearchResult<ShopDocument>> getShopFilters(@Valid @RequestBody ShopSearchRequest request) {
        

        return Result.success("鑾峰彇绛涢€変俊鎭垚鍔?, result);
    }

    @Operation(summary = "鑾峰彇搴楅摵鎼滅储寤鸿", description = "鏍规嵁杈撳叆鍏抽敭瀛楄幏鍙栧簵閾烘悳绱㈠缓璁?)
    @GetMapping("/suggestions")
    public Result<List<String>> getSearchSuggestions(
            @Parameter(description = "鎼滅储鍏抽敭瀛?) @RequestParam String keyword,
            @Parameter(description = "寤鸿鏁伴噺") @RequestParam(defaultValue = "10") Integer size) {
        

        List<String> suggestions = shopSearchService.getSearchSuggestions(keyword, size);

        
        return Result.success("鑾峰彇寤鸿鎴愬姛", suggestions);
    }

    @Operation(summary = "鑾峰彇鐑棬搴楅摵", description = "鑾峰彇褰撳墠鐑棬鐨勫簵閾?)
    @GetMapping("/hot-shops")
    public Result<List<ShopDocument>> getHotShops(
            @Parameter(description = "搴楅摵鏁伴噺") @RequestParam(defaultValue = "10") Integer size) {
        

        List<ShopDocument> hotShops = shopSearchService.getHotShops(size);

        
        return Result.success("鑾峰彇鐑棬搴楅摵鎴愬姛", hotShops);
    }

    @Operation(summary = "鏍规嵁搴楅摵ID鏌ヨ", description = "鏍规嵁搴楅摵ID鏌ヨ搴楅摵璇︽儏")
    @GetMapping("/{shopId}")
    public Result<ShopDocument> getShopById(@Parameter(description = "搴楅摵ID") @PathVariable Long shopId) {
        

        ShopDocument shop = shopSearchService.findByShopId(shopId);

        if (shop == null) {
            log.warn("鈿狅笍 搴楅摵涓嶅瓨鍦?- 搴楅摵ID: {}", shopId);
            throw new ResourceNotFoundException("Shop", String.valueOf(shopId));
        }
        
        return Result.success("鏌ヨ鎴愬姛", shop);
    }

    @Operation(summary = "鎺ㄨ崘搴楅摵", description = "鑾峰彇鎺ㄨ崘搴楅摵鍒楄〃")
    @GetMapping("/recommended")
    public Result<SearchResult<ShopDocument>> getRecommendedShops(
            @Parameter(description = "椤电爜") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") Integer size) {
        

        ShopSearchRequest request = new ShopSearchRequest();
        request.setRecommended(true);
        request.setStatus(1); 
        request.setPage(page);
        request.setSize(size);
        request.setSortBy("hotScore");
        request.setSortOrder("desc");

        SearchResult<ShopDocument> result = shopSearchService.searchShops(request);

        
        return Result.success("鑾峰彇鎺ㄨ崘搴楅摵鎴愬姛", result);
    }

    @Operation(summary = "鎸夊湴鍖烘悳绱㈠簵閾?, description = "鏍规嵁鍦板尯鍏抽敭瀛楁悳绱㈠簵閾?)
    @GetMapping("/by-location")
    public Result<SearchResult<ShopDocument>> searchShopsByLocation(
            @Parameter(description = "鍦板尯鍏抽敭瀛?) @RequestParam String location,
            @Parameter(description = "椤电爜") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") Integer size) {
        

        ShopSearchRequest request = new ShopSearchRequest();
        request.setAddressKeyword(location);
        request.setStatus(1); 
        request.setPage(page);
        request.setSize(size);
        request.setSortBy("rating");
        request.setSortOrder("desc");

        SearchResult<ShopDocument> result = shopSearchService.searchShops(request);

        
        return Result.success("鎼滅储鎴愬姛", result);
    }
}
