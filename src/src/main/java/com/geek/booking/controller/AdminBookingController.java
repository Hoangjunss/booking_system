package com.geek.booking.controller;

import com.geek.booking.dto.response.BookingResponse;
import com.geek.booking.security.UserPrincipal;
import com.geek.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
@Tag(name = "Admin Booking", description = "APIs for administrators to view and manage all bookings.")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    @Operation(summary = "Get all bookings (paginated and filtered)",
            description = "Retrieve a page of bookings with optional filters. Only accessible by ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved page of bookings"),
            @ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden – user does not have ADMIN role", content = @Content)
    })
    public ResponseEntity<Page<BookingResponse>> getAllBookings(
            @Parameter(description = "Filter by booking status (PENDING, PAID, CANCELLED, FAILED, EXPIRED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by user ID")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Filter by concert ID")
            @RequestParam(required = false) Long concertId,
            @Parameter(description = "Start date (ISO 8601) for creation time")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @Parameter(description = "End date (ISO 8601) for creation time")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bookingService.getAllBookings(status, userId, concertId, fromDate, toDate, pageable));
    }

    @PutMapping("/{bookingId}/status")
    @Operation(summary = "Update booking status",
            description = "Allowed transitions: PENDING → PAID/CANCELLED/FAILED; PAID → CANCELLED. Returns the updated booking object.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully, returns booking details"),
            @ApiResponse(responseCode = "400", description = "Invalid status or transition not allowed"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden – ADMIN role required")
    })
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @Parameter(description = "Booking ID", required = true, example = "1")
            @PathVariable Long bookingId,
            @Parameter(description = "New status (PAID, CANCELLED, FAILED)", required = true, example = "PAID")
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal admin) {
        BookingResponse bookingResponse = bookingService.updateBookingStatus(bookingId, status, admin.getId());
        return ResponseEntity.ok(bookingResponse);
    }
}