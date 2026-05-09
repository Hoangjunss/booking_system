package com.geek.booking.service;

import com.geek.booking.dto.request.user.UserVoucherUsageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface UserVoucherUsageService {
    void recordUsage(Long userId, Long voucherId, Long bookingId);
    boolean hasUserUsedVoucher(Long userId, Long voucherId);
    Page<UserVoucherUsageResponse> getAllUsages(Long userId, Long voucherId, Long bookingId,
                                                LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);
}