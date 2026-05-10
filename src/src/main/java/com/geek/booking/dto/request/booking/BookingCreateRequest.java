package com.geek.booking.dto.request.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BookingCreateRequest {
    @NotEmpty(message = "At least one ticket item is required")
    @Valid
    private List<BookingItemRequest> items;

    private String voucherCode;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}