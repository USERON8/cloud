package org.example.redis;

public class UnsafeRedisValue {

  private String payload;

  public UnsafeRedisValue() {}

  public UnsafeRedisValue(String payload) {
    this.payload = payload;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }
}
