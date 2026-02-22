package com.cloud.order.service;

import com.cloud.order.module.entity.Order;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;






public interface OrderExportService {
    





    void exportOrdersToExcel(List<Order> orders, OutputStream outputStream);

    







    void exportOrdersByConditions(String orderStatus, LocalDateTime startTime,
                                  LocalDateTime endTime, OutputStream outputStream);

    





    void exportOrderDetail(Long orderId, OutputStream outputStream);

    




    void generateExportTemplate(OutputStream outputStream);

    





    String getExportFileName(String prefix);
}
