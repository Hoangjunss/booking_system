package com.geek.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookingCreationResult {
    private final BookingResponse bookingResponse;
    private final boolean isDuplicate;
}