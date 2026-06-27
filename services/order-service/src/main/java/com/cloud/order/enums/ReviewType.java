package com.cloud.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewType {
  PRODUCT(1, "商品评价"),

  SHOP(2, "店铺评价");

  private final Integer code;

  private final String description;

  public static ReviewType fromCode(Integer code) {
    if (code == null) {
      return null;
    }
    for (ReviewType type : ReviewType.values()) {
      if (type.getCode().equals(code)) {
        return type;
      }
    }
    throw new IllegalArgumentException("未知的评价类型: " + code);
  }
}
