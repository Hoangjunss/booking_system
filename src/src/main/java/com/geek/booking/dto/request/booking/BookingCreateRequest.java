package com.geek.booking.dto.request.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingCreateRequest {
    @NotNull(message = "Ticket category ID is required")
    private Long categoryId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private String voucherCode;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}