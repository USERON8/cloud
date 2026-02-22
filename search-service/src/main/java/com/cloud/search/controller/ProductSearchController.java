package com.cloud.search.controller;


import com.cloud.common.result.Result;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductFilterRequest;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.mapper.SearchRequestMapper;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.service.ElasticsearchOptimizedService;
import com.cloud.search.service.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;





@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "鍟嗗搧鎼滅储", description = "鍟嗗搧鎼滅储鐩稿叧鎺ュ彛")
@Validated
public class ProductSearchController {

    private final ProductSearchService productSearchService;
    private final ProductDocumentRepository productDocumentRepository;
    private final ElasticsearchOptimizedService elasticsearchOptimizedService;
    private final SearchRequestMapper searchRequestMapper;


    @Operation(summary = "澶嶆潅鍟嗗搧鎼滅储", description = "鏀寔澶氭潯浠剁粍鍚堢殑澶嶆潅鍟嗗搧鎼滅储锛屽寘鍚仛鍚堛€侀珮浜€佹帓搴忕瓑鍔熻兘")
    @PostMapping("/complex-search")
    @PreAuthorize("hasAuthority('SCOPE_search:read')")
    public Result<SearchResult<ProductDocument>> complexSearch(@Valid @RequestBody ProductSearchRequest request) {
        


        SearchResult<ProductDocument> result = productSearchService.searchProducts(request);

        
        return Result.success("鎼滅储鎴愬姛", result);
    }

