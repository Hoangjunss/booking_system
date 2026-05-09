package com.geek.booking.controller;

import com.geek.booking.dto.response.BookingResponse;
import com.geek.booking.security.UserPrincipal;
import com.geek.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
@Tag(name = "Admin Booking", description = "Admin APIs for booking management")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    @Operation(summary = "Get all bookings with filters")
    public ResponseEntity<Page<BookingResponse>> getAllBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long concertId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            Pageable pageable) {
        return ResponseEntity.ok(bookingService.getAllBookings(status, userId, concertId, fromDate, toDate, pageable));
    }

    @PutMapping("/{bookingId}/status")
    @Operation(summary = "Update booking status")
    public ResponseEntity<Void> updateBookingStatus(
            @PathVariable Long bookingId,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal admin) {
        bookingService.updateBookingStatus(bookingId, status, admin.getId());
        return ResponseEntity.noContent().build();
    }
}