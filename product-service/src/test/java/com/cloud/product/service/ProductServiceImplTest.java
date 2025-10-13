package com.cloud.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.api.stock.StockFeignClient;
import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.product.converter.ProductConverter;
import com.cloud.product.exception.ProductServiceException;
import com.cloud.product.mapper.ProductMapper;
import com.cloud.product.module.dto.ProductPageDTO;
import com.cloud.product.module.entity.Product;
import com.cloud.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * ProductService 单元测试
 *
 * @author Claude Code
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("商品服务单元测试")
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductConverter productConverter;

    @Mock
    private StockFeignClient stockFeignClient;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductVO testProductVO;
    private ProductRequestDTO testRequestDTO;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("测试商品");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(100);
        testProduct.setStatus(1); // 上架状态

        testProductVO = new ProductVO();
        testProductVO.setId(1L);
        testProductVO.setName("测试商品");
        testProductVO.setPrice(new BigDecimal("99.99"));
        testProductVO.setStock(100);
        testProductVO.setStatus(1);

        testRequestDTO = new ProductRequestDTO();
        testRequestDTO.setName("测试商品");
        testRequestDTO.setPrice(new BigDecimal("99.99"));
        testRequestDTO.setStock(100);
    }

    @Test
    @DisplayName("根据ID获取商品-成功")
    void testGetProductById_Success() {
        // Given
        Long productId = 1L;
        when(productMapper.selectById(productId)).thenReturn(testProduct);
        when(productConverter.toVO(testProduct)).thenReturn(testProductVO);

        // When
        ProductVO result = productService.getProductById(productId);

        // Then
        assertNotNull(result);
        assertEquals(productId, result.getId());
        assertEquals("测试商品", result.getName());
        verify(productMapper).selectById(productId);
        verify(productConverter).toVO(testProduct);
    }

    @Test
    @DisplayName("根据ID获取商品-商品不存在")
    void testGetProductById_NotFound() {
        // Given
        Long productId = 999L;
        when(productMapper.selectById(productId)).thenReturn(null);

        // When & Then
        assertThrows(ProductServiceException.ProductNotFoundException.class,
                () -> productService.getProductById(productId));
        verify(productMapper).selectById(productId);
    }

    @Test
    @DisplayName("创建商品-成功")
    void testCreateProduct_Success() {
        // Given
        when(productConverter.requestDTOToEntity(testRequestDTO)).thenReturn(testProduct);
        when(productMapper.insert(any(Product.class))).thenReturn(1);

        // When
        Long result = productService.createProduct(testRequestDTO);

        // Then
        assertNotNull(result);
        verify(productMapper).insert(any(Product.class));
    }

    @Test
    @DisplayName("创建商品-商品信息为空")
    void testCreateProduct_NullRequest() {
        // Given
        ProductRequestDTO nullDTO = null;

        // When & Then
        assertThrows(BusinessException.class,
                () -> productService.createProduct(nullDTO));
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    @DisplayName("创建商品-商品名称为空")
    void testCreateProduct_EmptyName() {
        // Given
        testRequestDTO.setName("");

        // When & Then
        assertThrows(BusinessException.class,
                () -> productService.createProduct(testRequestDTO));
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    @DisplayName("更新商品-成功")
    void testUpdateProduct_Success() {
        // Given
        Long productId = 1L;
        when(productMapper.selectById(productId)).thenReturn(testProduct);
        when(productConverter.requestDTOToEntity(testRequestDTO)).thenReturn(testProduct);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);

        // When
        Boolean result = productService.updateProduct(productId, testRequestDTO);

        // Then
        assertTrue(result);
        verify(productMapper).selectById(productId);
        verify(productMapper).updateById(any(Product.class));
    }

    @Test
    @DisplayName("更新商品-商品不存在")
    void testUpdateProduct_NotFound() {
        // Given
        Long productId = 999L;
        when(productMapper.selectById(productId)).thenReturn(null);

        // When & Then
        assertThrows(EntityNotFoundException.class,
                () -> productService.updateProduct(productId, testRequestDTO));
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    @DisplayName("删除商品-成功")
    void testDeleteProduct_Success() {
        // Given
        Long productId = 1L;
        when(productMapper.selectById(productId)).thenReturn(testProduct);
        when(productMapper.deleteById(productId)).thenReturn(1);

        // When
        Boolean result = productService.deleteProduct(productId);

        // Then
        assertTrue(result);
        verify(productMapper).selectById(productId);
        verify(productMapper).deleteById(productId);
    }

    @Test
    @DisplayName("删除商品-商品不存在")
    void testDeleteProduct_NotFound() {
        // Given
        Long productId = 999L;
        when(productMapper.selectById(productId)).thenReturn(null);

        // When & Then
        assertThrows(EntityNotFoundException.class,
                () -> productService.deleteProduct(productId));
        verify(productMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("批量删除商品-成功")
    void testBatchDeleteProducts_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        List<Product> products = Arrays.asList(testProduct, testProduct, testProduct);
        when(productMapper.selectBatchIds(ids)).thenReturn(products);
        when(productMapper.deleteBatchIds(ids)).thenReturn(3);

        // When
        Boolean result = productService.batchDeleteProducts(ids);

        // Then
        assertTrue(result);
        verify(productMapper).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("批量删除商品-ID列表为空")
    void testBatchDeleteProducts_EmptyIds() {
        // Given
        List<Long> emptyIds = Arrays.asList();

        // When & Then
        assertThrows(BusinessException.class,
                () -> productService.batchDeleteProducts(emptyIds));
    }

    @Test
    @DisplayName("批量删除商品-超过最大数量")
    void testBatchDeleteProducts_ExceedMaxLimit() {
        // Given
        List<Long> tooManyIds = Arrays.asList(new Long[101]);

        // When & Then
        assertThrows(BusinessException.class,
                () -> productService.batchDeleteProducts(tooManyIds));
    }

    @Test
    @DisplayName("上架商品-成功")
    void testEnableProduct_Success() {
        // Given
        Long productId = 1L;
        testProduct.setStatus(0); // 下架状态
        when(productMapper.selectById(productId)).thenReturn(testProduct);
        when(productMapper.update(any(Product.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // When
        Boolean result = productService.enableProduct(productId);

        // Then
        assertTrue(result);
        verify(productMapper).selectById(productId);
    }

    @Test
    @DisplayName("下架商品-成功")
    void testDisableProduct_Success() {
        // Given
        Long productId = 1L;
        testProduct.setStatus(1); // 上架状态
        when(productMapper.selectById(productId)).thenReturn(testProduct);
        when(productMapper.update(any(Product.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // When
        Boolean result = productService.disableProduct(productId);

        // Then
        assertTrue(result);
        verify(productMapper).selectById(productId);
    }

    @Test
    @DisplayName("更新库存-成功")
    void testUpdateStock_Success() {
        // Given
        Long productId = 1L;
        Integer newStock = 200;
        when(productMapper.update(any(Product.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // When
        Boolean result = productService.updateStock(productId, newStock);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("更新库存-库存为负数")
    void testUpdateStock_NegativeStock() {
        // Given
        Long productId = 1L;
        Integer negativeStock = -10;

        // When & Then
        assertThrows(BusinessException.class,
                () -> productService.updateStock(productId, negativeStock));
    }

    @Test
    @DisplayName("增加库存-成功")
    void testIncreaseStock_Success() {
        // Given
        Long productId = 1L;
        Integer quantity = 50;
        when(productMapper.update(any(Product.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // When
        Boolean result = productService.increaseStock(productId, quantity);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("增加库存-数量小于等于0")
    void testIncreaseStock_InvalidQuantity() {
        // Given
        Long productId = 1L;
        Integer invalidQuantity = 0;

        // When & Then
        assertThrows(BusinessException.class,
                () -> productService.increaseStock(productId, invalidQuantity));
    }

    @Test
    @DisplayName("减少库存-成功")
    void testDecreaseStock_Success() {
        // Given
        Long productId = 1L;
        Integer quantity = 10;
        when(productMapper.update(any(Product.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // When
        Boolean result = productService.decreaseStock(productId, quantity);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("减少库存-数量小于等于0")
    void testDecreaseStock_InvalidQuantity() {
        // Given
        Long productId = 1L;
        Integer invalidQuantity = -5;

        // When & Then
        assertThrows(BusinessException.class,
                () -> productService.decreaseStock(productId, invalidQuantity));
    }

    @Test
    @DisplayName("减少库存-库存不足")
    void testDecreaseStock_InsufficientStock() {
        // Given
        Long productId = 1L;
        Integer quantity = 200; // 超过当前库存
        when(productMapper.update(any(Product.class), any(LambdaUpdateWrapper.class))).thenReturn(0);

        // When & Then
        assertThrows(BusinessException.class,
                () -> productService.decreaseStock(productId, quantity));
    }

    @Test
    @DisplayName("检查库存-库存充足")
    void testCheckStock_Sufficient() {
        // Given
        Long productId = 1L;
        Integer quantity = 50;
        when(productMapper.selectById(productId)).thenReturn(testProduct);

        // When
        Boolean result = productService.checkStock(productId, quantity);

        // Then
        assertTrue(result);
        verify(productMapper).selectById(productId);
    }

    @Test
    @DisplayName("检查库存-库存不足")
    void testCheckStock_Insufficient() {
        // Given
        Long productId = 1L;
        Integer quantity = 150; // 超过当前库存100
        when(productMapper.selectById(productId)).thenReturn(testProduct);

        // When
        Boolean result = productService.checkStock(productId, quantity);

        // Then
        assertFalse(result);
        verify(productMapper).selectById(productId);
    }

    @Test
    @DisplayName("检查库存-商品不存在")
    void testCheckStock_ProductNotFound() {
        // Given
        Long productId = 999L;
        Integer quantity = 50;
        when(productMapper.selectById(productId)).thenReturn(null);

        // When
        Boolean result = productService.checkStock(productId, quantity);

        // Then
        assertFalse(result);
        verify(productMapper).selectById(productId);
    }

    @Test
    @DisplayName("批量获取商品-成功")
    void testGetProductsByIds_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        List<Product> products = Arrays.asList(testProduct, testProduct, testProduct);
        when(productMapper.selectBatchIds(ids)).thenReturn(products);
        when(productConverter.toVOList(products)).thenReturn(Arrays.asList(testProductVO, testProductVO, testProductVO));

        // When
        List<ProductVO> result = productService.getProductsByIds(ids);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(productMapper).selectBatchIds(ids);
    }

    @Test
    @DisplayName("批量获取商品-ID列表为空")
    void testGetProductsByIds_EmptyIds() {
        // Given
        List<Long> emptyIds = Arrays.asList();

        // When
        List<ProductVO> result = productService.getProductsByIds(emptyIds);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("根据名称搜索商品-成功")
    void testSearchProductsByName_Success() {
        // Given
        String name = "测试";
        List<Product> products = Arrays.asList(testProduct);
        when(productMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(products);
        when(productConverter.toVOList(products)).thenReturn(Arrays.asList(testProductVO));

        // When
        List<ProductVO> result = productService.searchProductsByName(name, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("根据名称搜索商品-名称为空")
    void testSearchProductsByName_EmptyName() {
        // Given
        String emptyName = "";

        // When
        List<ProductVO> result = productService.searchProductsByName(emptyName, null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取商品总数-成功")
    void testGetTotalProductCount_Success() {
        // Given
        when(productMapper.selectCount(any())).thenReturn(100L);

        // When
        Long result = productService.getTotalProductCount();

        // Then
        assertEquals(100L, result);
        verify(productMapper).selectCount(any());
    }

    @Test
    @DisplayName("获取上架商品数量-成功")
    void testGetEnabledProductCount_Success() {
        // Given
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(80L);

        // When
        Long result = productService.getEnabledProductCount();

        // Then
        assertEquals(80L, result);
    }

    @Test
    @DisplayName("获取下架商品数量-成功")
    void testGetDisabledProductCount_Success() {
        // Given
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(20L);

        // When
        Long result = productService.getDisabledProductCount();

        // Then
        assertEquals(20L, result);
    }

    @Test
    @DisplayName("批量上架商品-成功")
    void testBatchEnableProducts_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(productMapper.update(any(Product.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // When
        Boolean result = productService.batchEnableProducts(ids);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("批量下架商品-成功")
    void testBatchDisableProducts_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(productMapper.update(any(Product.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // When
        Boolean result = productService.batchDisableProducts(ids);

        // Then
        assertTrue(result);
    }
}
