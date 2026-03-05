package com.cloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.product.ProductDTO;
import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.PageResult;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.product.converter.ProductConverter;
import com.cloud.product.exception.ProductServiceException;
import com.cloud.product.mapper.ProductMapper;
import com.cloud.product.module.dto.ProductPageDTO;
import com.cloud.product.module.entity.Category;
import com.cloud.product.module.entity.Product;
import com.cloud.product.module.entity.Shop;
import com.cloud.product.service.CategoryService;
import com.cloud.product.service.ProductService;
import com.cloud.product.service.ShopService;
import com.cloud.product.service.support.ProductCacheProtectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductConverter productConverter;
    private final ShopService shopService;
    private final CategoryService categoryService;
    private final ProductCacheProtectionService productCacheProtectionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(ProductRequestDTO requestDTO) {
        validateProductRequest(requestDTO, true);

        Shop shop = requireExistingShop(requestDTO.getShopId());
        validateOperationPermissionForShop(shop.getMerchantId(), null);
        if (requestDTO.getCategoryId() != null) {
            requireExistingCategory(requestDTO.getCategoryId());
        }

        Product product = new Product();
        applyRequest(product, requestDTO);

        boolean saved = save(product);
        if (!saved) {
            throw new BusinessException("Create product failed");
        }
        runAfterCommit(() -> {
            productCacheProtectionService.markProductExists(product.getId());
            productCacheProtectionService.evictProductCaches(product.getId());
        });

        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProduct(Long id, ProductRequestDTO requestDTO) {
        validateId(id);
        validateProductRequest(requestDTO, false);

        Product existing = requireExistingProduct(id);
        validateOperationPermissionForProduct(existing);

        if (requestDTO.getShopId() != null) {
            Shop targetShop = requireExistingShop(requestDTO.getShopId());
            validateOperationPermissionForShop(targetShop.getMerchantId(), id);
        }
        if (requestDTO.getCategoryId() != null) {
            requireExistingCategory(requestDTO.getCategoryId());
        }

        applyRequest(existing, requestDTO);
        boolean updated = updateById(existing);
        if (!updated) {
            return false;
        }
        scheduleProductCacheEvictionAfterCommit(id);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteProduct(Long id) {
        validateId(id);
        Product existing = requireExistingProduct(id);
        validateOperationPermissionForProduct(existing);

        boolean deleted = removeById(id);
        if (!deleted) {
            return false;
        }
        scheduleProductCacheEvictionAfterCommit(id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDeleteProducts(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }

        List<Product> existingProducts = listByIds(ids);
        validateBatchProductPermissions(existingProducts, ids);

        boolean deleted = removeByIds(ids);
        if (!deleted) {
            return false;
        }
        scheduleProductCacheEvictionAfterCommit(ids);

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "productCache", key = "#id", condition = "#id != null")
    public ProductVO getProductById(Long id) {
        validateId(id);
        return productCacheProtectionService.queryProductById(id, () -> {
            Product product = getById(id);
            return product == null ? null : productConverter.toVO(product);
        }).orElseThrow(() -> new ProductServiceException.ProductNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVO> getProductsByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        return productConverter.toVOList(listByIds(ids));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductVO> getProductsPage(ProductPageDTO pageDTO) {
        ProductPageDTO request = pageDTO == null ? new ProductPageDTO() : pageDTO;

        long current = request.getCurrent() == null || request.getCurrent() <= 0 ? 1L : request.getCurrent();
        long size = request.getSize() == null || request.getSize() <= 0 ? 20L : request.getSize();

        Page<Product> page = new Page<>(current, size);
        LambdaQueryWrapper<Product> queryWrapper = buildQueryWrapper(request);
        Page<Product> result = page(page, queryWrapper);

        List<ProductVO> records = productConverter.toVOList(result.getRecords());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVO> getProductsByCategoryId(Long categoryId, Integer status) {
        if (categoryId == null || categoryId <= 0) {
            throw new ProductServiceException.CategoryNotFoundException("Invalid category id");
        }
        if (categoryService.getById(categoryId) == null) {
            throw new ProductServiceException.CategoryNotFoundException(categoryId);
        }

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getCategoryId, categoryId);
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getCreatedAt);
        return productConverter.toVOList(list(wrapper));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVO> getProductsByBrandId(Long brandId, Integer status) {
        if (brandId == null || brandId <= 0) {
            throw new BusinessException("Invalid brand id");
        }
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getBrandId, brandId);
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getCreatedAt);
        return productConverter.toVOList(list(wrapper));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVO> searchProductsByName(String name, Integer status) {
        if (!StringUtils.hasText(name)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Product::getName, name);
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getCreatedAt);
        return productConverter.toVOList(list(wrapper));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getProductSuggestions(String keyword, Integer size) {
        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }

        int safeSize = size == null || size <= 0 ? 10 : Math.min(size, 50);
        String normalizedKeyword = keyword.trim();

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Product::getName)
                .like(Product::getName, normalizedKeyword)
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getCreatedAt)
                .last("LIMIT " + (safeSize * 3));

        List<Product> products = list(wrapper);
        if (CollectionUtils.isEmpty(products)) {
            return new ArrayList<>();
        }

        Set<String> deduplicated = new LinkedHashSet<>();
        for (Product product : products) {
            if (!StringUtils.hasText(product.getName())) {
                continue;
            }
            String suggestion = product.getName().trim();
            if (suggestion.length() > 60) {
                suggestion = suggestion.substring(0, 60);
            }
            deduplicated.add(suggestion);
            if (deduplicated.size() >= safeSize) {
                break;
            }
        }
        return new ArrayList<>(deduplicated);
    }

    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "productListCache", key = "'hot:products'")
    public List<ProductVO> getHotProducts() {
        return queryEnabledProductsByUpdatedAt(50);
    }

    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "productListCache", key = "'home:products'")
    public List<ProductVO> getHomeProducts() {
        return queryEnabledProductsByUpdatedAt(30);
    }

    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "productListCache", key = "'ranking:products'")
    public List<ProductVO> getRankingProducts() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
                .orderByDesc(Product::getStock)
                .orderByDesc(Product::getUpdatedAt)
                .last("LIMIT 50");
        return productConverter.toVOList(list(wrapper));
    }

    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "productStatsCache", key = "'home:snapshot'")
    public Map<String, Object> getHomeSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("totalCount", getTotalProductCount());
        snapshot.put("enabledCount", getEnabledProductCount());
        snapshot.put("hotProducts", getHotProducts());
        snapshot.put("homeProducts", getHomeProducts());
        snapshot.put("rankingProducts", getRankingProducts());
        return snapshot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean enableProduct(Long id) {
        boolean updated = updateProductStatus(id, 1);
        if (updated) {
            scheduleProductCacheEvictionAfterCommit(id);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disableProduct(Long id) {
        boolean updated = updateProductStatus(id, 0);
        if (updated) {
            scheduleProductCacheEvictionAfterCommit(id);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchEnableProducts(List<Long> ids) {
        boolean updated = batchUpdateProductStatus(ids, 1);
        if (updated) {
            scheduleProductCacheEvictionAfterCommit(ids);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDisableProducts(List<Long> ids) {
        boolean updated = batchUpdateProductStatus(ids, 0);
        if (updated) {
            scheduleProductCacheEvictionAfterCommit(ids);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStock(Long id, Integer stock) {
        validateId(id);
        if (stock == null || stock < 0) {
            throw new BusinessException("Invalid stock value");
        }

        requireExistingProduct(id);
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id).set(Product::getStock, stock);
        boolean updated = update(updateWrapper);
        if (updated) {
            scheduleProductCacheEvictionAfterCommit(id);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean increaseStock(Long id, Integer amount) {
        validateId(id);
        if (amount == null || amount <= 0) {
            throw new BusinessException("Increase amount must be positive");
        }

        requireExistingProduct(id);
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id)
                .setSql("stock_quantity = stock_quantity + " + amount);
        boolean updated = update(updateWrapper);
        if (updated) {
            scheduleProductCacheEvictionAfterCommit(id);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean decreaseStock(Long id, Integer amount) {
        validateId(id);
        if (amount == null || amount <= 0) {
            throw new BusinessException("Decrease amount must be positive");
        }

        Product existing = requireExistingProduct(id);
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id)
                .ge(Product::getStock, amount)
                .setSql("stock_quantity = stock_quantity - " + amount);
        boolean updated = update(updateWrapper);
        if (!updated) {
            int available = existing.getStock() == null ? 0 : existing.getStock();
            throw new ProductServiceException.StockInsufficientException(id, amount, available);
        }
        scheduleProductCacheEvictionAfterCommit(id);

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean checkStock(Long id, Integer amount) {
        validateId(id);
        if (amount == null || amount <= 0) {
            return true;
        }
        Product product = requireExistingProduct(id);
        return product.getStock() != null && product.getStock() >= amount;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalProductCount() {
        return count();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getEnabledProductCount() {
        return count(new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getDisabledProductCount() {
        return count(new LambdaQueryWrapper<Product>().eq(Product::getStatus, 0));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getProductCountByCategoryId(Long categoryId) {
        return count(new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, categoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getProductCountByBrandId(Long brandId) {
        if (brandId == null || brandId <= 0) {
            throw new BusinessException("Invalid brand id");
        }
        return count(new LambdaQueryWrapper<Product>().eq(Product::getBrandId, brandId));
    }

    @Override
    public void evictProductCache(Long id) {
        scheduleProductCacheEvictionAfterCommit(id);
    }

    @Override
    public void evictAllProductCache() {
        scheduleAllProductCacheEvictionAfterCommit();
    }

    @Override
    public void warmupProductCache(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            try {
                getProductById(id);
            } catch (Exception e) {
                log.warn("Warmup product cache failed: id={}", id, e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDTO createProductForFeign(ProductDTO productDTO) {
        ProductRequestDTO requestDTO = productConverter.dtoToRequestDTO(productDTO);
        Long id = createProduct(requestDTO);
        return productConverter.voToDTO(getProductById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductByIdForFeign(Long id) {
        return productConverter.voToDTO(getProductById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDTO updateProductForFeign(Long id, ProductDTO productDTO) {
        ProductRequestDTO requestDTO = productConverter.dtoToRequestDTO(productDTO);
        updateProduct(id, requestDTO);
        return productConverter.voToDTO(getProductById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productConverter.voListToDTOList(productConverter.toVOList(list()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByShopId(Long shopId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getShopId, shopId).orderByDesc(Product::getCreatedAt);
        return productConverter.voListToDTOList(productConverter.toVOList(list(wrapper)));
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("Invalid id");
        }
    }

    private List<ProductVO> queryEnabledProductsByUpdatedAt(int limit) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
                .orderByDesc(Product::getUpdatedAt)
                .last("LIMIT " + Math.max(10, limit));
        return productConverter.toVOList(list(wrapper));
    }

    private void validateProductRequest(ProductRequestDTO requestDTO, boolean create) {
        if (requestDTO == null) {
            throw new BusinessException("Product payload cannot be null");
        }
        if (create && !StringUtils.hasText(requestDTO.getName())) {
            throw new BusinessException("Product name cannot be blank");
        }
        if (create && requestDTO.getShopId() == null) {
            throw new BusinessException("Shop id is required");
        }
        if (requestDTO.getPrice() != null && requestDTO.getPrice().signum() < 0) {
            throw new BusinessException("Product price cannot be negative");
        }
        if (requestDTO.getStockQuantity() != null && requestDTO.getStockQuantity() < 0) {
            throw new BusinessException("Stock quantity cannot be negative");
        }
        if (requestDTO.getStatus() != null && requestDTO.getStatus() != 0 && requestDTO.getStatus() != 1) {
            throw new BusinessException("Product status must be 0 or 1");
        }
    }

    private void applyRequest(Product product, ProductRequestDTO requestDTO) {
        if (StringUtils.hasText(requestDTO.getName())) {
            product.setName(requestDTO.getName());
        }
        if (requestDTO.getPrice() != null) {
            product.setPrice(requestDTO.getPrice());
        }
        if (requestDTO.getStockQuantity() != null) {
            product.setStock(requestDTO.getStockQuantity());
        }
        if (requestDTO.getCategoryId() != null) {
            product.setCategoryId(requestDTO.getCategoryId());
        }
        if (requestDTO.getBrandId() != null) {
            product.setBrandId(requestDTO.getBrandId());
        }
        if (requestDTO.getStatus() != null) {
            product.setStatus(requestDTO.getStatus());
        } else if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (requestDTO.getShopId() != null) {
            product.setShopId(requestDTO.getShopId());
        }
    }

    private LambdaQueryWrapper<Product> buildQueryWrapper(ProductPageDTO pageDTO) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(pageDTO.getName())) {
            wrapper.like(Product::getName, pageDTO.getName());
        }
        if (pageDTO.getStatus() != null) {
            wrapper.eq(Product::getStatus, pageDTO.getStatus());
        }
        if (pageDTO.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, pageDTO.getCategoryId());
        }
        if (pageDTO.getBrandId() != null) {
            wrapper.eq(Product::getBrandId, pageDTO.getBrandId());
        }
        if (pageDTO.getShopId() != null) {
            wrapper.eq(Product::getShopId, pageDTO.getShopId());
        }
        if (pageDTO.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, pageDTO.getMinPrice());
        }
        if (pageDTO.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, pageDTO.getMaxPrice());
        }
        if (pageDTO.getMinStock() != null) {
            wrapper.ge(Product::getStock, pageDTO.getMinStock());
        }
        if (pageDTO.getMaxStock() != null) {
            wrapper.le(Product::getStock, pageDTO.getMaxStock());
        }

        wrapper.orderByDesc(Product::getCreatedAt);
        return wrapper;
    }

    private Boolean updateProductStatus(Long id, Integer status) {
        validateId(id);
        Product existing = requireExistingProduct(id);
        validateOperationPermissionForProduct(existing);

        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id).set(Product::getStatus, status);
        boolean updated = update(updateWrapper);
        return updated;
    }

    private Boolean batchUpdateProductStatus(List<Long> ids, Integer status) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }

        List<Product> existingProducts = listByIds(ids);
        validateBatchProductPermissions(existingProducts, ids);

        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Product::getId, ids).set(Product::getStatus, status);
        boolean updated = update(updateWrapper);
        if (!updated) {
            return false;
        }

        return true;
    }

    private Product requireExistingProduct(Long id) {
        Product product = getById(id);
        if (product == null) {
            throw new ProductServiceException.ProductNotFoundException(id);
        }
        return product;
    }

    private Shop requireExistingShop(Long shopId) {
        if (shopId == null || shopId <= 0) {
            throw new BusinessException("Invalid shop id");
        }
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            throw new BusinessException("Shop not found: " + shopId);
        }
        return shop;
    }

    private Category requireExistingCategory(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new ProductServiceException.CategoryNotFoundException("Invalid category id");
        }
        Category category = categoryService.getById(categoryId);
        if (category == null) {
            throw new ProductServiceException.CategoryNotFoundException(categoryId);
        }
        return category;
    }

    private void validateBatchProductPermissions(List<Product> existingProducts, List<Long> requestedIds) {
        if (CollectionUtils.isEmpty(existingProducts)) {
            throw new ProductServiceException.ProductNotFoundException("No product found in request");
        }
        if (existingProducts.size() != requestedIds.size()) {
            throw new ProductServiceException.ProductNotFoundException("Some products do not exist");
        }
        for (Product product : existingProducts) {
            validateOperationPermissionForProduct(product);
        }
    }

    private void validateOperationPermissionForProduct(Product product) {
        if (product == null || product.getId() == null) {
            throw new ProductServiceException.ProductNotFoundException("Product does not exist");
        }
        if (product.getShopId() == null) {
            throw new ProductServiceException.ProductPermissionException(
                    "Product does not contain a valid shop owner reference: productId=" + product.getId()
            );
        }
        Shop shop = requireExistingShop(product.getShopId());
        validateOperationPermissionForShop(shop.getMerchantId(), product.getId());
    }

    private void validateOperationPermissionForShop(Long merchantId, Long productId) {
        Authentication authentication = SecurityPermissionUtils.getCurrentAuthentication();
        if (!SecurityPermissionUtils.isAuthenticated(authentication)) {
            return;
        }
        if (SecurityPermissionUtils.hasAuthority(authentication, "SCOPE_internal_api")
                || SecurityPermissionUtils.isAdmin(authentication)) {
            return;
        }
        if (!SecurityPermissionUtils.isMerchant(authentication)) {
            throw new ProductServiceException.ProductPermissionException("Current user cannot manage products");
        }

        String currentUserId = SecurityPermissionUtils.getCurrentUserId(authentication);
        if (!StringUtils.hasText(currentUserId) || !currentUserId.equals(String.valueOf(merchantId))) {
            String targetIdText = productId == null ? "N/A" : String.valueOf(productId);
            throw new ProductServiceException.ProductPermissionException(
                    String.format("Merchant %s cannot manage product %s", currentUserId, targetIdText)
            );
        }
    }

    private void scheduleProductCacheEvictionAfterCommit(Long productId) {
        runAfterCommit(() -> productCacheProtectionService.evictProductCaches(productId));
    }

    private void scheduleProductCacheEvictionAfterCommit(List<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            scheduleAllProductCacheEvictionAfterCommit();
            return;
        }
        runAfterCommit(() -> productCacheProtectionService.evictProductCaches(productIds));
    }

    private void scheduleAllProductCacheEvictionAfterCommit() {
        runAfterCommit(productCacheProtectionService::evictAllProductCaches);
    }

    private void runAfterCommit(Runnable runnable) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            runnable.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
    }

}
