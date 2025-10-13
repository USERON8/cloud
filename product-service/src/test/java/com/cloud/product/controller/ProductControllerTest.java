package com.cloud.product.controller;

import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.product.controller.product.ProductController;
import com.cloud.product.module.dto.ProductPageDTO;
import com.cloud.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * ProductController 单元测试
 *
 * @author Claude Code
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("商品控制器单元测试")
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProductController productController;

    private ProductVO testProductVO;
    private ProductRequestDTO testRequestDTO;
    private PageResult<ProductVO> testPageResult;

    @BeforeEach
    void setUp() {
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

        testPageResult = PageResult.of(1L, 20L, 1L, Arrays.asList(testProductVO));

        when(authentication.getName()).thenReturn("testuser");
    }

    @Test
    @DisplayName("获取商品列表-成功")
    void testGetProducts_Success() {
        // Given
        when(productService.getProductsPage(any(ProductPageDTO.class))).thenReturn(testPageResult);

        // When
        Result<PageResult<ProductVO>> result = productController.getProducts(
                1, 20, null, null, null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getTotal());
        verify(productService).getProductsPage(any(ProductPageDTO.class));
    }

    @Test
    @DisplayName("根据ID获取商品详情-成功")
    void testGetProductById_Success() {
        // Given
        Long productId = 1L;
        when(productService.getProductById(productId)).thenReturn(testProductVO);

        // When
        Result<ProductVO> result = productController.getProductById(productId);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(testProductVO, result.getData());
        verify(productService).getProductById(productId);
    }

    @Test
    @DisplayName("根据商品名称查询商品-成功")
    void testFindByName_Success() {
        // Given
        String productName = "测试商品";
        List<ProductVO> products = Arrays.asList(testProductVO);
        when(productService.searchProductsByName(productName, null)).thenReturn(products);

        // When
        Result<List<ProductVO>> result = productController.findByName(productName);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(1, result.getData().size());
        verify(productService).searchProductsByName(productName, null);
    }

    @Test
    @DisplayName("创建商品-成功")
    void testCreateProduct_Success() {
        // Given
        Long productId = 1L;
        when(productService.createProduct(testRequestDTO)).thenReturn(productId);

        // When
        Result<Long> result = productController.createProduct(testRequestDTO);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(productId, result.getData());
        verify(productService).createProduct(testRequestDTO);
    }

    @Test
    @DisplayName("更新商品信息-成功")
    void testUpdateProduct_Success() {
        // Given
        Long productId = 1L;
        when(productService.updateProduct(productId, testRequestDTO)).thenReturn(true);

        // When
        Result<Boolean> result = productController.updateProduct(productId, testRequestDTO, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(productService).updateProduct(productId, testRequestDTO);
    }

    @Test
    @DisplayName("部分更新商品信息-成功")
    void testPatchProduct_Success() {
        // Given
        Long productId = 1L;
        when(productService.updateProduct(productId, testRequestDTO)).thenReturn(true);

        // When
        Result<Boolean> result = productController.patchProduct(productId, testRequestDTO, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(productService).updateProduct(productId, testRequestDTO);
    }

    @Test
    @DisplayName("删除商品-成功")
    void testDeleteProduct_Success() {
        // Given
        Long productId = 1L;
        when(productService.deleteProduct(productId)).thenReturn(true);

        // When
        Result<Boolean> result = productController.deleteProduct(productId);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(productService).deleteProduct(productId);
    }

    @Test
    @DisplayName("批量获取商品-成功")
    void testGetProductsByIds_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        List<ProductVO> products = Arrays.asList(testProductVO, testProductVO, testProductVO);
        when(productService.getProductsByIds(ids)).thenReturn(products);

        // When
        Result<List<ProductVO>> result = productController.getProductsByIds(ids);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(3, result.getData().size());
        verify(productService).getProductsByIds(ids);
    }

    @Test
    @DisplayName("获取商品档案-成功")
    void testGetProductProfile_Success() {
        // Given
        Long productId = 1L;
        when(productService.getProductById(productId)).thenReturn(testProductVO);

        // When
        Result<ProductVO> result = productController.getProductProfile(productId, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(testProductVO, result.getData());
        verify(productService).getProductById(productId);
    }

    @Test
    @DisplayName("更新商品档案-成功")
    void testUpdateProductProfile_Success() {
        // Given
        Long productId = 1L;
        when(productService.updateProduct(productId, testRequestDTO)).thenReturn(true);

        // When
        Result<Boolean> result = productController.updateProductProfile(
                productId, testRequestDTO, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(productService).updateProduct(productId, testRequestDTO);
    }

    @Test
    @DisplayName("更新商品状态-上架成功")
    void testUpdateProductStatus_EnableSuccess() {
        // Given
        Long productId = 1L;
        Integer status = 1; // 上架
        when(productService.enableProduct(productId)).thenReturn(true);

        // When
        Result<Boolean> result = productController.updateProductStatus(productId, status);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(productService).enableProduct(productId);
        verify(productService, never()).disableProduct(anyLong());
    }

    @Test
    @DisplayName("更新商品状态-下架成功")
    void testUpdateProductStatus_DisableSuccess() {
        // Given
        Long productId = 1L;
        Integer status = 0; // 下架
        when(productService.disableProduct(productId)).thenReturn(true);

        // When
        Result<Boolean> result = productController.updateProductStatus(productId, status);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(productService).disableProduct(productId);
        verify(productService, never()).enableProduct(anyLong());
    }

    @Test
    @DisplayName("根据分类查询商品-成功")
    void testGetProductsByCategoryId_Success() {
        // Given
        Long categoryId = 1L;
        Integer status = 1;
        List<ProductVO> products = Arrays.asList(testProductVO);
        when(productService.getProductsByCategoryId(categoryId, status)).thenReturn(products);

        // When
        Result<List<ProductVO>> result = productController.getProductsByCategoryId(categoryId, status);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(1, result.getData().size());
        verify(productService).getProductsByCategoryId(categoryId, status);
    }

    @Test
    @DisplayName("根据品牌查询商品-成功")
    void testGetProductsByBrandId_Success() {
        // Given
        Long brandId = 1L;
        Integer status = 1;
        List<ProductVO> products = Arrays.asList(testProductVO);
        when(productService.getProductsByBrandId(brandId, status)).thenReturn(products);

        // When
        Result<List<ProductVO>> result = productController.getProductsByBrandId(brandId, status);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(1, result.getData().size());
        verify(productService).getProductsByBrandId(brandId, status);
    }

    @Test
    @DisplayName("批量删除商品-成功")
    void testBatchDeleteProducts_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(productService.batchDeleteProducts(ids)).thenReturn(true);

        // When
        Result<Boolean> result = productController.batchDeleteProducts(ids);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(productService).batchDeleteProducts(ids);
    }

    @Test
    @DisplayName("批量上架商品-成功")
    void testBatchEnableProducts_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(productService.batchEnableProducts(ids)).thenReturn(true);

        // When
        Result<Boolean> result = productController.batchEnableProducts(ids);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(productService).batchEnableProducts(ids);
    }

    @Test
    @DisplayName("批量上架商品-超过最大数量限制")
    void testBatchEnableProducts_ExceedMaxLimit() {
        // Given
        List<Long> tooManyIds = Arrays.asList(new Long[101]); // 超过100个

        // When
        Result<Boolean> result = productController.batchEnableProducts(tooManyIds);

        // Then
        assertNotNull(result);
        assertFalse(result.getSuccess());
        verify(productService, never()).batchEnableProducts(any());
    }

    @Test
    @DisplayName("批量下架商品-成功")
    void testBatchDisableProducts_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(productService.batchDisableProducts(ids)).thenReturn(true);

        // When
        Result<Boolean> result = productController.batchDisableProducts(ids);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertTrue(result.getData());
        verify(productService).batchDisableProducts(ids);
    }

    @Test
    @DisplayName("批量下架商品-超过最大数量限制")
    void testBatchDisableProducts_ExceedMaxLimit() {
        // Given
        List<Long> tooManyIds = Arrays.asList(new Long[101]); // 超过100个

        // When
        Result<Boolean> result = productController.batchDisableProducts(tooManyIds);

        // Then
        assertNotNull(result);
        assertFalse(result.getSuccess());
        verify(productService, never()).batchDisableProducts(any());
    }

    @Test
    @DisplayName("批量创建商品-成功")
    void testBatchCreateProducts_Success() {
        // Given
        List<ProductRequestDTO> productList = Arrays.asList(testRequestDTO, testRequestDTO);
        when(productService.createProduct(any(ProductRequestDTO.class))).thenReturn(1L);

        // When
        Result<Integer> result = productController.batchCreateProducts(productList);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(2, result.getData());
        verify(productService, times(2)).createProduct(any(ProductRequestDTO.class));
    }

    @Test
    @DisplayName("批量创建商品-部分失败")
    void testBatchCreateProducts_PartialFailure() {
        // Given
        List<ProductRequestDTO> productList = Arrays.asList(testRequestDTO, testRequestDTO, testRequestDTO);
        when(productService.createProduct(any(ProductRequestDTO.class)))
                .thenReturn(1L)
                .thenThrow(new RuntimeException("创建失败"))
                .thenReturn(3L);

        // When
        Result<Integer> result = productController.batchCreateProducts(productList);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(2, result.getData()); // 3个中有2个成功
        verify(productService, times(3)).createProduct(any(ProductRequestDTO.class));
    }

    @Test
    @DisplayName("批量创建商品-超过最大数量限制")
    void testBatchCreateProducts_ExceedMaxLimit() {
        // Given
        List<ProductRequestDTO> tooManyProducts = Arrays.asList(new ProductRequestDTO[101]);

        // When
        Result<Integer> result = productController.batchCreateProducts(tooManyProducts);

        // Then
        assertNotNull(result);
        assertFalse(result.getSuccess());
        verify(productService, never()).createProduct(any(ProductRequestDTO.class));
    }

    @Test
    @DisplayName("批量更新商品-成功")
    void testBatchUpdateProducts_Success() {
        // Given
        ProductController.ProductUpdateRequest request1 = new ProductController.ProductUpdateRequest();
        request1.setId(1L);
        request1.setRequestDTO(testRequestDTO);

        ProductController.ProductUpdateRequest request2 = new ProductController.ProductUpdateRequest();
        request2.setId(2L);
        request2.setRequestDTO(testRequestDTO);

        List<ProductController.ProductUpdateRequest> productList = Arrays.asList(request1, request2);
        when(productService.updateProduct(anyLong(), any(ProductRequestDTO.class))).thenReturn(true);

        // When
        Result<Integer> result = productController.batchUpdateProducts(productList);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(2, result.getData());
        verify(productService, times(2)).updateProduct(anyLong(), any(ProductRequestDTO.class));
    }

    @Test
    @DisplayName("批量更新商品-部分失败")
    void testBatchUpdateProducts_PartialFailure() {
        // Given
        ProductController.ProductUpdateRequest request1 = new ProductController.ProductUpdateRequest();
        request1.setId(1L);
        request1.setRequestDTO(testRequestDTO);

        ProductController.ProductUpdateRequest request2 = new ProductController.ProductUpdateRequest();
        request2.setId(2L);
        request2.setRequestDTO(testRequestDTO);

        List<ProductController.ProductUpdateRequest> productList = Arrays.asList(request1, request2);
        when(productService.updateProduct(anyLong(), any(ProductRequestDTO.class)))
                .thenReturn(true)
                .thenThrow(new RuntimeException("更新失败"));

        // When
        Result<Integer> result = productController.batchUpdateProducts(productList);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(1, result.getData()); // 2个中有1个成功
        verify(productService, times(2)).updateProduct(anyLong(), any(ProductRequestDTO.class));
    }

    @Test
    @DisplayName("批量更新商品-超过最大数量限制")
    void testBatchUpdateProducts_ExceedMaxLimit() {
        // Given
        List<ProductController.ProductUpdateRequest> tooManyProducts =
                Arrays.asList(new ProductController.ProductUpdateRequest[101]);

        // When
        Result<Integer> result = productController.batchUpdateProducts(tooManyProducts);

        // Then
        assertNotNull(result);
        assertFalse(result.getSuccess());
        verify(productService, never()).updateProduct(anyLong(), any(ProductRequestDTO.class));
    }
}
