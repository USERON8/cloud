package com.cloud.order.service.support;

import com.cloud.api.stock.StockDubboApi;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockReservationRemoteService {

    @DubboReference(check = false, timeout = 5000, retries = 0)
    private StockDubboApi stockDubboApi;

    public Boolean reserve(StockOperateCommandDTO command) {
        return stockDubboApi.reserve(command);
    }
}
