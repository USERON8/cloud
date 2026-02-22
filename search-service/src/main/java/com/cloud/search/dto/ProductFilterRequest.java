package com.cloud.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;








@Data
@Schema(description = "鍟嗗搧绛涢€夎姹傚弬鏁?)
public class ProductFilterRequest {

    


    @Schema(description = "鎼滅储鍏抽敭瀛?, example = "鎵嬫満")
    private String keyword;

    


    @Schema(description = "鍒嗙被ID", example = "1")
    private Long categoryId;

    


    @Schema(description = "鍝佺墝ID", example = "1")
    private Long brandId;

    


    @Schema(description = "搴楅摵ID", example = "1")
    private Long shopId;

    


    @Schema(description = "鏈€浣庝环鏍?, example = "1000.00")
    @Min(value = 0, message = "浠锋牸涓嶈兘涓鸿礋鏁?)
    private BigDecimal minPrice;

    


    @Schema(description = "鏈€楂樹环鏍?, example = "5000.00")
    @Min(value = 0, message = "浠锋牸涓嶈兘涓鸿礋鏁?)
    private BigDecimal maxPrice;

    


    @Schema(description = "鏈€浣庨攢閲?, example = "100")
    @Min(value = 0, message = "閿€閲忎笉鑳戒负璐熸暟")
    private Integer minSalesCount;

    


    @Schema(description = "鏄惁鎺ㄨ崘", example = "true")
    private Boolean recommended;

    


    @Schema(description = "鏄惁鏂板搧", example = "true")
    private Boolean isNew;

    


    @Schema(description = "鏄惁鐑攢", example = "true")
    private Boolean isHot;

    


    @Schema(description = "鎺掑簭瀛楁", example = "price",
            allowableValues = {"price", "salesCount", "rating", "createdAt", "hotScore"})
    private String sortBy = "hotScore";

    


    @Schema(description = "鎺掑簭鏂瑰紡", example = "desc", allowableValues = {"asc", "desc"})
    private String sortOrder = "desc";

    


    @Schema(description = "椤电爜", example = "0")
    @Min(value = 0, message = "椤电爜涓嶈兘涓鸿礋鏁?)
    private Integer page = 0;

    


    @Schema(description = "姣忛〉澶у皬", example = "20")
    @Min(value = 1, message = "姣忛〉澶у皬鑷冲皯涓?")
    @Max(value = 100, message = "姣忛〉澶у皬涓嶈兘瓒呰繃100")
    private Integer size = 20;
}
