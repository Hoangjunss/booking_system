package com.geek.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCategoryResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer totalQuantity;
    private Integer availableQuantity;
}