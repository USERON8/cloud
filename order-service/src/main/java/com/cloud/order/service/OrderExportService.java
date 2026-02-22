package com.cloud.order.service;

import com.cloud.order.module.entity.Order;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单导出服务接口
 *
 * @author what's up
 */
public interface OrderExportService {
    /**
     * 导出订单到Excel
     *
     * @param orders       订单列表
     * @param outputStream 输出流
     */
    void exportOrdersToExcel(List<Order> orders, OutputStream outputStream);

    /**
     * 根据条件导出订单
     *
     * @param orderStatus  订单状态(可选)
     * @param startTime    开始时间(可选)
     * @param endTime      结束时间(可选)
     * @param outputStream 输出流
     */
    void exportOrdersByConditions(String orderStatus, LocalDateTime startTime,
                                  LocalDateTime endTime, OutputStream outputStream);

    /**
     * 导出单个订单详情到Excel
     *
     * @param orderId      订单ID
     * @param outputStream 输出流
     */
    void exportOrderDetail(Long orderId, OutputStream outputStream);

    /**
     * 生成订单Excel导出模板
     *
     * @param outputStream 输出流
     */
    void generateExportTemplate(OutputStream outputStream);

    /**
     * 获取订单导出文件名
     *
     * @param prefix 文件名前缀
     * @return 文件名
     */
    String getExportFileName(String prefix);
}
