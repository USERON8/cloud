package com.cloud.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;








@Data
@Schema(description = "搴楅摵鎼滅储璇锋眰鍙傛暟")
public class ShopSearchRequest {

    


    @Schema(description = "鎼滅储鍏抽敭瀛?, example = "鍗庝负鏃楄埌搴?)
    private String keyword;

    


    @Schema(description = "鍟嗗ID", example = "1")
    private Long merchantId;

    


    @Schema(description = "搴楅摵鐘舵€侊細0-鍏抽棴锛?-钀ヤ笟", example = "1")
    private Integer status;

    


    @Schema(description = "鏈€浣庤瘎鍒?, example = "4.0")
    private BigDecimal minRating;

    


    @Schema(description = "鏈€浣庡晢鍝佹暟閲?, example = "10")
    private Integer minProductCount;

    


    @Schema(description = "鏈€浣庡叧娉ㄦ暟閲?, example = "100")
    private Integer minFollowCount;

    


    @Schema(description = "鏄惁鎺ㄨ崘搴楅摵", example = "true")
    private Boolean recommended;

    


    @Schema(description = "鍦板潃鍏抽敭瀛?, example = "鍖椾含")
    private String addressKeyword;

    


    @Schema(description = "椤电爜", example = "0")
    private Integer page = 0;

    


    @Schema(description = "姣忛〉澶у皬", example = "20")
    private Integer size = 20;

    


    @Schema(description = "鎺掑簭瀛楁", example = "rating", allowableValues = {"rating", "productCount", "followCount", "createdAt", "hotScore"})
    private String sortBy;

    


    @Schema(description = "鎺掑簭鏂瑰紡", example = "desc", allowableValues = {"asc", "desc"})
    private String sortOrder = "desc";

    


    @Schema(description = "鏄惁鍚敤楂樹寒", example = "true")
    private Boolean highlight = false;

    


    @Schema(description = "鏄惁杩斿洖鑱氬悎淇℃伅", example = "true")
    private Boolean includeAggregations = false;
}
