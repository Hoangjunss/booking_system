package com.geek.booking.service;

import com.geek.booking.dto.response.BookingItemResponse;
import com.geek.booking.entity.BookingItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;


public interface BookingItemService {
    BookingItem createBookingItem(Long bookingId, Long categoryId, int quantity, java.math.BigDecimal unitPrice);
    List<BookingItemResponse> getItemsByBookingId(Long bookingId);
    List<BookingItem> getEntitiesByBookingId(Long bookingId);
    Page<BookingItemResponse> getAllBookingItems(Long bookingId, Long categoryId, BigDecimal minUnitPrice,
                                                 BigDecimal maxUnitPrice, Pageable pageable);
}