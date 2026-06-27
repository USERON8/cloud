package com.cloud.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RatingLevel {
  BAD(1, "差评", 1, 2),

  MEDIUM(2, "中评", 3, 3),

  GOOD(3, "好评", 4, 5);

  private final Integer code;

  private final String description;

  private final Integer minRating;

  private final Integer maxRating;

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

  public static boolean isGoodReview(Integer rating) {
    return rating != null && rating >= 4;
  }

  public static boolean isMediumReview(Integer rating) {
    return rating != null && rating == 3;
  }

  public static boolean isBadReview(Integer rating) {
    return rating != null && rating <= 2;
  }
}
