package com.geek.booking.service.impl;


import com.geek.booking.dto.request.voucher.VoucherCreateRequest;
import com.geek.booking.dto.response.VoucherResponse;
import com.geek.booking.entity.Voucher;
import com.geek.booking.enums.DiscountType;
import com.geek.booking.exception.BusinessException;
import com.geek.booking.exception.ResourceNotFoundException;
import com.geek.booking.mapper.VoucherMapper;
import com.geek.booking.repository.VoucherRepository;
import com.geek.booking.service.UserVoucherUsageService;
import com.geek.booking.service.VoucherService;


import com.geek.booking.specification.VoucherSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherMapper mapper;
    private final UserVoucherUsageService usageService;

    @Override
    @Transactional
    public VoucherResponse createVoucher(VoucherCreateRequest request) {
        Voucher voucher = mapper.toEntity(request);
        voucher = voucherRepository.save(voucher);
        log.info("Voucher created: code={}", voucher.getCode());
        return mapper.toResponse(voucher);
    }

    @Override
    public Page<VoucherResponse> getAllVouchers(String code, String discountType, Boolean isValid,
                                                LocalDateTime validFrom, LocalDateTime validTo, Pageable pageable) {
        DiscountType type = discountType != null ? DiscountType.valueOf(discountType.toUpperCase()) : null;
        return voucherRepository.findAll(VoucherSpecification.filterBy(code, type, isValid,
                        validFrom, validTo), pageable)
                .map(mapper::toResponse);
    }
    @Override
    public VoucherResponse getVoucherById(Long id) {
        return mapper.toResponse(voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found")));
    }

    @Override
    public VoucherResponse getVoucherByCode(String code) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        return mapper.toResponse(voucher);
    }

    @Override
    public Voucher getVoucherByCodeWithLock(String code) {
        return voucherRepository.findByCodeWithLock(code)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
    }

    @Override
    @Transactional
    public void useVoucher(Long voucherId, Long userId, Long bookingId) {
        Voucher voucher = voucherRepository.findByIdWithLock(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        if (voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new BusinessException("Voucher usage limit reached");
        }
        if (usageService.hasUserUsedVoucher(userId, voucherId)) {
            throw new BusinessException("User already used this voucher");
        }
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);
        usageService.recordUsage(userId, voucherId, bookingId);
        log.info("Voucher {} used by user {}", voucherId, userId);
    }

}