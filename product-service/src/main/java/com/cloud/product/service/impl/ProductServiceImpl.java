package com.cloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.api.stock.StockFeignClient;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.product.ProductDTO;
import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.domain.vo.stock.StockVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.result.PageResult;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;









@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
        implements ProductService {

    private final ProductConverter productConverter;
    private final ShopService shopService;
    private final CategoryService categoryService;
    private final StockFeignClient stockFeignClient;

    

    @Override
    @DistributedLock(
            key = "'product:create:' + #requestDTO.shopId + ':' + #requestDTO.name",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire product create lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @CachePut(cacheNames = "productCache", key = "#result",
            condition = "#result != null")
    public Long createProduct(ProductRequestDTO requestDTO) throws ProductServiceException {
        if (requestDTO == null || !StringUtils.hasText(requestDTO.getName())) {
            throw new BusinessException("鍟嗗搧淇℃伅涓嶈兘涓虹┖");
        }

        

        try {
            
            Product product = productConverter.requestDTOToEntity(requestDTO);

            
            if (product.getStatus() == null) {
                product.setStatus(0); 
            }

            
            boolean saved = save(product);
            if (!saved) {
                throw new BusinessException("鍟嗗搧淇濆瓨澶辫触");
            }

            
            return product.getId();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("鍒涘缓鍟嗗搧鏃跺彂鐢熸湭棰勬湡寮傚父锛屽晢鍝佸悕绉? {}", requestDTO.getName(), e);
            throw new BusinessException("鍒涘缓鍟嗗搧澶辫触: " + e.getMessage(), e);
        }
    }

    @Override
    @DistributedLock(
            key = "'product:update:' + #id",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire product update lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Caching(
            put = @CachePut(cacheNames = "productCache", key = "#id",
                    condition = "#result == true"),
            evict = {
                    @CacheEvict(cacheNames = "productListCache", allEntries = true),
                    @CacheEvict(cacheNames = "productStatsCache", allEntries = true)
            }
    )
    public Boolean updateProduct(Long id, ProductRequestDTO requestDTO) throws ProductServiceException {
        if (id == null || id <= 0) {
            throw new BusinessException("鍟嗗搧ID涓嶈兘涓虹┖鎴栧皬浜庣瓑浜?");
        }
        if (requestDTO == null || !StringUtils.hasText(requestDTO.getName())) {
            throw new BusinessException("鍟嗗搧淇℃伅涓嶈兘涓虹┖");
        }

        

        try {
            
            Product existingProduct = getById(id);
            if (existingProduct == null) {
                throw new EntityNotFoundException("鍟嗗搧", id);
            }

            
            Product updatedProduct = productConverter.requestDTOToEntity(requestDTO);
            updatedProduct.setId(id);
            
            updatedProduct.setCreatedAt(existingProduct.getCreatedAt());

            boolean updated = updateById(updatedProduct);
            if (!updated) {
                throw new BusinessException("鍟嗗搧鏇存柊澶辫触");
            }

            
            return true;

        } catch (EntityNotFoundException e) {
            throw e;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("鏇存柊鍟嗗搧鏃跺彂鐢熸湭棰勬湡寮傚父锛屽晢鍝両D: {}", id, e);
            throw new BusinessException("鏇存柊鍟嗗搧澶辫触: " + e.getMessage(), e);
        }
    }

    @Override
    @DistributedLock(
            key = "'product:delete:' + #id",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire product delete lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = {"productCache"}, key = "#id"),
                    @CacheEvict(cacheNames = "productListCache", allEntries = true),
                    @CacheEvict(cacheNames = "productStatsCache", allEntries = true)
            }
    )
    public Boolean deleteProduct(Long id) throws ProductServiceException {
        if (id == null || id <= 0) {
            throw new BusinessException("鍟嗗搧ID涓嶈兘涓虹┖鎴栧皬浜庣瓑浜?");
        }

        

        try {
            
            Product product = getById(id);
            if (product == null) {
                throw new EntityNotFoundException("鍟嗗搧", id);
            }

            boolean deleted = removeById(id);
            if (!deleted) {
                throw new BusinessException("鍟嗗搧鍒犻櫎澶辫触");
            }

            
            return true;

        } catch (EntityNotFoundException e) {
            throw e;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("鍒犻櫎鍟嗗搧鏃跺彂鐢熸湭棰勬湡寮傚父锛屽晢鍝両D: {}", id, e);
            throw new BusinessException("鍒犻櫎鍟嗗搧澶辫触: " + e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'product:batch:delete:' + #ids.toString()",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "鎵归噺鍒犻櫎鍟嗗搧鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"productCache", "productListCache", "productStatsCache"},
            allEntries = true)
    public Boolean batchDeleteProducts(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException("鍟嗗搧ID鍒楄〃涓嶈兘涓虹┖");
        }

        if (ids.size() > 100) {
            throw new BusinessException("鎵归噺鎿嶄綔鏁伴噺涓嶈兘瓒呰繃100");
        }

        

        try {
            
            List<Product> productsToDelete = listByIds(ids);
            if (productsToDelete.size() != ids.size()) {
                log.warn("閮ㄥ垎鍟嗗搧涓嶅瓨鍦紝璇锋眰鍒犻櫎: {}, 瀹為檯鎵惧埌: {}", ids.size(), productsToDelete.size());
            }

            boolean deleted = removeBatchByIds(ids);
            if (!deleted) {
                throw new BusinessException("鎵归噺鍒犻櫎鍟嗗搧澶辫触");
            }

            
            return true;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("鎵归噺鍒犻櫎鍟嗗搧鏃跺彂鐢熸湭棰勬湡寮傚父锛屽晢鍝佹暟閲? {}", ids.size(), e);
            throw new BusinessException("鎵归噺鍒犻櫎鍟嗗搧澶辫触: " + e.getMessage(), e);
        }
    }

    

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productCache", key = "#id",
            condition = "#id != null")
    public ProductVO getProductById(Long id) throws ProductServiceException.ProductNotFoundException {
        log.debug("鑾峰彇鍟嗗搧璇︽儏: {}", id);

        Product product = getById(id);
        if (product == null) {
            throw new ProductServiceException.ProductNotFoundException(id);
        }

        return productConverter.toVO(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productCache",
            key = "'batch:' + #ids.toString()",
            condition = "!T(org.springframework.util.CollectionUtils).isEmpty(#ids)")
    public List<ProductVO> getProductsByIds(List<Long> ids) {
        log.debug("鎵归噺鑾峰彇鍟嗗搧: {}", ids);

        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }

        List<Product> products = listByIds(ids);
        return productConverter.toVOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productListCache",
            key = "'page:' + #pageDTO.current + ':' + #pageDTO.size + ':' + (#pageDTO.name ?: 'null') + ':' + (#pageDTO.status ?: 'null')")
    public PageResult<ProductVO> getProductsPage(ProductPageDTO pageDTO) {
        log.debug("鍒嗛〉鏌ヨ鍟嗗搧: {}", pageDTO);

        Page<Product> page = new Page<>(pageDTO.getCurrent(), pageDTO.getSize());
        LambdaQueryWrapper<Product> queryWrapper = buildQueryWrapper(pageDTO);

        Page<Product> productPage = page(page, queryWrapper);
        List<ProductVO> productVOs = productConverter.toVOList(productPage.getRecords());

        return PageResult.of(productVOs, productPage.getTotal(), pageDTO.getCurrent(), pageDTO.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productListCache",
            key = "'category:' + #categoryId + ':' + (#status ?: 'null')")
    public List<ProductVO> getProductsByCategoryId(Long categoryId, Integer status) {
        log.debug("鏍规嵁鍒嗙被鏌ヨ鍟嗗搧: categoryId={}, status={}", categoryId, status);

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getCategoryId, categoryId);
        if (status != null) {
            queryWrapper.eq(Product::getStatus, status);
        }
        queryWrapper.orderByDesc(Product::getCreatedAt);

        List<Product> products = list(queryWrapper);
        return productConverter.toVOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productListCache",
            key = "'brand:' + #brandId + ':' + (#status ?: 'null')")
    public List<ProductVO> getProductsByBrandId(Long brandId, Integer status) {
        log.debug("鏍规嵁鍝佺墝鏌ヨ鍟嗗搧: brandId={}, status={}", brandId, status);

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getBrandId, brandId);
        if (status != null) {
            queryWrapper.eq(Product::getStatus, status);
        }
        queryWrapper.orderByDesc(Product::getCreatedAt);

        List<Product> products = list(queryWrapper);
        return productConverter.toVOList(products);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productListCache",
            key = "'search:' + #name + ':' + (#status ?: 'null')")
    public List<ProductVO> searchProductsByName(String name, Integer status) {
        log.debug("鎼滅储鍟嗗搧: name={}, status={}", name, status);

        if (!StringUtils.hasText(name)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Product::getName, name);
        if (status != null) {
            queryWrapper.eq(Product::getStatus, status);
        }
        queryWrapper.orderByDesc(Product::getCreatedAt);

        List<Product> products = list(queryWrapper);
        return productConverter.toVOList(products);
    }

    

    @Override
    @DistributedLock(
            key = "'product:enable:' + #id",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire product enable lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            put = @CachePut(cacheNames = "productCache", key = "#id"),
            evict = {
                    @CacheEvict(cacheNames = "productListCache", allEntries = true),
                    @CacheEvict(cacheNames = "productStatsCache", allEntries = true)
            }
    )
    public Boolean enableProduct(Long id) throws ProductServiceException {
        return updateProductStatus(id, 1, "涓婃灦");
    }

    @Override
    @DistributedLock(
            key = "'product:disable:' + #id",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire product disable lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            put = @CachePut(cacheNames = "productCache", key = "#id"),
            evict = {
                    @CacheEvict(cacheNames = "productListCache", allEntries = true),
                    @CacheEvict(cacheNames = "productStatsCache", allEntries = true)
            }
    )
    public Boolean disableProduct(Long id) throws ProductServiceException {
        return updateProductStatus(id, 0, "涓嬫灦");
    }

    @Override
    @DistributedLock(
            key = "'product:batch:enable:' + #ids.toString()",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "鎵归噺涓婃灦鍟嗗搧鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"productCache", "productListCache", "productStatsCache"},
            allEntries = true)
    public Boolean batchEnableProducts(List<Long> ids) {
        return batchUpdateProductStatus(ids, 1, "鎵归噺涓婃灦");
    }

    @Override
    @DistributedLock(
            key = "'product:batch:disable:' + #ids.toString()",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "鎵归噺涓嬫灦鍟嗗搧鎿嶄綔鑾峰彇閿佸け璐?
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"productCache", "productListCache", "productStatsCache"},
            allEntries = true)
    public Boolean batchDisableProducts(List<Long> ids) {
        return batchUpdateProductStatus(ids, 0, "鎵归噺涓嬫灦");
    }

    

    @Override
    @DistributedLock(
            key = "'product:stock:update:' + #id",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire product stock update lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            put = @CachePut(cacheNames = "productCache", key = "#id"),
            evict = @CacheEvict(cacheNames = {"productStatsCache"}, allEntries = true)
    )
    public Boolean updateStock(Long id, Integer stock) {
        

        if (stock < 0) {
            throw new BusinessException("搴撳瓨鏁伴噺涓嶈兘涓鸿礋鏁?);
        }

        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id).set(Product::getStock, stock);

        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new BusinessException("鏇存柊搴撳瓨澶辫触");
        }

        return true;
    }

    @Override
    @DistributedLock(
            key = "'product:stock:increase:' + #id",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire product stock increase lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            put = @CachePut(cacheNames = "productCache", key = "#id"),
            evict = @CacheEvict(cacheNames = {"productStatsCache"}, allEntries = true)
    )
    public Boolean increaseStock(Long id, Integer quantity) {
        

        if (quantity <= 0) {
            throw new BusinessException("澧炲姞鏁伴噺蹇呴』澶т簬0");
        }

        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id)
                .setSql("stock = stock + " + quantity);

        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new BusinessException("澧炲姞搴撳瓨澶辫触");
        }

        return true;
    }

    @Override
    @DistributedLock(
            key = "'product:stock:decrease:' + #id",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire product stock decrease lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            put = @CachePut(cacheNames = "productCache", key = "#id"),
            evict = @CacheEvict(cacheNames = {"productStatsCache"}, allEntries = true)
    )
    public Boolean decreaseStock(Long id, Integer quantity) {
        

        if (quantity <= 0) {
            throw new BusinessException("鍑忓皯鏁伴噺蹇呴』澶т簬0");
        }

        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id)
                .ge(Product::getStock, quantity) 
                .setSql("stock = stock - " + quantity);

        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new BusinessException("搴撳瓨涓嶈冻鎴栧噺灏戝け璐?);
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productCache", key = "'stock:' + #id + ':' + #quantity")
    public Boolean checkStock(Long id, Integer quantity) {
        log.debug("妫€鏌ュ晢鍝佸簱瀛? ID={}, Quantity={}", id, quantity);

        if (quantity <= 0) {
            return true;
        }

        Product product = getById(id);
        if (product == null) {
            return false;
        }

        return product.getStock() >= quantity;
    }

    

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productStatsCache", key = "'total'")
    public Long getTotalProductCount() {
        return count();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productStatsCache", key = "'enabled'")
    public Long getEnabledProductCount() {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, 1);
        return count(queryWrapper);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productStatsCache", key = "'disabled'")
    public Long getDisabledProductCount() {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, 0);
        return count(queryWrapper);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productStatsCache", key = "'category:' + #categoryId")
    public Long getProductCountByCategoryId(Long categoryId) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getCategoryId, categoryId);
        return count(queryWrapper);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productStatsCache", key = "'brand:' + #brandId")
    public Long getProductCountByBrandId(Long brandId) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getBrandId, brandId);
        return count(queryWrapper);
    }

    

    @Override
    @CacheEvict(cacheNames = {"productCache"}, key = "#id")
    public void evictProductCache(Long id) {
        
    }

    @Override
    @CacheEvict(cacheNames = {"productCache", "productListCache", "productStatsCache"},
            allEntries = true)
    public void evictAllProductCache() {
    }

    @Override
    public void warmupProductCache(List<Long> ids) {
        

        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        
        ids.forEach(this::getProductById);

        
    }

    

    






    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDTO createProductForFeign(ProductDTO productDTO) {
        

        
        ProductRequestDTO requestDTO = productConverter.dtoToRequestDTO(productDTO);
        Long productId = createProduct(requestDTO);

        
        ProductVO productVO = getProductById(productId);
        return productConverter.voToDTO(productVO);
    }

    






    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productCache", key = "'feign:' + #id",
            condition = "#id != null")
    public ProductDTO getProductByIdForFeign(Long id) {
        log.debug("鑾峰彇鍟嗗搧璇︽儏锛團eign): {}", id);
        try {
            ProductVO productVO = getProductById(id);
            return productConverter.voToDTO(productVO);
        } catch (ProductServiceException.ProductNotFoundException e) {
            log.warn("鍟嗗搧涓嶅瓨鍦紙Feign锛? {}", id);
            throw new EntityNotFoundException("鍟嗗搧", id);
        }
    }

    







    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDTO updateProductForFeign(Long id, ProductDTO productDTO) {
        

        
        ProductRequestDTO requestDTO = productConverter.dtoToRequestDTO(productDTO);
        Boolean success = updateProduct(id, requestDTO);

        if (!Boolean.TRUE.equals(success)) {
            throw new BusinessException("鏇存柊鍟嗗搧澶辫触");
        }

        ProductVO productVO = getProductById(id);
        return productConverter.voToDTO(productVO);
    }

    





    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productListCache", key = "'all'")
    public List<ProductDTO> getAllProducts() {
        log.debug("鑾峰彇鎵€鏈夊晢鍝侊紙Feign)");

        List<Product> products = list();
        List<ProductVO> productVOs = productConverter.toVOList(products);
        return productConverter.voListToDTOList(productVOs);
    }

    






    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productListCache",
            key = "'shop:' + #shopId")
    public List<ProductDTO> getProductsByShopId(Long shopId) {
        log.debug("鏍规嵁搴楅摵ID鑾峰彇鍟嗗搧鍒楄〃锛團eign): {}", shopId);

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getShopId, shopId)
                .eq(Product::getStatus, 1) 
                .orderByDesc(Product::getCreatedAt);

        List<Product> products = list(queryWrapper);
        List<ProductVO> productVOs = productConverter.toVOList(products);
        return productConverter.voListToDTOList(productVOs);
    }

    

    


    private LambdaQueryWrapper<Product> buildQueryWrapper(ProductPageDTO pageDTO) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        
        if (StringUtils.hasText(pageDTO.getName())) {
            queryWrapper.like(Product::getName, pageDTO.getName());
        }

        
        if (pageDTO.getStatus() != null) {
            queryWrapper.eq(Product::getStatus, pageDTO.getStatus());
        }

        
        if (pageDTO.getCategoryId() != null) {
            queryWrapper.eq(Product::getCategoryId, pageDTO.getCategoryId());
        }

        
        if (StringUtils.hasText(pageDTO.getCategoryName())) {
            queryWrapper.like(Product::getCategoryName, pageDTO.getCategoryName());
        }

        
        if (pageDTO.getBrandId() != null) {
            queryWrapper.eq(Product::getBrandId, pageDTO.getBrandId());
        }

        
        if (StringUtils.hasText(pageDTO.getBrandName())) {
            queryWrapper.like(Product::getBrandName, pageDTO.getBrandName());
        }

        
        if (pageDTO.getMinPrice() != null) {
            queryWrapper.ge(Product::getPrice, pageDTO.getMinPrice());
        }
        if (pageDTO.getMaxPrice() != null) {
            queryWrapper.le(Product::getPrice, pageDTO.getMaxPrice());
        }

        
        if (pageDTO.getMinStock() != null) {
            queryWrapper.ge(Product::getStock, pageDTO.getMinStock());
        }
        if (pageDTO.getMaxStock() != null) {
            queryWrapper.le(Product::getStock, pageDTO.getMaxStock());
        }

        
        applySorting(queryWrapper, pageDTO);

        return queryWrapper;
    }

    


    private void applySorting(LambdaQueryWrapper<Product> queryWrapper, ProductPageDTO pageDTO) {
        
        if ("ASC".equalsIgnoreCase(pageDTO.getPriceSort())) {
            queryWrapper.orderByAsc(Product::getPrice);
        } else if ("DESC".equalsIgnoreCase(pageDTO.getPriceSort())) {
            queryWrapper.orderByDesc(Product::getPrice);
        }

        
        if ("ASC".equalsIgnoreCase(pageDTO.getStockSort())) {
            queryWrapper.orderByAsc(Product::getStock);
        } else if ("DESC".equalsIgnoreCase(pageDTO.getStockSort())) {
            queryWrapper.orderByDesc(Product::getStock);
        }

        
        if ("ASC".equalsIgnoreCase(pageDTO.getCreateTimeSort())) {
            queryWrapper.orderByAsc(Product::getCreatedAt);
        } else if ("DESC".equalsIgnoreCase(pageDTO.getCreateTimeSort())) {
            queryWrapper.orderByDesc(Product::getCreatedAt);
        }

        
        if ("ASC".equalsIgnoreCase(pageDTO.getUpdateTimeSort())) {
            queryWrapper.orderByAsc(Product::getUpdatedAt);
        } else if ("DESC".equalsIgnoreCase(pageDTO.getUpdateTimeSort())) {
            queryWrapper.orderByDesc(Product::getUpdatedAt);
        }

        
        if (!StringUtils.hasText(pageDTO.getPriceSort()) &&
                !StringUtils.hasText(pageDTO.getStockSort()) &&
                !StringUtils.hasText(pageDTO.getCreateTimeSort()) &&
                !StringUtils.hasText(pageDTO.getUpdateTimeSort())) {
            queryWrapper.orderByDesc(Product::getCreatedAt);
        }
    }

    


    private Boolean updateProductStatus(Long id, Integer status, String operation) {
        if (id == null || id <= 0) {
            throw new BusinessException("鍟嗗搧ID涓嶈兘涓虹┖鎴栧皬浜庣瓑浜?");
        }

        

        try {
            
            Product product = getById(id);
            if (product == null) {
                throw new EntityNotFoundException("鍟嗗搧", id);
            }

            
            Integer beforeStatus = product.getStatus();
            if (product.getStatus().equals(status)) {
                log.warn("鍟嗗搧宸茬粡鏄瘂}鐘舵€?{}", operation, id);
                return true;
            }

            LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Product::getId, id).set(Product::getStatus, status);

            boolean updated = update(updateWrapper);
            if (!updated) {
                throw new BusinessException(operation + "鍟嗗搧澶辫触");
            }

            
            return true;

        } catch (EntityNotFoundException e) {
            throw e;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("{}鍟嗗搧鏃跺彂鐢熸湭棰勬湡寮傚父锛屽晢鍝両D: {}", operation, id, e);
            throw new BusinessException(operation + "鍟嗗搧澶辫触: " + e.getMessage(), e);
        }
    }

    


    private Boolean batchUpdateProductStatus(List<Long> ids, Integer status, String operation) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException("鍟嗗搧ID鍒楄〃涓嶈兘涓虹┖");
        }

        if (ids.size() > 100) {
            throw new BusinessException("鎵归噺鎿嶄綔鏁伴噺涓嶈兘瓒呰繃100");
        }

        

        try {
            LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(Product::getId, ids).set(Product::getStatus, status);

            boolean updated = update(updateWrapper);
            if (!updated) {
                throw new BusinessException(operation + "澶辫触");
            }

            
            return true;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("{}鏃跺彂鐢熸湭棰勬湡寮傚父锛屽晢鍝佹暟閲? {}", operation, ids.size(), e);
            throw new BusinessException(operation + "澶辫触: " + e.getMessage(), e);
        }
    }

    


    private String getShopName(Long shopId) {
        if (shopId == null) {
            return "鏈煡搴楅摵";
        }

        try {
            Shop shop = shopService.getById(shopId);
            return shop != null ? shop.getShopName() : "搴楅摵" + shopId;
        } catch (Exception e) {
            log.warn("鑾峰彇搴楅摵鍚嶇О澶辫触锛屽簵閾篒D锛歿}锛屼娇鐢ㄩ粯璁ゅ悕绉?, shopId, e);
            return "搴楅摵" + shopId;
        }
    }

    


    private Integer getStockQuantity(Long productId) {
        if (productId == null) {
            return 0;
        }

        try {
            
            StockVO stockVO = stockFeignClient.getStockByProductId(productId);
            if (stockVO != null && stockVO.getStockQuantity() != null) {
                return stockVO.getStockQuantity();
            }

            
            Product product = getById(productId);
            return product != null ? product.getStock() : 0;

        } catch (Exception e) {
            log.warn("鑾峰彇搴撳瓨鏁伴噺澶辫触锛屽晢鍝両D锛歿}锛屽皾璇曚粠鍟嗗搧琛ㄨ幏鍙?, productId, e);
            try {
                Product product = getById(productId);
                return product != null ? product.getStock() : 0;
            } catch (Exception ex) {
                log.error("浠庡晢鍝佽〃鑾峰彇搴撳瓨涔熷け璐ワ紝鍟嗗搧ID锛歿}", productId, ex);
                return 0;
            }
        }
    }

    


    private String getCategoryName(Long categoryId) {
        if (categoryId == null) {
            return "鏈垎绫?;
        }

        try {
            Category category = categoryService.getById(categoryId);
            return category != null ? category.getName() : "鍒嗙被" + categoryId;
        } catch (Exception e) {
            log.warn("鑾峰彇鍒嗙被鍚嶇О澶辫触锛屽垎绫籌D锛歿}锛屼娇鐢ㄩ粯璁ゅ悕绉?, categoryId, e);
            return "鍒嗙被" + categoryId;
        }
    }

    




    private String getBrandName(Long brandId) {
        if (brandId == null) {
            return "鏈煡鍝佺墝";
        }

        try {
            
            LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Product::getBrandId, brandId)
                    .isNotNull(Product::getBrandName)
                    .ne(Product::getBrandName, "")
                    .last("LIMIT 1");

            Product product = getOne(queryWrapper);
            if (product != null && StringUtils.hasText(product.getBrandName())) {
                return product.getBrandName();
            }

            
            return "鍝佺墝" + brandId;

        } catch (Exception e) {
            log.warn("鑾峰彇鍝佺墝鍚嶇О澶辫触锛屽搧鐗孖D锛歿}锛屼娇鐢ㄩ粯璁ゅ悕绉?, brandId, e);
            return "鍝佺墝" + brandId;
        }
    }

}





