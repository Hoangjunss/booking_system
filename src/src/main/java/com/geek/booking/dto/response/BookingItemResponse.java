package com.geek.booking.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class BookingItemResponse {
    private Long id;
    private Long bookingId;
    private Long ticketCategoryId;
    private String categoryName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}