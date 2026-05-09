package com.geek.booking.service;

import com.geek.booking.dto.request.booking.BookingCancelRequest;
import com.geek.booking.dto.request.booking.BookingCreateRequest;
import com.geek.booking.dto.response.BookingResponse;
import com.geek.booking.entity.*;
import com.geek.booking.enums.BookingStatus;
import com.geek.booking.enums.DiscountType;
import com.geek.booking.enums.UserRole;
import com.geek.booking.exception.*;
import com.geek.booking.mapper.BookingMapper;
import com.geek.booking.repository.BookingRepository;
import com.geek.booking.service.impl.BookingServiceImpl;
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
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TicketCategoryService ticketCategoryService;
    @Mock
    private VoucherService voucherService;
    @Mock
    private UserService userService;
    @Mock
    private ConcertService concertService;
    @Mock
    private BookingItemService bookingItemService;
    @Mock
    private UserVoucherUsageService usageService;
    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testUser;
    private Concert testConcert;
    private TicketCategory testCategory;
    private Voucher testVoucher;
    private Booking testBooking;
    private BookingResponse testResponse;
    private BookingCreateRequest createRequest;
    private BookingCancelRequest cancelRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("customer@example.com").role(UserRole.CUSTOMER).build();
        testConcert = Concert.builder().id(1L).name("Test Concert").build();
        testCategory = TicketCategory.builder()
                .id(1L)
                .concert(testConcert)
                .name("VIP")
                .price(BigDecimal.valueOf(100))
                .totalQuantity(10)
                .availableQuantity(10)
                .build();
        testVoucher = Voucher.builder()
                .id(1L)
                .code("TEST10")
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(10))
                .usageLimit(100)
                .usedCount(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .minOrderValue(BigDecimal.valueOf(50))
                .build();
        testBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .concert(testConcert)
                .status(BookingStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(180))
                .idempotencyKey("uuid-123")
                .build();
        testResponse = BookingResponse.builder()
                .bookingId(1L)
                .userId(1L)
                .concertId(1L)
                .status(BookingStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(180))
                .build();

        createRequest = new BookingCreateRequest();
        createRequest.setCategoryId(1L);
        createRequest.setQuantity(2);
        createRequest.setVoucherCode("TEST10");
        createRequest.setIdempotencyKey("test-uuid");

        cancelRequest = new BookingCancelRequest();
        cancelRequest.setReason("Changed mind");
    }

    @Test
    void createBooking_Success_WithVoucher() {
        when(bookingRepository.findByIdempotencyKey(createRequest.getIdempotencyKey())).thenReturn(Optional.empty());
        when(ticketCategoryService.getCategoryEntityWithLock(1L)).thenReturn(testCategory);
        when(voucherService.getVoucherByCodeWithLock("TEST10")).thenReturn(testVoucher);
        when(usageService.hasUserUsedVoucher(1L, 1L)).thenReturn(false);
        when(userService.getEntityById(1L)).thenReturn(testUser);
        when(concertService.getConcertEntityById(1L)).thenReturn(testConcert);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(bookingMapper.toResponse(testBooking)).thenReturn(testResponse);

        BookingResponse result = bookingService.createBooking(createRequest, 1L);

        assertThat(result.getBookingId()).isEqualTo(1L);
        verify(ticketCategoryService).reserveTicket(1L, 2);
        verify(voucherService).useVoucher(1L, 1L, 1L);
    }

    @Test
    void createBooking_InvalidVoucher_ThrowsException() {
        when(bookingRepository.findByIdempotencyKey(createRequest.getIdempotencyKey())).thenReturn(Optional.empty());
        when(ticketCategoryService.getCategoryEntityWithLock(1L)).thenReturn(testCategory);
        when(voucherService.getVoucherByCodeWithLock("TEST10")).thenThrow(new VoucherInvalidException("Voucher not found"));

        assertThatThrownBy(() -> bookingService.createBooking(createRequest, 1L))
                .isInstanceOf(VoucherInvalidException.class);
    }

    @Test
    void cancelBooking_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingItemService.getEntitiesByBookingId(1L)).thenReturn(List.of());

        bookingService.cancelBooking(1L, 1L, cancelRequest);

        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(testBooking);
    }

    @Test
    void cancelBooking_NotFound_ThrowsException() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookingService.cancelBooking(99L, 1L, cancelRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelBooking_NotOwner_ThrowsAccessDenied() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        assertThatThrownBy(() -> bookingService.cancelBooking(1L, 999L, cancelRequest))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getBookingById_NotFound_ThrowsException() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookingService.getBookingById(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserBookings_ReturnsList() {
        when(bookingRepository.findByUserId(1L)).thenReturn(List.of(testBooking));
        when(bookingMapper.toResponse(testBooking)).thenReturn(testResponse);

        List<BookingResponse> result = bookingService.getUserBookings(1L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBookingId()).isEqualTo(1L);
    }

    @Test
    void getAllBookings_WithFilters_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> bookingPage = new PageImpl<>(List.of(testBooking));
        when(bookingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(bookingPage);
        when(bookingMapper.toResponse(testBooking)).thenReturn(testResponse);

        Page<BookingResponse> result = bookingService.getAllBookings("PENDING", null, null, null, null, pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void updateBookingStatus_NotAdmin_ThrowsException() {
        when(userService.isAdmin(100L)).thenReturn(false);
        assertThatThrownBy(() -> bookingService.updateBookingStatus(1L, "PAID", 100L))
                .isInstanceOf(AccessDeniedException.class);
    }
}