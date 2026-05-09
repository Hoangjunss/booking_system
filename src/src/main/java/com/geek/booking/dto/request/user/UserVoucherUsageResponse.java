package com.geek.booking.dto.request.user;


import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserVoucherUsageResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private Long voucherId;
    private String voucherCode;
    private Long bookingId;
    private LocalDateTime usedAt;
}