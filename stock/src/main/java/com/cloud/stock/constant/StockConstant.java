package com.cloud.stock.constant;

/**
 * 库存相关常量
 */
public class StockConstant {

    /**
     * 库存状态
     */
    public static class Status {
        public static final int OUT_OF_STOCK = 0;      // 缺货
        public static final int LOW_STOCK = 1;         // 库存不足
        public static final int SUFFICIENT_STOCK = 2;  // 库存充足
    }

    /**
     * 库存阈值
     */
    public static class Threshold {
        public static final int LOW_STOCK_THRESHOLD = 10; // 低库存阈值
    }

    /**
     * 库存状态描述
     */
    public static class StatusDesc {
        public static final String OUT_OF_STOCK = "缺货";
        public static final String LOW_STOCK = "库存不足";
        public static final String SUFFICIENT_STOCK = "库存充足";
        public static final String UNKNOWN = "未知";
    }
}