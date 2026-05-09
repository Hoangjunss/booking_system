package com.geek.booking.service;

import com.geek.booking.dto.request.voucher.VoucherCreateRequest;
import com.geek.booking.dto.response.VoucherResponse;
import com.geek.booking.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;


public interface VoucherService {
    VoucherResponse createVoucher(VoucherCreateRequest request);
    Page<VoucherResponse> getAllVouchers(String code, String discountType, Boolean isValid,
                                         LocalDateTime validFrom, LocalDateTime validTo, Pageable pageable);
    VoucherResponse getVoucherById(Long id);
    VoucherResponse getVoucherByCode(String code);
    Voucher getVoucherByCodeWithLock(String code);
    void useVoucher(Long voucherId, Long userId, Long bookingId);
}