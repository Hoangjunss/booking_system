package com.geek.booking.dto.request.ticketCategory;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TicketCategoryUpdateRequest {
    private String name;
    private BigDecimal price;
    private Integer totalQuantity;
}