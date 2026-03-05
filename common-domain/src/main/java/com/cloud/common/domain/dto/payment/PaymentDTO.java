package com.cloud.common.domain.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull(message = "Order ID cannot be null")
    private Long orderId;

    @Size(max = 64, message = "Order number length must be less than or equal to 64")
    private String orderNo;

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Status cannot be null")
    @Min(value = 0, message = "Status must be greater than or equal to 0")
    @Max(value = 4, message = "Status must be less than or equal to 4")
    private Integer status;

    @NotNull(message = "Channel cannot be null")
    @Min(value = 1, message = "Channel must be greater than or equal to 1")
    @Max(value = 3, message = "Channel must be less than or equal to 3")
    private Integer channel;

    @Size(max = 32, message = "Payment method length must be less than or equal to 32")
    private String paymentMethod;

    @Size(max = 100, message = "Transaction ID length must be less than or equal to 100")
    private String transactionId;

    @Size(max = 64, message = "Trace ID length must be less than or equal to 64")
    private String traceId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer deleted;
}
