package com.geek.booking.repository;


import com.geek.booking.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long>, JpaSpecificationExecutor<BookingItem> {
    List<BookingItem> findByBookingId(Long bookingId);
}