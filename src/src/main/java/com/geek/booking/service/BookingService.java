package com.geek.booking.service;


import com.geek.booking.dto.request.booking.BookingCancelRequest;
import com.geek.booking.dto.request.booking.BookingCreateRequest;
import com.geek.booking.dto.response.BookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
public interface BookingService {

    BookingResponse createBooking(BookingCreateRequest request, Long userId);
    void cancelBooking(Long bookingId, Long userId, BookingCancelRequest request);
    BookingResponse getBookingById(Long bookingId, Long userId);
    List<BookingResponse> getUserBookings(Long userId);

    Page<BookingResponse> getAllBookings(String status, Long userId, Long concertId,
                                         LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);

    void updateBookingStatus(Long bookingId, String newStatus, Long adminId);
}