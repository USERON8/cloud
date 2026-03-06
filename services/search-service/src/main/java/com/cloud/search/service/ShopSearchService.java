package com.cloud.search.service;


import com.cloud.search.document.ShopDocument;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.dto.ShopSearchRequest;

import java.util.List;








public interface ShopSearchService {

    




    void deleteShop(Long shopId);

    





    void updateShopStatus(Long shopId, Integer status);

    





    ShopDocument findByShopId(Long shopId);

    




    void batchDeleteShops(List<Long> shopIds);

    





    boolean isEventProcessed(String traceId);

    




    void markEventProcessed(String traceId);

    


    void rebuildShopIndex();

    




    boolean indexExists();

    


    void createShopIndex();

    


    void deleteShopIndex();

    





    SearchResult<ShopDocument> searchShops(ShopSearchRequest request);

    






    List<String> getSearchSuggestions(String keyword, Integer size);

    





    List<ShopDocument> getHotShops(Integer size);

    





    SearchResult<ShopDocument> getShopFilters(ShopSearchRequest request);
}
