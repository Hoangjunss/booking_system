package com.geek.booking.service;

import com.geek.booking.dto.response.BookingItemResponse;
import com.geek.booking.entity.Booking;
import com.geek.booking.entity.BookingItem;
import com.geek.booking.entity.TicketCategory;
import com.geek.booking.repository.BookingItemRepository;
import com.geek.booking.repository.BookingRepository;
import com.geek.booking.repository.TicketCategoryRepository;
import com.geek.booking.service.impl.BookingItemServiceImpl;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingItemServiceTest {

    @Mock
    private BookingItemRepository repository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TicketCategoryRepository categoryRepository;
    @InjectMocks
    private BookingItemServiceImpl service;

    private BookingItem item;
    private Booking booking;
    private TicketCategory category;

    @BeforeEach
    void setUp() {
        booking = Booking.builder().id(1L).build();
        category = TicketCategory.builder().id(1L).name("VIP").build();
        item = BookingItem.builder().id(1L).booking(booking).ticketCategory(category).quantity(2).unitPrice(BigDecimal.valueOf(100)).build();
    }

    @Test
    void createBookingItem_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(repository.save(any())).thenReturn(item);
        BookingItem result = service.createBookingItem(1L, 1L, 2, BigDecimal.valueOf(100));
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getItemsByBookingId_ReturnsList() {
        when(repository.findByBookingId(1L)).thenReturn(List.of(item));
        List<BookingItemResponse> result = service.getItemsByBookingId(1L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryName()).isEqualTo("VIP");
    }

    @Test
    void getAllBookingItems_WithFilters_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BookingItem> page = new PageImpl<>(List.of(item));
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        Page<BookingItemResponse> result = service.getAllBookingItems(1L, 1L, BigDecimal.valueOf(50), BigDecimal.valueOf(150), pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}