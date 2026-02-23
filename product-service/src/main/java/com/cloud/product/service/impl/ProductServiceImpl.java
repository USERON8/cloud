package com.cloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.api.stock.StockFeignClient;
import com.cloud.common.domain.dto.product.ProductDTO;
import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.PageResult;
import com.cloud.product.converter.ProductConverter;
import com.cloud.product.exception.ProductServiceException;
import com.cloud.product.mapper.ProductMapper;
import com.cloud.product.messaging.ProductSearchSyncProducer;
import com.cloud.product.module.dto.ProductPageDTO;
import com.cloud.product.module.entity.Product;
import com.cloud.product.service.CategoryService;
import com.cloud.product.service.ProductService;
import com.cloud.product.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductConverter productConverter;
    private final ShopService shopService;
    private final CategoryService categoryService;
    private final StockFeignClient stockFeignClient;
    private final ProductSearchSyncProducer productSearchSyncProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(ProductRequestDTO requestDTO) {
        validateProductRequest(requestDTO, true);

        Product product = new Product();
        applyRequest(product, requestDTO);

        boolean saved = save(product);
        if (!saved) {
            throw new BusinessException("Create product failed");
        }
        Product persisted = getById(product.getId());
        if (persisted == null) {
            throw new BusinessException("Created product not found");
        }
        if (!productSearchSyncProducer.sendProductCreated(persisted)) {
            throw new BusinessException("Send product-created search sync event failed");
        }
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProduct(Long id, ProductRequestDTO requestDTO) {
        validateId(id);
        validateProductRequest(requestDTO, false);

        Product product = getById(id);
        if (product == null) {
            throw new ProductServiceException.ProductNotFoundException(id);
        }

        applyRequest(product, requestDTO);
        boolean updated = updateById(product);
        if (!updated) {
            return false;
        }
        Product persisted = getById(id);
        if (persisted == null) {
            throw new ProductServiceException.ProductNotFoundException(id);
        }
        if (!productSearchSyncProducer.sendProductUpdated(persisted)) {
            throw new BusinessException("Send product-updated search sync event failed");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteProduct(Long id) {
        validateId(id);
        Product existing = getById(id);
        if (existing == null) {
            throw new ProductServiceException.ProductNotFoundException(id);
        }
        boolean deleted = removeById(id);
        if (!deleted) {
            return false;
        }
        if (!productSearchSyncProducer.sendProductDeleted(id)) {
            throw new BusinessException("Send product-deleted search sync event failed");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDeleteProducts(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }
        List<Product> existingProducts = listByIds(ids);
        boolean deleted = removeByIds(ids);
        if (!deleted) {
            return false;
        }
        for (Product product : existingProducts) {
            if (product == null || product.getId() == null) {
                continue;
            }
            if (!productSearchSyncProducer.sendProductDeleted(product.getId())) {
                throw new BusinessException("Send batch product-deleted search sync event failed");
            }
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productCache", key = "#id", condition = "#id != null")
    public ProductVO getProductById(Long id) {
        validateId(id);
        Product product = getById(id);
        if (product == null) {
            throw new ProductServiceException.ProductNotFoundException(id);
        }
        return productConverter.toVO(product);
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
        log.warn("Brand query is ignored because products schema has no brand_id column: brandId={}, status={}",
                brandId, status);
        return new ArrayList<>();
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
    @Transactional(rollbackFor = Exception.class)
    public Boolean enableProduct(Long id) {
        return updateProductStatus(id, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disableProduct(Long id) {
        return updateProductStatus(id, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchEnableProducts(List<Long> ids) {
        return batchUpdateProductStatus(ids, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDisableProducts(List<Long> ids) {
        return batchUpdateProductStatus(ids, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStock(Long id, Integer stock) {
        validateId(id);
        if (stock == null || stock < 0) {
            throw new BusinessException("Invalid stock value");
        }
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id).set(Product::getStock, stock);
        return update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean increaseStock(Long id, Integer amount) {
        validateId(id);
        if (amount == null || amount <= 0) {
            throw new BusinessException("Increase amount must be positive");
        }
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id).setSql("stock_quantity = stock_quantity + " + amount);
        return update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean decreaseStock(Long id, Integer amount) {
        validateId(id);
        if (amount == null || amount <= 0) {
            throw new BusinessException("Decrease amount must be positive");
        }
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id)
                .ge(Product::getStock, amount)
                .setSql("stock_quantity = stock_quantity - " + amount);
        return update(updateWrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean checkStock(Long id, Integer amount) {
        validateId(id);
        if (amount == null || amount <= 0) {
            return true;
        }
        Product product = getById(id);
        if (product == null) {
            throw new ProductServiceException.ProductNotFoundException(id);
        }
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
        log.warn("Brand count query is ignored because products schema has no brand_id column: brandId={}", brandId);
        return 0L;
    }

    @Override
    @CacheEvict(cacheNames = "productCache", key = "#id")
    public void evictProductCache(Long id) {
    }

    @Override
    @CacheEvict(cacheNames = {"productCache", "productListCache", "productStatsCache"}, allEntries = true)
    public void evictAllProductCache() {
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

    @Override
    @Transactional(readOnly = true)
    public Integer syncProductsToSearch(Integer pageSize, Integer status) {
        int size = (pageSize == null || pageSize <= 0) ? 200 : Math.min(pageSize, 1000);
        long current = 1L;
        int sentCount = 0;

        while (true) {
            Page<Product> page = new Page<>(current, size);
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            if (status != null) {
                wrapper.eq(Product::getStatus, status);
            }
            wrapper.orderByAsc(Product::getId);

            Page<Product> result = page(page, wrapper);
            List<Product> records = result.getRecords();
            if (CollectionUtils.isEmpty(records)) {
                break;
            }

            for (Product product : records) {
                if (product == null || product.getId() == null) {
                    continue;
                }
                if (!productSearchSyncProducer.sendProductUpdated(product)) {
                    throw new BusinessException("Send product full-sync event failed, productId=" + product.getId());
                }
                sentCount++;
            }

            if (current >= result.getPages()) {
                break;
            }
            current++;
        }

        log.info("Product full sync events sent: count={}, pageSize={}, status={}", sentCount, size, status);
        return sentCount;
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("Invalid id");
        }
    }

    private void validateProductRequest(ProductRequestDTO requestDTO, boolean create) {
        if (requestDTO == null) {
            throw new BusinessException("Product payload cannot be null");
        }
        if (create && !StringUtils.hasText(requestDTO.getName())) {
            throw new BusinessException("Product name cannot be blank");
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
            log.warn("Ignore brand filter because products schema has no brand_id column: brandId={}",
                    pageDTO.getBrandId());
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
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id).set(Product::getStatus, status);
        return update(updateWrapper);
    }

    private Boolean batchUpdateProductStatus(List<Long> ids, Integer status) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Product::getId, ids).set(Product::getStatus, status);
        return update(updateWrapper);
    }
}
