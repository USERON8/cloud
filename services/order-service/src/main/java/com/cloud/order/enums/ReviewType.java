package com.cloud.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewType {
  PRODUCT(1, "鍟嗗搧璇勪环"),

  SHOP(2, "搴楅摵璇勪环");

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
    throw new IllegalArgumentException("鏈煡鐨勮瘎浠风被鍨? " + code);
  }
}
