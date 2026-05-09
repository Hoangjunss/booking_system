package com.geek.booking.service;

import com.geek.booking.dto.request.voucher.VoucherCreateRequest;
import com.geek.booking.dto.response.VoucherResponse;
import com.geek.booking.entity.Voucher;
import com.geek.booking.enums.DiscountType;
import com.geek.booking.exception.BusinessException;
import com.geek.booking.exception.ResourceNotFoundException;
import com.geek.booking.mapper.VoucherMapper;
import com.geek.booking.repository.VoucherRepository;
import com.geek.booking.service.impl.VoucherServiceImpl;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;
    @Mock
    private VoucherMapper voucherMapper;
    @Mock
    private UserVoucherUsageService usageService;
    @InjectMocks
    private VoucherServiceImpl voucherService;

    private Voucher voucher;
    private VoucherResponse response;

    @BeforeEach
    void setUp() {
        voucher = Voucher.builder()
                .id(1L).code("TEST10").discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.TEN).usageLimit(100).usedCount(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .minOrderValue(BigDecimal.valueOf(50))
                .build();
        response = VoucherResponse.builder().id(1L).code("TEST10").build();
    }

    @Test
    void createVoucher_Success() {
        VoucherCreateRequest request = new VoucherCreateRequest();
        request.setCode("NEW");
        request.setDiscountType("PERCENT");
        request.setDiscountValue(BigDecimal.valueOf(15));
        request.setValidFrom(LocalDateTime.now());
        request.setValidTo(LocalDateTime.now().plusDays(30));
        when(voucherMapper.toEntity(request)).thenReturn(voucher);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(voucher);
        when(voucherMapper.toResponse(voucher)).thenReturn(response);
        VoucherResponse result = voucherService.createVoucher(request);
        assertThat(result.getCode()).isEqualTo("TEST10");
    }

    @Test
    void getVoucherById_Success() {
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));
        when(voucherMapper.toResponse(voucher)).thenReturn(response);
        VoucherResponse result = voucherService.getVoucherById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getVoucherById_NotFound_ThrowsException() {
        when(voucherRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> voucherService.getVoucherById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getVoucherByCode_Success() {
        when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));
        when(voucherMapper.toResponse(voucher)).thenReturn(response);
        VoucherResponse result = voucherService.getVoucherByCode("TEST10");
        assertThat(result.getCode()).isEqualTo("TEST10");
    }

    @Test
    void getVoucherByCode_NotFound_ThrowsException() {
        when(voucherRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> voucherService.getVoucherByCode("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getVoucherByCodeWithLock_Success() {
        when(voucherRepository.findByCodeWithLock("TEST10")).thenReturn(Optional.of(voucher));
        Voucher result = voucherService.getVoucherByCodeWithLock("TEST10");
        assertThat(result.getCode()).isEqualTo("TEST10");
    }

    @Test
    void useVoucher_Success() {
        when(voucherRepository.findByIdWithLock(1L)).thenReturn(Optional.of(voucher));
        when(usageService.hasUserUsedVoucher(1L, 1L)).thenReturn(false);
        voucherService.useVoucher(1L, 1L, 10L);
        assertThat(voucher.getUsedCount()).isEqualTo(1);
        verify(voucherRepository).save(voucher);
        verify(usageService).recordUsage(1L, 1L, 10L);
    }

    @Test
    void useVoucher_AlreadyUsedByUser_ThrowsException() {
        when(voucherRepository.findByIdWithLock(1L)).thenReturn(Optional.of(voucher));
        when(usageService.hasUserUsedVoucher(1L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> voucherService.useVoucher(1L, 1L, 10L))
                .isInstanceOf(BusinessException.class);
        verify(voucherRepository, never()).save(any());
    }

    @Test
    void useVoucher_LimitReached_ThrowsException() {
        voucher.setUsedCount(100);
        when(voucherRepository.findByIdWithLock(1L)).thenReturn(Optional.of(voucher));
        assertThatThrownBy(() -> voucherService.useVoucher(1L, 1L, 10L))
                .isInstanceOf(BusinessException.class);
    }



    @Test
    void getAllVouchers_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Voucher> page = new PageImpl<>(List.of(voucher));
        when(voucherRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(voucherMapper.toResponse(voucher)).thenReturn(response);
        Page<VoucherResponse> result = voucherService.getAllVouchers("TEST", "PERCENT", true, null, null, pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}