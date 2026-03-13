package com.cloud.order.service;

public interface LogisticsProvider {
    LogisticsInfo query(String trackingNumber);
}
