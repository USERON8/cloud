package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.module.entity.Order;
import com.cloud.order.service.OrderExportService;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExportServiceImpl implements OrderExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final OrderService orderService;

    @Override
    public void exportOrdersToExcel(List<Order> orders, OutputStream outputStream) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Orders");
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {
                    "Order ID", "Order No", "User ID", "Total Amount", "Pay Amount", "Order Status",
                    "Refund Status", "Address ID", "Pay Time", "Ship Time", "Complete Time", "Cancel Time",
                    "Cancel Reason", "Remark", "Shop ID", "Created At", "Updated At"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5200);
            }

            int rowNum = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowNum++);
                int col = 0;
                row.createCell(col++).setCellValue(safeLong(order.getId()));
                row.createCell(col++).setCellValue(safeString(order.getOrderNo()));
                row.createCell(col++).setCellValue(safeLong(order.getUserId()));
                row.createCell(col++).setCellValue(order.getTotalAmount() == null ? 0D : order.getTotalAmount().doubleValue());
                row.createCell(col++).setCellValue(order.getPayAmount() == null ? 0D : order.getPayAmount().doubleValue());
                row.createCell(col++).setCellValue(getStatusName(order.getStatus()));
                row.createCell(col++).setCellValue(getRefundStatusName(order.getRefundStatus()));
                row.createCell(col++).setCellValue(safeLong(order.getAddressId()));
                row.createCell(col++).setCellValue(formatDateTime(order.getPayTime()));
                row.createCell(col++).setCellValue(formatDateTime(order.getShipTime()));
                row.createCell(col++).setCellValue(formatDateTime(order.getCompleteTime()));
                row.createCell(col++).setCellValue(formatDateTime(order.getCancelTime()));
                row.createCell(col++).setCellValue(safeString(order.getCancelReason()));
                row.createCell(col++).setCellValue(safeString(order.getRemark()));
                row.createCell(col++).setCellValue(safeLong(order.getShopId()));
                row.createCell(col++).setCellValue(formatDateTime(order.getCreatedAt()));
                row.createCell(col).setCellValue(formatDateTime(order.getUpdatedAt()));
            }

            workbook.write(outputStream);
        } catch (Exception e) {
            log.error("Export orders to excel failed", e);
            throw new BusinessException("Export orders to excel failed", e);
        }
    }

    @Override
    public void exportOrdersByConditions(String orderStatus, LocalDateTime startTime,
                                         LocalDateTime endTime, OutputStream outputStream) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();

        if (orderStatus != null && !orderStatus.isBlank()) {
            try {
                queryWrapper.eq(Order::getStatus, Integer.parseInt(orderStatus));
            } catch (NumberFormatException e) {
                log.warn("Ignore invalid order status filter: {}", orderStatus);
            }
        }

        if (startTime != null) {
            queryWrapper.ge(Order::getCreatedAt, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(Order::getCreatedAt, endTime);
        }

        queryWrapper.orderByDesc(Order::getCreatedAt);
        List<Order> orders = orderService.list(queryWrapper);
        exportOrdersToExcel(orders, outputStream);
    }

    @Override
    public void exportOrderDetail(Long orderId, OutputStream outputStream) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException("Order not found");
        }
        exportOrdersToExcel(List.of(order), outputStream);
    }

    @Override
    public void generateExportTemplate(OutputStream outputStream) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Order Template");
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {
                    "Order ID", "Order No", "User ID", "Total Amount", "Pay Amount",
                    "Order Status", "Address ID", "Remark"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5200);
            }

            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("1");
            sample.createCell(1).setCellValue("ORD202601010001");
            sample.createCell(2).setCellValue("10001");
            sample.createCell(3).setCellValue("99.90");
            sample.createCell(4).setCellValue("99.90");
            sample.createCell(5).setCellValue("Pending payment");
            sample.createCell(6).setCellValue("20001");
            sample.createCell(7).setCellValue("Template row");

            workbook.write(outputStream);
        } catch (Exception e) {
            log.error("Generate export template failed", e);
            throw new BusinessException("Generate export template failed", e);
        }
    }

    @Override
    public String getExportFileName(String prefix) {
        String safePrefix = (prefix == null || prefix.isBlank()) ? "orders" : prefix;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return safePrefix + "_" + timestamp + ".xlsx";
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? "" : time.format(DATE_TIME_FORMATTER);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private String getStatusName(Integer status) {
        if (status == null) {
            return "Unknown";
        }
        return switch (status) {
            case 0 -> "Pending payment";
            case 1 -> "Paid";
            case 2 -> "Shipped";
            case 3 -> "Completed";
            case 4 -> "Cancelled";
            default -> "Unknown(" + status + ")";
        };
    }

    private String getRefundStatusName(Integer refundStatus) {
        if (refundStatus == null) {
            return "None";
        }
        return switch (refundStatus) {
            case 0 -> "Applying";
            case 1 -> "Refunding";
            case 2 -> "Refund success";
            case 3 -> "Refund failed";
            case 4 -> "Refund closed";
            default -> "Unknown(" + refundStatus + ")";
        };
    }
}
