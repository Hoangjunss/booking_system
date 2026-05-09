package com.geek.booking.service.impl;

import com.geek.booking.dto.request.user.UserVoucherUsageResponse;
import com.geek.booking.entity.Booking;
import com.geek.booking.entity.User;
import com.geek.booking.entity.UserVoucherUsage;
import com.geek.booking.entity.Voucher;
import com.geek.booking.exception.ResourceNotFoundException;
import com.geek.booking.mapper.UserMapper;
import com.geek.booking.mapper.UserVoucherUsageMapper;
import com.geek.booking.repository.BookingRepository;
import com.geek.booking.repository.UserRepository;
import com.geek.booking.repository.UserVoucherUsageRepository;
import com.geek.booking.repository.VoucherRepository;
import com.geek.booking.service.UserVoucherUsageService;
import com.geek.booking.specification.UserVoucherUsageSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserVoucherUsageServiceImpl implements UserVoucherUsageService {

    private final UserVoucherUsageRepository usageRepository;
    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;
    private final BookingRepository bookingRepository;
    private final UserVoucherUsageMapper userVoucherUsageMapper;

    @Override
    @Transactional
    public void recordUsage(Long userId, Long voucherId, Long bookingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        UserVoucherUsage usage = UserVoucherUsage.builder()
                .user(user)
                .voucher(voucher)
                .booking(booking)
                .build();
        usageRepository.save(usage);
    }

    @Override
    public boolean hasUserUsedVoucher(Long userId, Long voucherId) {
        return usageRepository.existsByUserIdAndVoucherId(userId, voucherId);
    }
    @Override
    public Page<UserVoucherUsageResponse> getAllUsages(Long userId, Long voucherId, Long bookingId,
                                                       LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        return usageRepository.findAll(UserVoucherUsageSpecification.filterBy(userId, voucherId,
                        bookingId, fromDate, toDate), pageable)
                .map(userVoucherUsageMapper::toResponse);
    }
}