    @Operation(summary = "鑾峰彇鍟嗗搧绛涢€夎仛鍚堜俊鎭?, description = "鑾峰彇鍟嗗搧鎼滅储鐨勭瓫閫夎仛鍚堜俊鎭紝鐢ㄤ簬鏋勫缓绛涢€夋潯浠?)
    @PostMapping("/filters")
    public Result<SearchResult<ProductDocument>> getProductFilters(@Valid @RequestBody ProductSearchRequest request) {
        

        return Result.success("鑾峰彇绛涢€変俊鎭垚鍔?, result);
    }

    @Operation(summary = "鑾峰彇鎼滅储寤鸿", description = "鏍规嵁杈撳叆鍏抽敭瀛楄幏鍙栨悳绱㈠缓璁?)
    @GetMapping("/suggestions")
    public Result<List<String>> getSearchSuggestions(
            @Parameter(description = "鎼滅储鍏抽敭瀛?) @RequestParam String keyword,
            @Parameter(description = "寤鸿鏁伴噺") @RequestParam(defaultValue = "10") Integer size) {
        

        List<String> suggestions = productSearchService.getSearchSuggestions(keyword, size);

        
        return Result.success("鑾峰彇寤鸿鎴愬姛", suggestions);
    }

    @Operation(summary = "鑾峰彇鐑棬鎼滅储鍏抽敭瀛?, description = "鑾峰彇褰撳墠鐑棬鐨勬悳绱㈠叧閿瓧")
    @GetMapping("/hot-keywords")
    public Result<List<String>> getHotSearchKeywords(
            @Parameter(description = "鍏抽敭瀛楁暟閲?) @RequestParam(defaultValue = "10") Integer size) {
        

        List<String> hotKeywords = productSearchService.getHotSearchKeywords(size);

        
        return Result.success("鑾峰彇鐑棬鍏抽敭瀛楁垚鍔?, hotKeywords);
    }

    @Operation(summary = "鍏抽敭璇嶆悳绱㈠晢鍝?, description = "鏍规嵁鍏抽敭璇嶆悳绱㈠晢鍝侊紝鏀寔涓枃鍒嗚瘝鍜屾嫾闊虫悳绱?)
    @GetMapping("/search")
    public Result<Page<ProductDocument>> searchProducts(
            @Parameter(description = "鎼滅储鍏抽敭璇?) @RequestParam String keyword,
            @Parameter(description = "椤电爜锛屼粠0寮€濮?) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "鎺掑簭瀛楁") @RequestParam(defaultValue = "hotScore") String sortBy,
            @Parameter(description = "鎺掑簭鏂瑰悜") @RequestParam(defaultValue = "desc") String sortDir) {

        

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ProductDocument> result = productDocumentRepository.searchByKeyword(keyword, pageable);

        
        return Result.success("鎼滅储鎴愬姛", result);
    }

    @Operation(summary = "鍒嗙被鍟嗗搧鎼滅储", description = "鍦ㄦ寚瀹氬垎绫讳笅鎼滅储鍟嗗搧")
    @GetMapping("/search/category/{categoryId}")
    public Result<Page<ProductDocument>> searchByCategory(
            @Parameter(description = "鍒嗙被ID") @PathVariable Long categoryId,
            @Parameter(description = "鎼滅储鍏抽敭璇?) @RequestParam(required = false) String keyword,
            @Parameter(description = "椤电爜") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));

        Page<ProductDocument> result;
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = productDocumentRepository.searchByKeywordAndCategory(keyword, categoryId, pageable);
        } else {
            result = productDocumentRepository.findByCategoryId(categoryId, pageable);
        }

        

        return Result.success("鎼滅储鎴愬姛", result);
    }

    @Operation(summary = "搴楅摵鍟嗗搧鎼滅储", description = "鍦ㄦ寚瀹氬簵閾轰笅鎼滅储鍟嗗搧")
    @GetMapping("/search/shop/{shopId}")
    public Result<Page<ProductDocument>> searchByShop(
            @Parameter(description = "搴楅摵ID") @PathVariable Long shopId,
            @Parameter(description = "鎼滅储鍏抽敭璇?) @RequestParam(required = false) String keyword,
            @Parameter(description = "椤电爜") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));

        Page<ProductDocument> result;
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = productDocumentRepository.searchByKeywordAndShop(keyword, shopId, pageable);
        } else {
            result = productDocumentRepository.findByShopId(shopId, pageable);
        }

        

        return Result.success("鎼滅储鎴愬姛", result);
    }

    @Operation(summary = "楂樼骇鎼滅储", description = "鏀寔澶氭潯浠剁粍鍚堢殑楂樼骇鎼滅储")
    @GetMapping("/search/advanced")
    public Result<Page<ProductDocument>> advancedSearch(
            @Parameter(description = "鎼滅储鍏抽敭璇?) @RequestParam String keyword,
            @Parameter(description = "鏈€浣庝环鏍?) @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "鏈€楂樹环鏍?) @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "椤电爜") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));
        BigDecimal min = minPrice != null ? minPrice : BigDecimal.ZERO;
        BigDecimal max = maxPrice != null ? maxPrice : new BigDecimal("999999");

        Page<ProductDocument> result = productDocumentRepository.advancedSearch(keyword, min, max, pageable);

        

        return Result.success("鎼滅储鎴愬姛", result);
    }

    @Operation(summary = "鏅鸿兘鎼滅储", description = "浣跨敤浼樺寲鐨凟S寮曟搸杩涜鏅鸿兘鎼滅储")
    @GetMapping("/smart-search")
    public Result<ElasticsearchOptimizedService.SearchResult> smartSearch(
            @Parameter(description = "鎼滅储鍏抽敭璇?) @RequestParam(required = false) String keyword,
            @Parameter(description = "鍒嗙被ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "鏈€浣庝环鏍?) @RequestParam(required = false) Double minPrice,
            @Parameter(description = "鏈€楂樹环鏍?) @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "鎺掑簭瀛楁") @RequestParam(defaultValue = "score") String sortField,
            @Parameter(description = "鎺掑簭鏂瑰悜") @RequestParam(defaultValue = "desc") String sortOrder,
            @Parameter(description = "椤电爜锛屼粠1寮€濮?) @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") int size) {

        


        int from = (page - 1) * size;
        ElasticsearchOptimizedService.SearchResult result = elasticsearchOptimizedService
                .smartProductSearch(keyword, categoryId, minPrice, maxPrice,
                        sortField, sortOrder, from, size);

        return Result.success("鎼滅储鎴愬姛", result);
    }


    @Operation(summary = "鎺ㄨ崘鍟嗗搧", description = "鑾峰彇鎺ㄨ崘鍟嗗搧鍒楄〃")
    @GetMapping("/recommended")
    public Result<Page<ProductDocument>> getRecommendedProducts(
            @Parameter(description = "椤电爜") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));
        Page<ProductDocument> result = productDocumentRepository.findByRecommendedTrue(pageable);

        
        return Result.success("鑾峰彇鎺ㄨ崘鍟嗗搧鎴愬姛", result);
    }

    @Operation(summary = "鏂板搧鎺ㄨ崘", description = "鑾峰彇鏂板搧鍒楄〃")
    @GetMapping("/new")
    public Result<Page<ProductDocument>> getNewProducts(
            @Parameter(description = "椤电爜") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProductDocument> result = productDocumentRepository.findByIsNewTrue(pageable);

        
        return Result.success("鑾峰彇鏂板搧鎴愬姛", result);
    }

    @Operation(summary = "鐑攢鍟嗗搧", description = "鑾峰彇鐑攢鍟嗗搧鍒楄〃")
    @GetMapping("/hot")
    public Result<Page<ProductDocument>> getHotProducts(
            @Parameter(description = "椤电爜") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "salesCount"));
        Page<ProductDocument> result = productDocumentRepository.findByIsHotTrue(pageable);

        
        return Result.success("鑾峰彇鐑攢鍟嗗搧鎴愬姛", result);
    }

    @Operation(summary = "閲嶅缓鍟嗗搧绱㈠紩", description = "閲嶅缓鍟嗗搧鎼滅储绱㈠紩")
    @PostMapping("/rebuild-index")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    public Result<String> rebuildIndex() {
        productSearchService.rebuildProductIndex();
        
        return Result.success("绱㈠紩閲嶅汉鎴愬姛", "绱㈠紩閲嶅汉鎴愬姛");
    }

    

