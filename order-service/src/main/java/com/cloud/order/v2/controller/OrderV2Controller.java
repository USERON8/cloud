package com.cloud.order.v2.controller;

import com.cloud.common.result.Result;
import com.cloud.order.v2.dto.CreateMainOrderRequest;
import com.cloud.order.v2.entity.AfterSaleV2;
import com.cloud.order.v2.entity.OrderMainV2;
import com.cloud.order.v2.entity.OrderSubV2;
import com.cloud.order.v2.service.OrderV2Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class OrderV2Controller {

    private final OrderV2Service orderV2Service;

    @PostMapping("/order-main")
    public Result<OrderMainV2> createMainOrder(@RequestBody @Valid CreateMainOrderRequest request) {
        return Result.success(orderV2Service.createMainOrder(request));
    }

    @GetMapping("/order-main/{mainOrderId}/sub-orders")
    public Result<List<OrderSubV2>> listSubOrders(@PathVariable Long mainOrderId) {
        return Result.success(orderV2Service.listSubOrders(mainOrderId));
    }

    @PostMapping("/order-sub/{subOrderId}/actions/{action}")
    public Result<OrderSubV2> advanceSubOrderStatus(@PathVariable Long subOrderId,
                                                    @PathVariable String action) {
        return Result.success(orderV2Service.advanceSubOrderStatus(subOrderId, action));
    }

    @PostMapping("/after-sales")
    public Result<AfterSaleV2> applyAfterSale(@RequestBody AfterSaleV2 afterSale) {
        return Result.success(orderV2Service.applyAfterSale(afterSale));
    }

    @PostMapping("/after-sales/{afterSaleId}/actions/{action}")
    public Result<AfterSaleV2> advanceAfterSaleStatus(@PathVariable Long afterSaleId,
                                                      @PathVariable String action,
                                                      @RequestParam(required = false) String remark) {
        return Result.success(orderV2Service.advanceAfterSaleStatus(afterSaleId, action, remark));
    }
}

