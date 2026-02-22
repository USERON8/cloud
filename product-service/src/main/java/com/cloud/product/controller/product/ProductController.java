package com.cloud.product.controller.product;

import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.validation.BatchValidationUtils;
import com.cloud.product.module.dto.ProductPageDTO;
import com.cloud.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;






@Slf4j
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "鍟嗗搧鏈嶅姟", description = "鍟嗗搧璧勬簮鐨凴ESTful API鎺ュ彛")
public class ProductController {

    private final ProductService productService;

    


    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "鑾峰彇鍟嗗搧鍒楄〃", description = "鑾峰彇鍟嗗搧鍒楄〃锛屾敮鎸佸垎椤靛拰鏌ヨ鍙傛暟")
    public Result<PageResult<ProductVO>> getProducts(
            @Parameter(description = "椤电爜") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "姣忛〉鏁伴噺") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "鍟嗗搧鍚嶇О") @RequestParam(required = false) String name,
            @Parameter(description = "鍒嗙被ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "鍝佺墝ID") @RequestParam(required = false) Long brandId,
            @Parameter(description = "鍟嗗搧鐘舵€?) @RequestParam(required = false) Integer status) {

        ProductPageDTO productPageDTO = new ProductPageDTO();
        productPageDTO.setCurrent(page.longValue());
        productPageDTO.setSize(size.longValue());
        productPageDTO.setName(name);
        productPageDTO.setCategoryId(categoryId);
        productPageDTO.setBrandId(brandId);
        productPageDTO.setStatus(status);

        return Result.success(productService.getProductsPage(productPageDTO));
    }

    


    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "鏍规嵁鍟嗗搧鍚嶇О鏌ヨ鍟嗗搧", description = "鏍规嵁鍟嗗搧鍚嶇О鏌ヨ鍟嗗搧淇℃伅")
    public Result<List<ProductVO>> findByName(
            @Parameter(description = "鍟嗗搧鍚嶇О") @RequestParam
            @NotNull(message = "鍟嗗搧鍚嶇О涓嶈兘涓虹┖") String name) {
        List<ProductVO> products = productService.searchProductsByName(name, null);
        return Result.success("鏌ヨ鎴愬姛", products);
    }

    


    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "鑾峰彇鍟嗗搧璇︽儏", description = "鏍规嵁鍟嗗搧ID鑾峰彇鍟嗗搧璇︾粏淇℃伅")
    public Result<ProductVO> getProductById(
            @Parameter(description = "鍟嗗搧ID") @PathVariable
            @Positive(message = "鍟嗗搧ID蹇呴』涓烘鏁存暟") Long id) {

        ProductVO product = productService.getProductById(id);
        if (product == null) {
            log.warn("鍟嗗搧涓嶅瓨鍦紝鍟嗗搧ID: {}", id);
            throw new ResourceNotFoundException("Product", String.valueOf(id));
        }
        
        return Result.success("鏌ヨ鎴愬姛", product);
    }

    


    @PostMapping
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:create')")
    @Operation(summary = "鍒涘缓鍟嗗搧", description = "鍒涘缓鏂板晢鍝?)
    public Result<Long> createProduct(
            @Parameter(description = "鍟嗗搧淇℃伅") @RequestBody
            @Valid @NotNull(message = "鍟嗗搧淇℃伅涓嶈兘涓虹┖") ProductRequestDTO requestDTO) {

        Long productId = productService.createProduct(requestDTO);
        
        return Result.success("鍟嗗搧鍒涘缓鎴愬姛", productId);
    }

    


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "鏇存柊鍟嗗搧淇℃伅", description = "鏇存柊鍟嗗搧淇℃伅")
    public Result<Boolean> updateProduct(
            @Parameter(description = "鍟嗗搧ID") @PathVariable Long id,
            @Parameter(description = "鍟嗗搧淇℃伅") @RequestBody
            @Valid @NotNull(message = "鍟嗗搧淇℃伅涓嶈兘涓虹┖") ProductRequestDTO requestDTO,
            Authentication authentication) {

        boolean result = productService.updateProduct(id, requestDTO);
        
        return Result.success("鍟嗗搧鏇存柊鎴愬姛", result);
    }

    


    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "閮ㄥ垎鏇存柊鍟嗗搧淇℃伅", description = "閮ㄥ垎鏇存柊鍟嗗搧淇℃伅")
    public Result<Boolean> patchProduct(
            @Parameter(description = "鍟嗗搧ID") @PathVariable Long id,
            @Parameter(description = "鍟嗗搧淇℃伅") @RequestBody ProductRequestDTO requestDTO,
            Authentication authentication) {

        boolean result = productService.updateProduct(id, requestDTO);
        
        return Result.success("鍟嗗搧鏇存柊鎴愬姛", result);
    }

    


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "鍒犻櫎鍟嗗搧", description = "鍒犻櫎鍟嗗搧")
    public Result<Boolean> deleteProduct(
            @Parameter(description = "鍟嗗搧ID") @PathVariable
            @Positive(message = "鍟嗗搧ID蹇呴』涓烘鏁存暟") Long id) {

        boolean result = productService.deleteProduct(id);
        
        return Result.success("鍟嗗搧鍒犻櫎鎴愬姛", result);
    }

    


    @GetMapping("/batch")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "鎵归噺鑾峰彇鍟嗗搧", description = "鏍规嵁ID鍒楄〃鎵归噺鑾峰彇鍟嗗搧淇℃伅")
    public Result<List<ProductVO>> getProductsByIds(
            @Parameter(description = "鍟嗗搧ID鍒楄〃") @RequestParam
            @NotNull(message = "鍟嗗搧ID鍒楄〃涓嶈兘涓虹┖") List<Long> ids) {

        BatchValidationUtils.validateIdList(ids, "鎵归噺鏌ヨ鍟嗗搧");
        List<ProductVO> products = productService.getProductsByIds(ids);
        
        return Result.success("鏌ヨ鎴愬姛", products);
    }

    


    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "鑾峰彇鍟嗗搧妗ｆ", description = "鑾峰彇鍟嗗搧璇︾粏妗ｆ淇℃伅")
    public Result<ProductVO> getProductProfile(
            @Parameter(description = "鍟嗗搧ID") @PathVariable Long id,
            Authentication authentication) {

        ProductVO productProfile = productService.getProductById(id);
        
        return Result.success("鏌ヨ鎴愬姛", productProfile);
    }

    


    @PutMapping("/{id}/profile")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "鏇存柊鍟嗗搧妗ｆ", description = "鏇存柊鍟嗗搧璇︾粏妗ｆ淇℃伅")
    public Result<Boolean> updateProductProfile(
            @Parameter(description = "鍟嗗搧ID") @PathVariable Long id,
            @Parameter(description = "鍟嗗搧妗ｆ淇℃伅") @RequestBody
            @Valid @NotNull(message = "鍟嗗搧妗ｆ淇℃伅涓嶈兘涓虹┖") ProductRequestDTO profileDTO,
            Authentication authentication) {

        boolean result = productService.updateProduct(id, profileDTO);
        
        return Result.success("鍟嗗搧妗ｆ鏇存柊鎴愬姛", result);
    }

    

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "鏇存柊鍟嗗搧鐘舵€?, description = "鍚敤鎴栫鐢ㄥ晢鍝?)
    public Result<Boolean> updateProductStatus(
            @Parameter(description = "鍟嗗搧ID") @PathVariable Long id,
            @Parameter(description = "鍟嗗搧鐘舵€?) @RequestParam Integer status) {

        boolean result;
        if (status == 1) {
            result = productService.enableProduct(id);
            
        } else {
            result = productService.disableProduct(id);
            
        }
        return Result.success("鍟嗗搧鐘舵€佹洿鏂版垚鍔?, result);
    }

    


    @GetMapping("/category/{categoryId}")
    @Operation(summary = "鏍规嵁鍒嗙被鏌ヨ鍟嗗搧", description = "鑾峰彇鎸囧畾鍒嗙被涓嬬殑鍟嗗搧鍒楄〃")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    public Result<List<ProductVO>> getProductsByCategoryId(
            @Parameter(description = "鍒嗙被ID", required = true) @PathVariable
            @NotNull(message = "鍒嗙被ID涓嶈兘涓虹┖")
            @Positive(message = "鍒嗙被ID蹇呴』涓烘鏁存暟") Long categoryId,

            @Parameter(description = "鍟嗗搧鐘舵€侊細1-涓婃灦锛?-涓嬫灦") @RequestParam(value = "status", required = false) Integer status) {

        
        List<ProductVO> products = productService.getProductsByCategoryId(categoryId, status);
        return Result.success(products);
    }

    


    @GetMapping("/brand/{brandId}")
    @Operation(summary = "鏍规嵁鍝佺墝鏌ヨ鍟嗗搧", description = "鑾峰彇鎸囧畾鍝佺墝涓嬬殑鍟嗗搧鍒楄〃")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    public Result<List<ProductVO>> getProductsByBrandId(
            @Parameter(description = "鍝佺墝ID", required = true) @PathVariable
            @NotNull(message = "鍝佺墝ID涓嶈兘涓虹┖")
            @Positive(message = "鍝佺墝ID蹇呴』涓烘鏁存暟") Long brandId,

            @Parameter(description = "鍟嗗搧鐘舵€侊細1-涓婃灦锛?-涓嬫灦") @RequestParam(value = "status", required = false) Integer status) {

        
        List<ProductVO> products = productService.getProductsByBrandId(brandId, status);
        return Result.success(products);
    }

    


    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "鎵归噺鍒犻櫎鍟嗗搧", description = "鏍规嵁ID鍒楄〃鎵归噺鍒犻櫎鍟嗗搧")
    public Result<Boolean> batchDeleteProducts(
            @Parameter(description = "鍟嗗搧ID鍒楄〃", required = true) @RequestBody
            @NotEmpty(message = "ID鍒楄〃涓嶈兘涓虹┖") List<Long> ids) {

        BatchValidationUtils.validateIdList(ids, "鎵归噺鍒犻櫎鍟嗗搧");
        
        Boolean success = productService.batchDeleteProducts(ids);
        
        return Result.success("鎵归噺鍒犻櫎鍟嗗搧鎴愬姛", success);
    }

    


    @PutMapping("/batch/enable")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "鎵归噺涓婃灦鍟嗗搧", description = "鎵归噺灏嗗晢鍝佽涓轰笂鏋剁姸鎬?)
    public Result<Boolean> batchEnableProducts(
            @Parameter(description = "鍟嗗搧ID鍒楄〃", required = true) @RequestBody
            @NotEmpty(message = "ID鍒楄〃涓嶈兘涓虹┖") List<Long> ids) {

        if (ids.size() > 100) {
            return Result.error("鎵归噺鎿嶄綔鏁伴噺涓嶈兘瓒呰繃100涓?);
        }

        
        Boolean success = productService.batchEnableProducts(ids);
        
        return Result.success("鎵归噺涓婃灦鍟嗗搧鎴愬姛", success);
    }

    


    @PutMapping("/batch/disable")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "鎵归噺涓嬫灦鍟嗗搧", description = "鎵归噺灏嗗晢鍝佽涓轰笅鏋剁姸鎬?)
    public Result<Boolean> batchDisableProducts(
            @Parameter(description = "鍟嗗搧ID鍒楄〃", required = true) @RequestBody
            @NotEmpty(message = "ID鍒楄〃涓嶈兘涓虹┖") List<Long> ids) {

        if (ids.size() > 100) {
            return Result.error("鎵归噺鎿嶄綔鏁伴噺涓嶈兘瓒呰繃100涓?);
        }

        
        Boolean success = productService.batchDisableProducts(ids);
        
        return Result.success("鎵归噺涓嬫灦鍟嗗搧鎴愬姛", success);
    }

    


    @PostMapping("/batch")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:create')")
    @Operation(summary = "鎵归噺鍒涘缓鍟嗗搧", description = "鎵归噺鍒涘缓澶氫釜鍟嗗搧")
    public Result<Integer> batchCreateProducts(
            @Parameter(description = "鍟嗗搧淇℃伅鍒楄〃", required = true) @RequestBody
            @Valid @NotEmpty(message = "鍟嗗搧淇℃伅鍒楄〃涓嶈兘涓虹┖") List<ProductRequestDTO> productList) {

        if (productList.size() > 100) {
            return Result.error("鎵归噺鍒涘缓鏁伴噺涓嶈兘瓒呰繃100涓?);
        }

        
        int successCount = 0;
        for (ProductRequestDTO requestDTO : productList) {
            try {
                productService.createProduct(requestDTO);
                successCount++;
            } catch (Exception e) {
                log.error("鍒涘缓鍟嗗搧澶辫触 - 鍟嗗搧鍚嶇О: {}", requestDTO.getName(), e);
            }
        }

        
        return Result.success(String.format("鎵归噺鍒涘缓鍟嗗搧鎴愬姛: %d/%d", successCount, productList.size()), successCount);
    }

    


    @PutMapping("/batch")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "鎵归噺鏇存柊鍟嗗搧", description = "鎵归噺鏇存柊澶氫釜鍟嗗搧淇℃伅")
    public Result<Integer> batchUpdateProducts(
            @Parameter(description = "鍟嗗搧鏇存柊淇℃伅鍒楄〃", required = true) @RequestBody
            @Valid @NotEmpty(message = "鍟嗗搧淇℃伅鍒楄〃涓嶈兘涓虹┖") List<ProductUpdateRequest> productList) {

        if (productList.size() > 100) {
            return Result.error("鎵归噺鏇存柊鏁伴噺涓嶈兘瓒呰繃100涓?);
        }

        
        int successCount = 0;
        for (ProductUpdateRequest request : productList) {
            try {
                if (request.getId() != null && request.getRequestDTO() != null) {
                    productService.updateProduct(request.getId(), request.getRequestDTO());
                    successCount++;
                }
            } catch (Exception e) {
                log.error("鏇存柊鍟嗗搧澶辫触 - 鍟嗗搧ID: {}", request.getId(), e);
            }
        }

        
        return Result.success(String.format("鎵归噺鏇存柊鍟嗗搧鎴愬姛: %d/%d", successCount, productList.size()), successCount);
    }

    


    public static class ProductUpdateRequest {
        private Long id;
        private ProductRequestDTO requestDTO;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public ProductRequestDTO getRequestDTO() {
            return requestDTO;
        }

        public void setRequestDTO(ProductRequestDTO requestDTO) {
            this.requestDTO = requestDTO;
        }
    }
}
