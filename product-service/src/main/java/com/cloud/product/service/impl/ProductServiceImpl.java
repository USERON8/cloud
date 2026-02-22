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

/**
 * 商品服务实现
 * 实现商品相关的业务操作，使用多级缓存提升性能
 * 遵循用户服务标准，包含事务管理和缓存管理
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
        implements ProductService {

    private final ProductConverter productConverter;
    private final ShopService shopService;
    private final CategoryService categoryService;
    private final StockFeignClient stockFeignClient;

    // ================= 基础CRUD操作 =================

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
            throw new BusinessException("商品信息不能为空");
        }

        log.info("创建商品: {}", requestDTO.getName());

        try {
            // 转换为实�?
            Product product = productConverter.requestDTOToEntity(requestDTO);

            // 设置默认状�?
            if (product.getStatus() == null) {
                product.setStatus(0); // 默认下架状�?
            }

            // 保存商品
            boolean saved = save(product);
            if (!saved) {
                throw new BusinessException("商品保存失败");
            }

            log.info("商品创建成功, ID: {}", product.getId());
            return product.getId();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建商品时发生未预期异常，商品名�? {}", requestDTO.getName(), e);
            throw new BusinessException("创建商品失败: " + e.getMessage(), e);
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
            throw new BusinessException("商品ID不能为空或小于等�?");
        }
        if (requestDTO == null || !StringUtils.hasText(requestDTO.getName())) {
            throw new BusinessException("商品信息不能为空");
        }

        log.info("更新商品: ID={}, Name={}", id, requestDTO.getName());

        try {
            // 检查商品是否存�?
            Product existingProduct = getById(id);
            if (existingProduct == null) {
                throw new EntityNotFoundException("商品", id);
            }

            // 更新商品信息
            Product updatedProduct = productConverter.requestDTOToEntity(requestDTO);
            updatedProduct.setId(id);
            // 保持创建时间不变
            updatedProduct.setCreatedAt(existingProduct.getCreatedAt());

            boolean updated = updateById(updatedProduct);
            if (!updated) {
                throw new BusinessException("商品更新失败");
            }

            log.info("商品更新成功: {}", id);
            return true;

        } catch (EntityNotFoundException e) {
            throw e;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新商品时发生未预期异常，商品ID: {}", id, e);
            throw new BusinessException("更新商品失败: " + e.getMessage(), e);
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
            throw new BusinessException("商品ID不能为空或小于等�?");
        }

        log.info("删除商品: {}", id);

        try {
            // 检查商品是否存�?
            Product product = getById(id);
            if (product == null) {
                throw new EntityNotFoundException("商品", id);
            }

            boolean deleted = removeById(id);
            if (!deleted) {
                throw new BusinessException("商品删除失败");
            }

            log.info("商品删除成功: {}", id);
            return true;

        } catch (EntityNotFoundException e) {
            throw e;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除商品时发生未预期异常，商品ID: {}", id, e);
            throw new BusinessException("删除商品失败: " + e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasMerchantAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")
    @DistributedLock(
            key = "'product:batch:delete:' + #ids.toString()",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "批量删除商品操作获取锁失�?
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"productCache", "productListCache", "productStatsCache"},
            allEntries = true)
    public Boolean batchDeleteProducts(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException("商品ID列表不能为空");
        }

        if (ids.size() > 100) {
            throw new BusinessException("批量操作数量不能超过100");
        }

        log.info("批量删除商品: {}", ids);

        try {
            // 先获取要删除的商品信�?
            List<Product> productsToDelete = listByIds(ids);
            if (productsToDelete.size() != ids.size()) {
                log.warn("部分商品不存在，请求删除: {}, 实际找到: {}", ids.size(), productsToDelete.size());
            }

            boolean deleted = removeBatchByIds(ids);
            if (!deleted) {
                throw new BusinessException("批量删除商品失败");
            }

            log.info("批量删除商品成功, 数量: {}", ids.size());
            return true;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量删除商品时发生未预期异常，商品数�? {}", ids.size(), e);
            throw new BusinessException("批量删除商品失败: " + e.getMessage(), e);
        }
    }

    // ================= 查询操作 =================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productCache", key = "#id",
            condition = "#id != null")
    public ProductVO getProductById(Long id) throws ProductServiceException.ProductNotFoundException {
        log.debug("获取商品详情: {}", id);

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
        log.debug("批量获取商品: {}", ids);

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
        log.debug("分页查询商品: {}", pageDTO);

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
        log.debug("根据分类查询商品: categoryId={}, status={}", categoryId, status);

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
        log.debug("根据品牌查询商品: brandId={}, status={}", brandId, status);

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
        log.debug("搜索商品: name={}, status={}", name, status);

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

    // ================= 状态管�?=================

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
        return updateProductStatus(id, 1, "上架");
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
        return updateProductStatus(id, 0, "下架");
    }

    @Override
    @DistributedLock(
            key = "'product:batch:enable:' + #ids.toString()",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "批量上架商品操作获取锁失�?
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"productCache", "productListCache", "productStatsCache"},
            allEntries = true)
    public Boolean batchEnableProducts(List<Long> ids) {
        return batchUpdateProductStatus(ids, 1, "批量上架");
    }

    @Override
    @DistributedLock(
            key = "'product:batch:disable:' + #ids.toString()",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "批量下架商品操作获取锁失�?
    )
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"productCache", "productListCache", "productStatsCache"},
            allEntries = true)
    public Boolean batchDisableProducts(List<Long> ids) {
        return batchUpdateProductStatus(ids, 0, "批量下架");
    }

    // ================= 库存管理 =================

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
        log.info("更新商品库存: ID={}, Stock={}", id, stock);

        if (stock < 0) {
            throw new BusinessException("库存数量不能为负�?);
        }

        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id).set(Product::getStock, stock);

        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new BusinessException("更新库存失败");
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
        log.info("增加商品库存: ID={}, Quantity={}", id, quantity);

        if (quantity <= 0) {
            throw new BusinessException("增加数量必须大于0");
        }

        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id)
                .setSql("stock = stock + " + quantity);

        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new BusinessException("增加库存失败");
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
        log.info("减少商品库存: ID={}, Quantity={}", id, quantity);

        if (quantity <= 0) {
            throw new BusinessException("减少数量必须大于0");
        }

        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, id)
                .ge(Product::getStock, quantity) // 保证库存充足
                .setSql("stock = stock - " + quantity);

        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new BusinessException("库存不足或减少失�?);
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productCache", key = "'stock:' + #id + ':' + #quantity")
    public Boolean checkStock(Long id, Integer quantity) {
        log.debug("检查商品库�? ID={}, Quantity={}", id, quantity);

        if (quantity <= 0) {
            return true;
        }

        Product product = getById(id);
        if (product == null) {
            return false;
        }

        return product.getStock() >= quantity;
    }

    // ================= 统计分析 =================

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

    // ================= 缓存管理 =================

    @Override
    @CacheEvict(cacheNames = {"productCache"}, key = "#id")
    public void evictProductCache(Long id) {
        log.info("清除商品缓存: {}", id);
    }

    @Override
    @CacheEvict(cacheNames = {"productCache", "productListCache", "productStatsCache"},
            allEntries = true)
    public void evictAllProductCache() {
        log.info("清除所有商品缓�?);
    }

    @Override
    public void warmupProductCache(List<Long> ids) {
        log.info("预热商品缓存: {}", ids);

        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // 预热商品详情缓存
        ids.forEach(this::getProductById);

        log.info("商品缓存预热完成, 数量: {}", ids.size());
    }

    // ================= Feign客户端接口方法实�?=================

    /**
     * 创建商品（Feign客户端接口）
     * 供其他服务通过Feign调用创建商品
     *
     * @param productDTO 商品DTO
     * @return 创建的商品DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDTO createProductForFeign(ProductDTO productDTO) {
        log.info("创建商品（Feign): {}", productDTO.getName());

        // 转换为RequestDTO
        ProductRequestDTO requestDTO = productConverter.dtoToRequestDTO(productDTO);
        Long productId = createProduct(requestDTO);

        // 返回创建的商品信�?
        ProductVO productVO = getProductById(productId);
        return productConverter.voToDTO(productVO);
    }

    /**
     * 根据ID获取商品（Feign客户端接口）
     * 供其他服务通过Feign调用获取商品信息
     *
     * @param id 商品ID
     * @return 商品DTO
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productCache", key = "'feign:' + #id",
            condition = "#id != null")
    public ProductDTO getProductByIdForFeign(Long id) {
        log.debug("获取商品详情（Feign): {}", id);
        try {
            ProductVO productVO = getProductById(id);
            return productConverter.voToDTO(productVO);
        } catch (ProductServiceException.ProductNotFoundException e) {
            log.warn("商品不存在（Feign�? {}", id);
            throw new EntityNotFoundException("商品", id);
        }
    }

    /**
     * 更新商品（Feign客户端接口）
     * 供其他服务通过Feign调用更新商品信息
     *
     * @param id         商品ID
     * @param productDTO 商品DTO
     * @return 更新后的商品DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDTO updateProductForFeign(Long id, ProductDTO productDTO) {
        log.info("更新商品（Feign): ID={}, Name={}", id, productDTO.getName());

        // 转换为RequestDTO
        ProductRequestDTO requestDTO = productConverter.dtoToRequestDTO(productDTO);
        Boolean success = updateProduct(id, requestDTO);

        if (!Boolean.TRUE.equals(success)) {
            throw new BusinessException("更新商品失败");
        }

        ProductVO productVO = getProductById(id);
        return productConverter.voToDTO(productVO);
    }

    /**
     * 获取所有商品（Feign客户端接口）
     * 供其他服务通过Feign调用获取所有商品信�?
     *
     * @return 商品DTO列表
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productListCache", key = "'all'")
    public List<ProductDTO> getAllProducts() {
        log.debug("获取所有商品（Feign)");

        List<Product> products = list();
        List<ProductVO> productVOs = productConverter.toVOList(products);
        return productConverter.voListToDTOList(productVOs);
    }

    /**
     * 根据店铺ID获取商品列表（Feign客户端接口）
     * 供其他服务通过Feign调用根据店铺ID获取商品信息
     *
     * @param shopId 店铺ID
     * @return 商品DTO列表
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productListCache",
            key = "'shop:' + #shopId")
    public List<ProductDTO> getProductsByShopId(Long shopId) {
        log.debug("根据店铺ID获取商品列表（Feign): {}", shopId);

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getShopId, shopId)
                .eq(Product::getStatus, 1) // 只获取上架的商品
                .orderByDesc(Product::getCreatedAt);

        List<Product> products = list(queryWrapper);
        List<ProductVO> productVOs = productConverter.toVOList(products);
        return productConverter.voListToDTOList(productVOs);
    }

    // ================= 私有辅助方法 =================

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<Product> buildQueryWrapper(ProductPageDTO pageDTO) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        // 商品名称模糊查询
        if (StringUtils.hasText(pageDTO.getName())) {
            queryWrapper.like(Product::getName, pageDTO.getName());
        }

        // 商品状�?
        if (pageDTO.getStatus() != null) {
            queryWrapper.eq(Product::getStatus, pageDTO.getStatus());
        }

        // 分类ID
        if (pageDTO.getCategoryId() != null) {
            queryWrapper.eq(Product::getCategoryId, pageDTO.getCategoryId());
        }

        // 分类名称
        if (StringUtils.hasText(pageDTO.getCategoryName())) {
            queryWrapper.like(Product::getCategoryName, pageDTO.getCategoryName());
        }

        // 品牌ID
        if (pageDTO.getBrandId() != null) {
            queryWrapper.eq(Product::getBrandId, pageDTO.getBrandId());
        }

        // 品牌名称
        if (StringUtils.hasText(pageDTO.getBrandName())) {
            queryWrapper.like(Product::getBrandName, pageDTO.getBrandName());
        }

        // 价格范围
        if (pageDTO.getMinPrice() != null) {
            queryWrapper.ge(Product::getPrice, pageDTO.getMinPrice());
        }
        if (pageDTO.getMaxPrice() != null) {
            queryWrapper.le(Product::getPrice, pageDTO.getMaxPrice());
        }

        // 库存范围
        if (pageDTO.getMinStock() != null) {
            queryWrapper.ge(Product::getStock, pageDTO.getMinStock());
        }
        if (pageDTO.getMaxStock() != null) {
            queryWrapper.le(Product::getStock, pageDTO.getMaxStock());
        }

        // 排序
        applySorting(queryWrapper, pageDTO);

        return queryWrapper;
    }

    /**
     * 应用排序条件
     */
    private void applySorting(LambdaQueryWrapper<Product> queryWrapper, ProductPageDTO pageDTO) {
        // 价格排序
        if ("ASC".equalsIgnoreCase(pageDTO.getPriceSort())) {
            queryWrapper.orderByAsc(Product::getPrice);
        } else if ("DESC".equalsIgnoreCase(pageDTO.getPriceSort())) {
            queryWrapper.orderByDesc(Product::getPrice);
        }

        // 库存排序
        if ("ASC".equalsIgnoreCase(pageDTO.getStockSort())) {
            queryWrapper.orderByAsc(Product::getStock);
        } else if ("DESC".equalsIgnoreCase(pageDTO.getStockSort())) {
            queryWrapper.orderByDesc(Product::getStock);
        }

        // 创建时间排序
        if ("ASC".equalsIgnoreCase(pageDTO.getCreateTimeSort())) {
            queryWrapper.orderByAsc(Product::getCreatedAt);
        } else if ("DESC".equalsIgnoreCase(pageDTO.getCreateTimeSort())) {
            queryWrapper.orderByDesc(Product::getCreatedAt);
        }

        // 更新时间排序
        if ("ASC".equalsIgnoreCase(pageDTO.getUpdateTimeSort())) {
            queryWrapper.orderByAsc(Product::getUpdatedAt);
        } else if ("DESC".equalsIgnoreCase(pageDTO.getUpdateTimeSort())) {
            queryWrapper.orderByDesc(Product::getUpdatedAt);
        }

        // 默认排序（如果没有指定任何排序）
        if (!StringUtils.hasText(pageDTO.getPriceSort()) &&
                !StringUtils.hasText(pageDTO.getStockSort()) &&
                !StringUtils.hasText(pageDTO.getCreateTimeSort()) &&
                !StringUtils.hasText(pageDTO.getUpdateTimeSort())) {
            queryWrapper.orderByDesc(Product::getCreatedAt);
        }
    }

    /**
     * 更新商品状�?
     */
    private Boolean updateProductStatus(Long id, Integer status, String operation) {
        if (id == null || id <= 0) {
            throw new BusinessException("商品ID不能为空或小于等�?");
        }

        log.info("{}商品: {}", operation, id);

        try {
            // 检查商品是否存�?
            Product product = getById(id);
            if (product == null) {
                throw new EntityNotFoundException("商品", id);
            }

            // 检查状态是否已经是目标状�?
            Integer beforeStatus = product.getStatus();
            if (product.getStatus().equals(status)) {
                log.warn("商品已经是{}状�?{}", operation, id);
                return true;
            }

            LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Product::getId, id).set(Product::getStatus, status);

            boolean updated = update(updateWrapper);
            if (!updated) {
                throw new BusinessException(operation + "商品失败");
            }

            log.info("{}商品成功: {}", operation, id);
            return true;

        } catch (EntityNotFoundException e) {
            throw e;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("{}商品时发生未预期异常，商品ID: {}", operation, id, e);
            throw new BusinessException(operation + "商品失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量更新商品状�?
     */
    private Boolean batchUpdateProductStatus(List<Long> ids, Integer status, String operation) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException("商品ID列表不能为空");
        }

        if (ids.size() > 100) {
            throw new BusinessException("批量操作数量不能超过100");
        }

        log.info("{}, 数量: {}", operation, ids.size());

        try {
            LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(Product::getId, ids).set(Product::getStatus, status);

            boolean updated = update(updateWrapper);
            if (!updated) {
                throw new BusinessException(operation + "失败");
            }

            log.info("{}成功, 数量: {}", operation, ids.size());
            return true;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("{}时发生未预期异常，商品数�? {}", operation, ids.size(), e);
            throw new BusinessException(operation + "失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取店铺名称
     */
    private String getShopName(Long shopId) {
        if (shopId == null) {
            return "未知店铺";
        }

        try {
            Shop shop = shopService.getById(shopId);
            return shop != null ? shop.getShopName() : "店铺" + shopId;
        } catch (Exception e) {
            log.warn("获取店铺名称失败，店铺ID：{}，使用默认名�?, shopId, e);
            return "店铺" + shopId;
        }
    }

    /**
     * 获取库存数量
     */
    private Integer getStockQuantity(Long productId) {
        if (productId == null) {
            return 0;
        }

        try {
            // 首先尝试从库存服务获�?
            StockVO stockVO = stockFeignClient.getStockByProductId(productId);
            if (stockVO != null && stockVO.getStockQuantity() != null) {
                return stockVO.getStockQuantity();
            }

            // 如果库存服务没有数据，从商品表获�?
            Product product = getById(productId);
            return product != null ? product.getStock() : 0;

        } catch (Exception e) {
            log.warn("获取库存数量失败，商品ID：{}，尝试从商品表获�?, productId, e);
            try {
                Product product = getById(productId);
                return product != null ? product.getStock() : 0;
            } catch (Exception ex) {
                log.error("从商品表获取库存也失败，商品ID：{}", productId, ex);
                return 0;
            }
        }
    }

    /**
     * 获取分类名称
     */
    private String getCategoryName(Long categoryId) {
        if (categoryId == null) {
            return "未分�?;
        }

        try {
            Category category = categoryService.getById(categoryId);
            return category != null ? category.getName() : "分类" + categoryId;
        } catch (Exception e) {
            log.warn("获取分类名称失败，分类ID：{}，使用默认名�?, categoryId, e);
            return "分类" + categoryId;
        }
    }

    /**
     * 获取品牌名称
     * 注意：由于当前系统没有独立的品牌服务，这里通过查询商品表中的品牌信息来获取
     * 如果将来有独立的品牌服务，可以替换为 Feign 调用
     */
    private String getBrandName(Long brandId) {
        if (brandId == null) {
            return "未知品牌";
        }

        try {
            // 查询该品牌ID下的任意一个商品，获取品牌名称
            LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Product::getBrandId, brandId)
                    .isNotNull(Product::getBrandName)
                    .ne(Product::getBrandName, "")
                    .last("LIMIT 1");

            Product product = getOne(queryWrapper);
            if (product != null && StringUtils.hasText(product.getBrandName())) {
                return product.getBrandName();
            }

            // 如果没有找到品牌名称，返回默认�?
            return "品牌" + brandId;

        } catch (Exception e) {
            log.warn("获取品牌名称失败，品牌ID：{}，使用默认名�?, brandId, e);
            return "品牌" + brandId;
        }
    }

}





