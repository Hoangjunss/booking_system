package com.geek.booking.dto.request.voucher;


import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherUpdateRequest {
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private Integer usageLimit;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private BigDecimal minOrderValue;
}