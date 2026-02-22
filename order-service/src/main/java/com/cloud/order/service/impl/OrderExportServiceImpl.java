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

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final OrderService orderService;

    @Override
    public void exportOrdersToExcel(List<Order> orders, OutputStream outputStream) {
        log.info("开始导出订单到Excel, 订单数量: {}", orders.size());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("订单列表");

            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 创建表头 - 只使用Order实体中实际存在的字段
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "订单ID", "订单号", "用户ID", "订单金额", "实付金额",
                    "订单状态", "退款状态", "收货地址ID", "支付时间",
                    "发货时间", "完成时间", "取消时间", "取消原因",
                    "备注", "店铺ID", "创建时间", "更新时间"
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
                row.createCell(colNum++).setCellValue(order.getTotalAmount() != null ?
                        order.getTotalAmount().doubleValue() : 0.0);
                row.createCell(colNum++).setCellValue(order.getPayAmount() != null ?
                        order.getPayAmount().doubleValue() : 0.0);
                row.createCell(colNum++).setCellValue(getStatusName(order.getStatus()));
                row.createCell(colNum++).setCellValue(getRefundStatusName(order.getRefundStatus()));
                row.createCell(colNum++).setCellValue(order.getAddressId() != null ? order.getAddressId() : 0);
                row.createCell(colNum++).setCellValue(formatDateTime(order.getPayTime()));
                row.createCell(colNum++).setCellValue(formatDateTime(order.getShipTime()));
                row.createCell(colNum++).setCellValue(formatDateTime(order.getCompleteTime()));
                row.createCell(colNum++).setCellValue(formatDateTime(order.getCancelTime()));
                row.createCell(colNum++).setCellValue(order.getCancelReason() != null ? order.getCancelReason() : "");
                row.createCell(colNum++).setCellValue(order.getRemark() != null ? order.getRemark() : "");
                row.createCell(colNum++).setCellValue(order.getShopId() != null ? order.getShopId() : 0);
                row.createCell(colNum++).setCellValue(formatDateTime(order.getCreatedAt()));
                row.createCell(colNum++).setCellValue(formatDateTime(order.getUpdatedAt()));
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
            try {
                Integer status = Integer.parseInt(orderStatus);
                queryWrapper.eq(Order::getStatus, status);
            } catch (NumberFormatException e) {
                log.warn("订单状态格式错误: {}", orderStatus);
            }
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
                    "订单ID", "订单号", "用户ID", "订单金额", "实付金额",
                    "订单状态", "收货地址ID", "备注"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // 添加一行示例数据
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("示例: 1");
            exampleRow.createCell(1).setCellValue("示例: ORD2025011600001");
            exampleRow.createCell(2).setCellValue("示例: 10001");
            exampleRow.createCell(3).setCellValue("示例: 999.00");
            exampleRow.createCell(4).setCellValue("示例: 999.00");
            exampleRow.createCell(5).setCellValue("示例: 待支付");

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

    /**
     * 获取订单状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "待支付";
            case 1:
                return "已支付";
            case 2:
                return "已发货";
            case 3:
                return "已完成";
            case 4:
                return "已取消";
            case 5:
                return "退款中";
            default:
                return "未知(" + status + ")";
        }
    }

    /**
     * 获取退款状态名称
     */
    private String getRefundStatusName(Integer refundStatus) {
        if (refundStatus == null) {
            return "无";
        }
        switch (refundStatus) {
            case 0:
                return "无退款";
            case 1:
                return "退款中";
            case 2:
                return "退款成功";
            case 3:
                return "退款失败";
            default:
                return "未知(" + refundStatus + ")";
        }
    }
}
