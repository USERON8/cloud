package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.module.entity.Order;
import com.cloud.order.service.OrderExportService;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 订单导出服务实现
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExportServiceImpl implements OrderExportService {

    private final OrderService orderService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void exportOrdersToExcel(List<Order> orders, OutputStream outputStream) {
        log.info("开始导出订单到Excel, 订单数量: {}", orders.size());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("订单列表");

            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "订单ID", "订单号", "用户ID", "用户昵称", "商品名称",
                    "商品数量", "订单金额", "实付金额", "订单状态",
                    "支付方式", "支付时间", "发货时间", "完成时间",
                    "收货人", "收货电话", "收货地址", "创建时间"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);  // 设置列宽
            }

            // 填充数据
            int rowNum = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;

                row.createCell(colNum++).setCellValue(order.getId());
                row.createCell(colNum++).setCellValue(order.getOrderNo());
                row.createCell(colNum++).setCellValue(order.getUserId());
                row.createCell(colNum++).setCellValue(order.getUserNickname());
                row.createCell(colNum++).setCellValue(order.getProductName());
                row.createCell(colNum++).setCellValue(order.getProductQuantity());
                row.createCell(colNum++).setCellValue(order.getTotalAmount() != null ?
                        order.getTotalAmount().doubleValue() : 0.0);
                row.createCell(colNum++).setCellValue(order.getActualAmount() != null ?
                        order.getActualAmount().doubleValue() : 0.0);
                row.createCell(colNum++).setCellValue(order.getOrderStatus());
                row.createCell(colNum++).setCellValue(order.getPaymentMethod());
                row.createCell(colNum++).setCellValue(formatDateTime(order.getPaymentTime()));
                row.createCell(colNum++).setCellValue(formatDateTime(order.getShipmentTime()));
                row.createCell(colNum++).setCellValue(formatDateTime(order.getCompletionTime()));
                row.createCell(colNum++).setCellValue(order.getReceiverName());
                row.createCell(colNum++).setCellValue(order.getReceiverPhone());
                row.createCell(colNum++).setCellValue(order.getReceiverAddress());
                row.createCell(colNum++).setCellValue(formatDateTime(order.getCreatedAt()));
            }

            // 写入输出流
            workbook.write(outputStream);
            log.info("订单导出完成");

        } catch (Exception e) {
            log.error("导出订单到Excel失败", e);
            throw new BusinessException("导出订单失败", e);
        }
    }

    @Override
    public void exportOrdersByConditions(String orderStatus, LocalDateTime startTime,
                                          LocalDateTime endTime, OutputStream outputStream) {
        log.info("根据条件导出订单, orderStatus: {}, startTime: {}, endTime: {}",
                orderStatus, startTime, endTime);

        // 构造查询条件
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();

        if (orderStatus != null && !orderStatus.isEmpty()) {
            queryWrapper.eq(Order::getOrderStatus, orderStatus);
        }

        if (startTime != null) {
            queryWrapper.ge(Order::getCreatedAt, startTime);
        }

        if (endTime != null) {
            queryWrapper.le(Order::getCreatedAt, endTime);
        }

        queryWrapper.orderByDesc(Order::getCreatedAt);

        // 查询订单列表
        List<Order> orders = orderService.list(queryWrapper);

        if (orders.isEmpty()) {
            log.warn("没有符合条件的订单");
        }

        // 导出到Excel
        exportOrdersToExcel(orders, outputStream);
    }

    @Override
    public void exportOrderDetail(Long orderId, OutputStream outputStream) {
        log.info("导出订单详情, orderId: {}", orderId);

        // 查询订单详情
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 导出单个订单
        exportOrdersToExcel(List.of(order), outputStream);
    }

    @Override
    public void generateExportTemplate(OutputStream outputStream) {
        log.info("生成订单导出模板");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("订单模板");

            // 创建表头
            CellStyle headerStyle = createHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);

            String[] headers = {
                    "订单ID", "订单号", "用户ID", "用户昵称", "商品名称",
                    "商品数量", "订单金额", "实付金额", "订单状态",
                    "支付方式", "收货人", "收货电话", "收货地址"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // 添加一行示例数据
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("示例数据");

            workbook.write(outputStream);
            log.info("订单导出模板生成完成");

        } catch (Exception e) {
            log.error("生成订单导出模板失败", e);
            throw new BusinessException("生成模板失败", e);
        }
    }

    @Override
    public String getExportFileName(String prefix) {
        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("%s_%s.xlsx", prefix, timestamp);
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 设置背景色
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 设置边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 设置字体
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        // 设置对齐
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * 格式化日期时间
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "";
    }
}
