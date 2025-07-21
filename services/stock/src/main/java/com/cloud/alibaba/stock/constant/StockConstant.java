package com.cloud.alibaba.stock.constant;

/**
 * 库存相关常量
 */
public class StockConstant {

    /**
     * 库存状态
     */
    public static class Status {
        /**
         * 缺货
         */
        public static final Integer OUT_OF_STOCK = 0;
        /**
         * 库存不足
         */
        public static final Integer LOW_STOCK = 1;
        /**
         * 库存充足
         */
        public static final Integer SUFFICIENT_STOCK = 2;
    }

    /**
     * 库存阈值
     */
    public static class Threshold {
        /**
         * 低库存阈值
         */
        public static final Integer LOW_STOCK_THRESHOLD = 10;
    }

    /**
     * 排序字段
     */
    public static class OrderBy {
        public static final String STOCK_COUNT = "stock_count";
        public static final String AVAILABLE_COUNT = "available_count";
        public static final String FROZEN_COUNT = "frozen_count";
        public static final String CREATE_TIME = "create_time";
        public static final String UPDATE_TIME = "update_time";
    }

    /**
     * 库存状态描述
     */
    public static class StatusDesc {
        public static final String OUT_OF_STOCK = "缺货";
        public static final String LOW_STOCK = "不足";
        public static final String SUFFICIENT_STOCK = "充足";
        public static final String UNKNOWN = "未知";
    }
}
