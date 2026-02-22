package com.cloud.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;






@Slf4j
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
@Tag(name = "鍟嗗搧鍒嗙被鏈嶅姟", description = "鍟嗗搧鍒嗙被璧勬簮鐨凴ESTful API鎺ュ彛")
public class CategoryController {

    private final CategoryService categoryService;

    


    @GetMapping
    @Operation(summary = "鍒嗛〉鏌ヨ鍟嗗搧鍒嗙被", description = "鑾峰彇鍟嗗搧鍒嗙被鍒楄〃锛屾敮鎸佸垎椤?)
    public Result<PageResult<CategoryDTO>> getCategories(
            @Parameter(description = "椤电爜") @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "椤电爜蹇呴』澶т簬0") Integer page,
            @Parameter(description = "姣忛〉鏁伴噺") @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "姣忛〉鏁伴噺蹇呴』澶т簬0")
            @Max(value = 100, message = "姣忛〉鏁伴噺涓嶈兘瓒呰繃100") Integer size,
            @Parameter(description = "鐖跺垎绫籌D") @RequestParam(required = false) Long parentId,
            @Parameter(description = "鍒嗙被鐘舵€?) @RequestParam(required = false) Integer status) {

        try {
            Page<CategoryDTO> pageResult = categoryService.getCategoriesPage(page, size, parentId, status);
            PageResult<CategoryDTO> result = PageResult.of(
                    pageResult.getCurrent(),
                    pageResult.getSize(),
                    pageResult.getTotal(),
                    pageResult.getRecords()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("鍒嗛〉鏌ヨ鍟嗗搧鍒嗙被澶辫触", e);
            return Result.error("鍒嗛〉鏌ヨ鍟嗗搧鍒嗙被澶辫触: " + e.getMessage());
        }
    }

    


    @GetMapping("/{id}")
    @Operation(summary = "鑾峰彇鍟嗗搧鍒嗙被璇︽儏", description = "鏍规嵁鍒嗙被ID鑾峰彇璇︾粏淇℃伅")
    public Result<CategoryDTO> getCategoryById(
            @Parameter(description = "鍒嗙被ID") @PathVariable
            @NotNull(message = "鍒嗙被ID涓嶈兘涓虹┖")
            @Positive(message = "鍒嗙被ID蹇呴』涓烘鏁存暟") Long id) {

        try {
            CategoryDTO category = categoryService.getCategoryById(id);
            if (category == null) {
                return Result.error("鍟嗗搧鍒嗙被涓嶅瓨鍦?);
            }
            return Result.success("鏌ヨ鎴愬姛", category);
        } catch (Exception e) {
            log.error("鑾峰彇鍟嗗搧鍒嗙被璇︽儏澶辫触锛屽垎绫籌D: {}", id, e);
            return Result.error("鑾峰彇鍟嗗搧鍒嗙被璇︽儏澶辫触: " + e.getMessage());
        }
    }

    


    @GetMapping("/tree")
    @Operation(summary = "鑾峰彇鏍戝舰鍒嗙被缁撴瀯", description = "鑾峰彇瀹屾暣鐨勬爲褰㈠晢鍝佸垎绫荤粨鏋?)
    public Result<List<CategoryDTO>> getCategoryTree(
            @Parameter(description = "鏄惁鍙繑鍥炲惎鐢ㄧ殑鍒嗙被") @RequestParam(defaultValue = "false") Boolean enabledOnly) {

        try {
            List<CategoryDTO> tree = categoryService.getCategoryTree(enabledOnly);
            return Result.success("鏌ヨ鎴愬姛", tree);
        } catch (Exception e) {
            log.error("鑾峰彇鏍戝舰鍒嗙被缁撴瀯澶辫触", e);
            return Result.error("鑾峰彇鏍戝舰鍒嗙被缁撴瀯澶辫触: " + e.getMessage());
        }
    }

    

    @GetMapping("/{id}/children")
    @Operation(summary = "鑾峰彇瀛愬垎绫诲垪琛?, description = "鑾峰彇鎸囧畾鍒嗙被涓嬬殑鎵€鏈夊瓙鍒嗙被")
    public Result<List<CategoryDTO>> getChildrenCategories(
            @Parameter(description = "鐖跺垎绫籌D") @PathVariable Long id,
            @Parameter(description = "鏄惁閫掑綊鑾峰彇") @RequestParam(defaultValue = "false") Boolean recursive) {

        try {
            List<CategoryDTO> children = categoryService.getChildrenCategories(id, recursive);
            return Result.success("鏌ヨ鎴愬姛", children);
        } catch (Exception e) {
            log.error("鑾峰彇瀛愬垎绫诲垪琛ㄥけ璐ワ紝鐖跺垎绫籌D: {}", id, e);
            return Result.error("鑾峰彇瀛愬垎绫诲垪琛ㄥけ璐? " + e.getMessage());
        }
    }

    


    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "鍒涘缓鍟嗗搧鍒嗙被", description = "鍒涘缓鏂扮殑鍟嗗搧鍒嗙被")
    public Result<CategoryDTO> createCategory(
            @Parameter(description = "鍒嗙被淇℃伅") @RequestBody
            @Valid @NotNull(message = "鍒嗙被淇℃伅涓嶈兘涓虹┖") CategoryDTO categoryDTO) {

        try {
            CategoryDTO created = categoryService.createCategory(categoryDTO);
            return Result.success("鍟嗗搧鍒嗙被鍒涘缓鎴愬姛", created);
        } catch (Exception e) {
            log.error("鍒涘缓鍟嗗搧鍒嗙被澶辫触", e);
            return Result.error("鍒涘缓鍟嗗搧鍒嗙被澶辫触: " + e.getMessage());
        }
    }

    


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "鏇存柊鍟嗗搧鍒嗙被", description = "鏇存柊鍟嗗搧鍒嗙被淇℃伅")
    public Result<Boolean> updateCategory(
            @Parameter(description = "鍒嗙被ID") @PathVariable Long id,
            @Parameter(description = "鍒嗙被淇℃伅") @RequestBody
            @Valid @NotNull(message = "鍒嗙被淇℃伅涓嶈兘涓虹┖") CategoryDTO categoryDTO) {

        categoryDTO.setId(id);

        try {
            boolean result = categoryService.updateCategory(categoryDTO);
            return Result.success("鍟嗗搧鍒嗙被鏇存柊鎴愬姛", result);
        } catch (Exception e) {
            log.error("鏇存柊鍟嗗搧鍒嗙被澶辫触锛屽垎绫籌D: {}", id, e);
            return Result.error("鏇存柊鍟嗗搧鍒嗙被澶辫触: " + e.getMessage());
        }
    }

    


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "鍒犻櫎鍟嗗搧鍒嗙被", description = "鍒犻櫎鍟嗗搧鍒嗙被锛堥€昏緫鍒犻櫎锛?)
    public Result<Boolean> deleteCategory(
            @Parameter(description = "鍒嗙被ID") @PathVariable
            @NotNull(message = "鍒嗙被ID涓嶈兘涓虹┖") Long id,
            @Parameter(description = "鏄惁鍒犻櫎瀛愬垎绫?) @RequestParam(defaultValue = "false") Boolean cascade) {

        try {
            boolean result = categoryService.deleteCategory(id, cascade);
            return Result.success("鍒犻櫎鎴愬姛", result);
        } catch (Exception e) {
            log.error("鍒犻櫎鍟嗗搧鍒嗙被澶辫触锛屽垎绫籌D: {}", id, e);
            return Result.error("鍒犻櫎澶辫触: " + e.getMessage());
        }
    }

    

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "鏇存柊鍒嗙被鐘舵€?, description = "鍚敤鎴栫鐢ㄥ垎绫?)
    public Result<Boolean> updateCategoryStatus(
            @Parameter(description = "鍒嗙被ID") @PathVariable Long id,
            @Parameter(description = "鍒嗙被鐘舵€?) @RequestParam Integer status) {

        try {
            boolean result = categoryService.updateCategoryStatus(id, status);
            return Result.success("鐘舵€佹洿鏂版垚鍔?, result);
        } catch (Exception e) {
            log.error("鏇存柊鍒嗙被鐘舵€佸け璐ワ紝鍒嗙被ID: {}, 鐘舵€? {}", id, status, e);
            return Result.error("鏇存柊鐘舵€佸け璐? " + e.getMessage());
        }
    }

    


    @PatchMapping("/{id}/sort")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "鏇存柊鍒嗙被鎺掑簭", description = "鏇存柊鍒嗙被鐨勬帓搴忓€?)
    public Result<Boolean> updateCategorySort(
            @Parameter(description = "鍒嗙被ID") @PathVariable Long id,
            @Parameter(description = "鎺掑簭鍊?) @RequestParam Integer sort) {

        try {
            boolean result = categoryService.updateCategorySort(id, sort);
            return Result.success("鎺掑簭鏇存柊鎴愬姛", result);
        } catch (Exception e) {
            log.error("鏇存柊鍒嗙被鎺掑簭澶辫触锛屽垎绫籌D: {}, 鎺掑簭: {}", id, sort, e);
            return Result.error("鏇存柊鎺掑簭澶辫触: " + e.getMessage());
        }
    }

    


    @PatchMapping("/{id}/move")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "绉诲姩鍒嗙被", description = "灏嗗垎绫荤Щ鍔ㄥ埌鏂扮殑鐖跺垎绫讳笅")
    public Result<Boolean> moveCategory(
            @Parameter(description = "鍒嗙被ID") @PathVariable Long id,
            @Parameter(description = "鏂扮埗鍒嗙被ID") @RequestParam Long newParentId) {

        try {
            boolean result = categoryService.moveCategory(id, newParentId);
            return Result.success("绉诲姩鎴愬姛", result);
        } catch (Exception e) {
            log.error("绉诲姩鍒嗙被澶辫触锛屽垎绫籌D: {}, 鏂扮埗鍒嗙被ID: {}", id, newParentId, e);
            return Result.error("绉诲姩澶辫触: " + e.getMessage());
        }
    }

    


    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "鎵归噺鍒犻櫎鍒嗙被", description = "鎵归噺鍒犻櫎鍟嗗搧鍒嗙被")
    public Result<Boolean> deleteCategoriesBatch(
            @Parameter(description = "鍒嗙被ID鍒楄〃") @RequestBody
            @NotNull(message = "鍒嗙被ID鍒楄〃涓嶈兘涓虹┖")
            @NotEmpty(message = "鍒嗙被ID鍒楄〃涓嶈兘涓虹┖") List<Long> ids) {

        try {
            boolean result = categoryService.deleteCategoriesBatch(ids);
            return Result.success("鎵归噺鍒犻櫎鎴愬姛", result);
        } catch (Exception e) {
            log.error("鎵归噺鍒犻櫎鍒嗙被澶辫触锛屽垎绫籌Ds: {}", ids, e);
            return Result.error("鎵归噺鍒犻櫎澶辫触: " + e.getMessage());
        }
    }

    

    @PatchMapping("/batch/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "鎵归噺鏇存柊鍒嗙被鐘舵€?, description = "鎵归噺鍚敤鎴栫鐢ㄥ垎绫?)
    public Result<Integer> updateCategoryStatusBatch(
            @Parameter(description = "鍒嗙被ID鍒楄〃") @RequestParam
            @NotNull(message = "鍒嗙被ID鍒楄〃涓嶈兘涓虹┖") List<Long> ids,
            @Parameter(description = "鍒嗙被鐘舵€?) @RequestParam
            @NotNull(message = "鍒嗙被鐘舵€佷笉鑳戒负绌?) Integer status) {

        if (ids == null || ids.isEmpty()) {
            return Result.badRequest("鍒嗙被ID鍒楄〃涓嶈兘涓虹┖");
        }

        if (ids.size() > 100) {
            return Result.badRequest("鎵归噺鎿嶄綔鏁伴噺涓嶈兘瓒呰繃100涓?);
        }

        try {
            int successCount = 0;
            for (Long id : ids) {
                if (categoryService.updateCategoryStatus(id, status)) {
                    successCount++;
                }
            }
            
            return Result.success(String.format("鎵归噺鏇存柊鍒嗙被鐘舵€佹垚鍔? %d/%d", successCount, ids.size()), successCount);
        } catch (Exception e) {
            log.error("鎵归噺鏇存柊鍒嗙被鐘舵€佸け璐? IDs: {}", ids, e);
            return Result.error("鎵归噺鏇存柊鐘舵€佸け璐? " + e.getMessage());
        }
    }

    


    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "鎵归噺鍒涘缓鍒嗙被", description = "鎵归噺鍒涘缓澶氫釜鍒嗙被")
    public Result<Integer> createCategoriesBatch(
            @Parameter(description = "鍒嗙被淇℃伅鍒楄〃") @RequestBody
            @Valid @NotEmpty(message = "鍒嗙被淇℃伅鍒楄〃涓嶈兘涓虹┖") List<CategoryDTO> categoryList) {

        if (categoryList.size() > 100) {
            return Result.badRequest("鎵归噺鍒涘缓鏁伴噺涓嶈兘瓒呰繃100涓?);
        }

        try {
            int successCount = 0;
            for (CategoryDTO categoryDTO : categoryList) {
                try {
                    categoryService.createCategory(categoryDTO);
                    successCount++;
                } catch (Exception e) {
                    log.error("鍒涘缓鍒嗙被澶辫触, name: {}", categoryDTO.getName(), e);
                }
            }
            
            return Result.success(String.format("鎵归噺鍒涘缓鍒嗙被鎴愬姛: %d/%d", successCount, categoryList.size()), successCount);
        } catch (Exception e) {
            log.error("鎵归噺鍒涘缓鍒嗙被澶辫触", e);
            return Result.error("鎵归噺鍒涘缓澶辫触: " + e.getMessage());
        }
    }
}
