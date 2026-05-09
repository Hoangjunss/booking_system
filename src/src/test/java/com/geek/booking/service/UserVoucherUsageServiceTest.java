package com.geek.booking.service;

import com.geek.booking.dto.request.user.UserVoucherUsageResponse;
import com.geek.booking.entity.Booking;
import com.geek.booking.entity.User;
import com.geek.booking.entity.UserVoucherUsage;
import com.geek.booking.entity.Voucher;
import com.geek.booking.exception.ResourceNotFoundException;
import com.geek.booking.repository.BookingRepository;
import com.geek.booking.repository.UserRepository;
import com.geek.booking.repository.UserVoucherUsageRepository;
import com.geek.booking.repository.VoucherRepository;
import com.geek.booking.service.impl.UserVoucherUsageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserVoucherUsageServiceTest {

    @Mock
    private UserVoucherUsageRepository usageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VoucherRepository voucherRepository;
    @Mock
    private BookingRepository bookingRepository;
    @InjectMocks
    private UserVoucherUsageServiceImpl service;

    private User user;
    private Voucher voucher;
    private Booking booking;
    private UserVoucherUsage usage;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).build();
        voucher = Voucher.builder().id(1L).build();
        booking = Booking.builder().id(1L).build();
        usage = UserVoucherUsage.builder().id(1L).user(user).voucher(voucher).booking(booking).build();
    }

    @Test
    void recordUsage_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        service.recordUsage(1L, 1L, 1L);
        // No exception thrown, success
    }

    @Test
    void recordUsage_UserNotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.recordUsage(99L, 1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void hasUserUsedVoucher_ReturnsTrue() {
        when(usageRepository.existsByUserIdAndVoucherId(1L, 1L)).thenReturn(true);
        boolean result = service.hasUserUsedVoucher(1L, 1L);
        assertThat(result).isTrue();
    }

    @Test
    void hasUserUsedVoucher_ReturnsFalse() {
        when(usageRepository.existsByUserIdAndVoucherId(1L, 1L)).thenReturn(false);
        boolean result = service.hasUserUsedVoucher(1L, 1L);
        assertThat(result).isFalse();
    }


}