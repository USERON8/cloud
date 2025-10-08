package com.cloud.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评分等级枚举
 *
 * @author Cloud
 * @date 2025-01-07
 */
@Getter
@AllArgsConstructor
public enum RatingLevel {

    /**
     * 差评 (1-2星)
     */
    BAD(1, "差评", 1, 2),

    /**
     * 中评 (3星)
     */
    MEDIUM(2, "中评", 3, 3),

    /**
     * 好评 (4-5星)
     */
    GOOD(3, "好评", 4, 5);

    /**
     * 等级代码
     */
    private final Integer code;

    /**
     * 等级描述
     */
    private final String description;

    /**
     * 最小评分
     */
    private final Integer minRating;

    /**
     * 最大评分
     */
    private final Integer maxRating;

    /**
     * 根据评分获取等级
     *
     * @param rating 评分
     * @return 评分等级
     */
    public static RatingLevel fromRating(Integer rating) {
        if (rating == null) {
            return null;
        }
        for (RatingLevel level : RatingLevel.values()) {
            if (rating >= level.getMinRating() && rating <= level.getMaxRating()) {
                return level;
            }
        }
        throw new IllegalArgumentException("无效的评分: " + rating);
    }

    /**
     * 判断是否为好评
     *
     * @param rating 评分
     * @return 是否为好评
     */
    public static boolean isGoodReview(Integer rating) {
        return rating != null && rating >= 4;
    }

    /**
     * 判断是否为中评
     *
     * @param rating 评分
     * @return 是否为中评
     */
    public static boolean isMediumReview(Integer rating) {
        return rating != null && rating == 3;
    }

    /**
     * 判断是否为差评
     *
     * @param rating 评分
     * @return 是否为差评
     */
    public static boolean isBadReview(Integer rating) {
        return rating != null && rating <= 2;
    }
}

