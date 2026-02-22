package com.cloud.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;








@Data
@Schema(description = "鍟嗗搧鎼滅储璇锋眰鍙傛暟")
public class ProductSearchRequest {

    


    @Schema(description = "鎼滅储鍏抽敭瀛?, example = "鏅鸿兘鎵嬫満")
    private String keyword;

    


    @Schema(description = "搴楅摵ID", example = "1")
    private Long shopId;

    


    @Schema(description = "搴楅摵鍚嶇О", example = "鍗庝负瀹樻柟鏃楄埌搴?)
    private String shopName;

    


    @Schema(description = "鍒嗙被ID", example = "3")
    private Long categoryId;

    


    @Schema(description = "鍒嗙被鍚嶇О", example = "鎵嬫満")
    private String categoryName;

    


    @Schema(description = "鍝佺墝ID", example = "1")
    private Long brandId;

    


    @Schema(description = "鍝佺墝鍚嶇О", example = "鍗庝负")
    private String brandName;

    


    @Schema(description = "鏈€浣庝环鏍?, example = "1000.00")
    private BigDecimal minPrice;

    


    @Schema(description = "鏈€楂樹环鏍?, example = "5000.00")
    private BigDecimal maxPrice;

    


    @Schema(description = "鍟嗗搧鐘舵€侊細0-涓嬫灦锛?-涓婃灦", example = "1")
    private Integer status;

    


    @Schema(description = "搴撳瓨鐘舵€侊細0-鏃犲簱瀛橈紝1-鏈夊簱瀛?, example = "1")
    private Integer stockStatus;

    


    @Schema(description = "鏄惁鎺ㄨ崘", example = "true")
    private Boolean recommended;

    


    @Schema(description = "鏄惁鏂板搧", example = "true")
    private Boolean isNew;

    


    @Schema(description = "鏄惁鐑攢", example = "true")
    private Boolean isHot;

    


    @Schema(description = "鍟嗗搧鏍囩", example = "5G,鍙屽崱")
    private List<String> tags;

    


    @Schema(description = "鏈€浣庨攢閲?, example = "100")
    private Integer minSalesCount;

    


    @Schema(description = "鏈€浣庤瘎鍒?, example = "4.0")
    private BigDecimal minRating;

    


    @Schema(description = "椤电爜", example = "0")
    private Integer page = 0;

    


    @Schema(description = "姣忛〉澶у皬", example = "20")
    private Integer size = 20;

    


    @Schema(description = "鎺掑簭瀛楁", example = "price", allowableValues = {"price", "salesCount", "rating", "createdAt", "hotScore"})
    private String sortBy;

    


    @Schema(description = "鎺掑簭鏂瑰紡", example = "asc", allowableValues = {"asc", "desc"})
    private String sortOrder = "desc";

    


    @Schema(description = "鏄惁鍚敤楂樹寒", example = "true")
    private Boolean highlight = false;

    


    @Schema(description = "鏄惁杩斿洖鑱氬悎淇℃伅", example = "true")
    private Boolean includeAggregations = false;
}
