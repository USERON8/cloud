package com.cloud.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.product.ProductDTO;
import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.result.PageResult;
import com.cloud.product.exception.ProductServiceException;
import com.cloud.product.module.dto.ProductPageDTO;
import com.cloud.product.module.entity.Product;

import java.util.List;









public interface ProductService extends IService<Product> {

    

    






    Long createProduct(ProductRequestDTO requestDTO) throws ProductServiceException;

    








    Boolean updateProduct(Long id, ProductRequestDTO requestDTO) throws ProductServiceException.ProductNotFoundException, ProductServiceException;

    







    Boolean deleteProduct(Long id) throws ProductServiceException.ProductNotFoundException, ProductServiceException;

    






    Boolean batchDeleteProducts(List<Long> ids) throws ProductServiceException;

    

    






    ProductVO getProductById(Long id) throws ProductServiceException.ProductNotFoundException;

    





    List<ProductVO> getProductsByIds(List<Long> ids);

    





    PageResult<ProductVO> getProductsPage(ProductPageDTO pageDTO);

    







    List<ProductVO> getProductsByCategoryId(Long categoryId, Integer status) throws ProductServiceException.CategoryNotFoundException;

    






    List<ProductVO> getProductsByBrandId(Long brandId, Integer status);

    






    List<ProductVO> searchProductsByName(String name, Integer status);

    

    








    Boolean enableProduct(Long id) throws ProductServiceException.ProductNotFoundException, ProductServiceException.ProductStatusException, ProductServiceException;

    








    Boolean disableProduct(Long id) throws ProductServiceException.ProductNotFoundException, ProductServiceException.ProductStatusException, ProductServiceException;

    








    Boolean batchEnableProducts(List<Long> ids) throws ProductServiceException.ProductNotFoundException, ProductServiceException.ProductStatusException, ProductServiceException;

    








    Boolean batchDisableProducts(List<Long> ids) throws ProductServiceException.ProductNotFoundException, ProductServiceException.ProductStatusException, ProductServiceException;

    

    






    Boolean updateStock(Long id, Integer stock);

    








    Boolean increaseStock(Long id, Integer amount) throws ProductServiceException.ProductNotFoundException, ProductServiceException;

    








    Boolean decreaseStock(Long id, Integer amount) throws ProductServiceException.ProductNotFoundException, ProductServiceException;

    








    Boolean checkStock(Long id, Integer amount) throws ProductServiceException.ProductNotFoundException, ProductServiceException;

    

    




    Long getTotalProductCount();

    




    Long getEnabledProductCount();

    




    Long getDisabledProductCount();

    





    Long getProductCountByCategoryId(Long categoryId);

    





    Long getProductCountByBrandId(Long brandId);

    

    




    void evictProductCache(Long id);

    


    void evictAllProductCache();

    




    void warmupProductCache(List<Long> ids);

    

    





    ProductDTO createProductForFeign(ProductDTO productDTO);

    





    ProductDTO getProductByIdForFeign(Long id);

    






    ProductDTO updateProductForFeign(Long id, ProductDTO productDTO);

    




    List<ProductDTO> getAllProducts();

    





    List<ProductDTO> getProductsByShopId(Long shopId);
}
