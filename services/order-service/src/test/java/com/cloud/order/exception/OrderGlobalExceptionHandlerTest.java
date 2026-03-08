package com.cloud.order.exception;

import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderGlobalExceptionHandlerTest {

    private final OrderGlobalExceptionHandler handler = new OrderGlobalExceptionHandler();

    @Test
    void handleWrappedRuntimeExceptionShouldReturnNestedBusinessException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders");
        RuntimeException exception = new RuntimeException(
                "try to proceed invocation error",
                new BusinessException("insufficient salable stock")
        );

        Result<String> result = handler.handleWrappedRuntimeException(exception, request);

        assertThat(result.getCode()).isEqualTo(502);
        assertThat(result.getMessage()).isEqualTo("insufficient salable stock");
    }

    @Test
    void handleWrappedRuntimeExceptionShouldFallbackToSystemError() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders");

        Result<String> result = handler.handleWrappedRuntimeException(new RuntimeException("boom"), request);

        assertThat(result.getCode()).isEqualTo(1001);
        assertThat(result.getMessage()).isEqualTo("System error");
    }
}
