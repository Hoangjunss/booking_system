package com.geek.booking.controller;

import com.geek.booking.dto.request.booking.BookingCancelRequest;
import com.geek.booking.dto.request.booking.BookingCreateRequest;
import com.geek.booking.dto.response.BookingResponse;
import com.geek.booking.security.UserPrincipal;
import com.geek.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Customer Booking", description = "Booking APIs for customers")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerBookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create a new booking")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingCreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.createBooking(request, currentUser.getId()));
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel a pending booking")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody BookingCancelRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        bookingService.cancelBooking(bookingId, currentUser.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId, currentUser.getId()));
    }

    @GetMapping("/user")
    @Operation(summary = "Get all my bookings")
    public ResponseEntity<List<BookingResponse>> getUserBookings(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.getUserBookings(currentUser.getId()));
    }
}