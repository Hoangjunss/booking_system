package com.geek.booking.dto.request.booking;

import com.geek.booking.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingStatusUpdateRequest {
    @NotNull
    private BookingStatus status;
}