    @Operation(summary = "鍩虹鎼滅储", description = "鏍规嵁鍏抽敭瀛楄繘琛岀畝鍗曟悳绱?)
    @GetMapping("/basic")
    public Result<SearchResult<ProductDocument>> basicSearch(
            @Parameter(description = "鎼滅储鍏抽敭瀛?) @RequestParam(required = false) String keyword,
            @Parameter(description = "椤电爜锛屼粠0寮€濮?) @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") Integer size) {

        

        SearchResult<ProductDocument> result = productSearchService.basicSearch(keyword, page, size);

        
        return Result.success("鎼滅储鎴愬姛", result);
    }

    @Operation(summary = "绛涢€夋悳绱?, description = "鏀寔澶氭潯浠剁粍鍚堢殑绛涢€夋悳绱?)
    @PostMapping("/filter")
    public Result<SearchResult<ProductDocument>> filterSearch(@Valid @RequestBody ProductFilterRequest request) {
        


        
        ProductSearchRequest searchRequest = searchRequestMapper.toSearchRequest(request);
        SearchResult<ProductDocument> result = productSearchService.filterSearch(searchRequest);

        
        return Result.success("鎼滅储鎴愬姛", result);
    }

    @Operation(summary = "鎸夊垎绫荤瓫閫?, description = "鏍规嵁鍒嗙被ID绛涢€夊晢鍝?)
    @GetMapping("/filter/category/{categoryId}")
    public Result<SearchResult<ProductDocument>> filterByCategory(
            @Parameter(description = "鍒嗙被ID") @PathVariable Long categoryId,
            @Parameter(description = "椤电爜锛屼粠0寮€濮?) @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") Integer size) {

        

        SearchResult<ProductDocument> result = productSearchService.searchByCategory(categoryId, page, size);

        
        return Result.success("绛涢€夋垚鍔?, result);
    }

    @Operation(summary = "鎸夊搧鐗岀瓫閫?, description = "鏍规嵁鍝佺墝ID绛涢€夊晢鍝?)
    @GetMapping("/filter/brand/{brandId}")
    public Result<SearchResult<ProductDocument>> filterByBrand(
            @Parameter(description = "鍝佺墝ID") @PathVariable Long brandId,
            @Parameter(description = "椤电爜锛屼粠0寮€濮?) @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") Integer size) {

        

        SearchResult<ProductDocument> result = productSearchService.searchByBrand(brandId, page, size);

        
        return Result.success("绛涢€夋垚鍔?, result);
    }

    @Operation(summary = "鎸変环鏍煎尯闂寸瓫閫?, description = "鏍规嵁浠锋牸鍖洪棿绛涢€夊晢鍝?)
    @GetMapping("/filter/price")
    public Result<SearchResult<ProductDocument>> filterByPrice(
            @Parameter(description = "鏈€浣庝环鏍?) @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "鏈€楂樹环鏍?) @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "椤电爜锛屼粠0寮€濮?) @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") Integer size) {

        

        SearchResult<ProductDocument> result = productSearchService.searchByPriceRange(minPrice, maxPrice, page, size);

        
        return Result.success("绛涢€夋垚鍔?, result);
    }

    @Operation(summary = "鎸夊簵閾虹瓫閫?, description = "鏍规嵁搴楅摵ID绛涢€夊晢鍝?)
    @GetMapping("/filter/shop/{shopId}")
    public Result<SearchResult<ProductDocument>> filterByShop(
            @Parameter(description = "搴楅摵ID") @PathVariable Long shopId,
            @Parameter(description = "椤电爜锛屼粠0寮€濮?) @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") Integer size) {

        

        SearchResult<ProductDocument> result = productSearchService.searchByShop(shopId, page, size);

        
        return Result.success("绛涢€夋垚鍔?, result);
    }

    @Operation(summary = "缁勫悎绛涢€?, description = "鏀寔鍏抽敭瀛椼€佸垎绫汇€佸搧鐗屻€佷环鏍笺€佸簵閾虹瓑澶氭潯浠剁粍鍚堢瓫閫?)
    @GetMapping("/filter/combined")
    public Result<SearchResult<ProductDocument>> combinedFilter(
            @Parameter(description = "鎼滅储鍏抽敭瀛?) @RequestParam(required = false) String keyword,
            @Parameter(description = "鍒嗙被ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "鍝佺墝ID") @RequestParam(required = false) Long brandId,
            @Parameter(description = "鏈€浣庝环鏍?) @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "鏈€楂樹环鏍?) @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "搴楅摵ID") @RequestParam(required = false) Long shopId,
            @Parameter(description = "鎺掑簭瀛楁") @RequestParam(defaultValue = "hotScore") String sortBy,
            @Parameter(description = "鎺掑簭鏂瑰悜") @RequestParam(defaultValue = "desc") String sortOrder,
            @Parameter(description = "椤电爜锛屼粠0寮€濮?) @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "姣忛〉澶у皬") @RequestParam(defaultValue = "20") Integer size) {

        


        SearchResult<ProductDocument> result = productSearchService.combinedSearch(
                keyword, categoryId, brandId, minPrice, maxPrice, shopId,
                sortBy, sortOrder, page, size);

        
        return Result.success("绛涢€夋垚鍔?, result);
    }

}
