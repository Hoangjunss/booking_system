package com.geek.booking.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.geek.booking.enums.BookingStatus;

@Data
@Builder
public class BookingResponse {
    private Long bookingId;
    private Long userId;
    private Long concertId;
    private String concertName;
    private BookingStatus status;
    private BigDecimal totalPrice;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}