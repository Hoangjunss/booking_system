package com.geek.booking.dto.request.voucher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherCreateRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String discountType;
    @NotNull
    private BigDecimal discountValue;
    private Integer usageLimit = 1;
    @NotNull
    private LocalDateTime validFrom;
    @NotNull
    private LocalDateTime validTo;
    private BigDecimal minOrderValue = BigDecimal.ZERO;
}