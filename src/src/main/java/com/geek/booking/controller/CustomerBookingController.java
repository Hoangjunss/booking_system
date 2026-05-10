package com.geek.booking.controller;

import com.geek.booking.dto.request.booking.BookingCancelRequest;
import com.geek.booking.dto.request.booking.BookingCreateRequest;
import com.geek.booking.dto.response.BookingCreationResult;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Customer Booking", description = "Booking APIs for authenticated customers.")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerBookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create a new booking",
            description = "Supports multiple ticket categories in one booking. Idempotency key is required to prevent duplicate submissions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking created (or duplicate request returns existing)"),
            @ApiResponse(responseCode = "400", description = "Validation error or business rule violation (e.g., voucher invalid)"),
            @ApiResponse(responseCode = "409", description = "Insufficient inventory or data integrity conflict"),
            @ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid token")
    })
    public ResponseEntity<BookingCreationResult> createBooking(
            @Valid @RequestBody BookingCreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.createBooking(request, currentUser.getId()));
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel a pending booking",
            description = "Only bookings with status PENDING can be cancelled. Cancelled bookings release reserved tickets.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking cancelled successfully, returns updated booking"),
            @ApiResponse(responseCode = "400", description = "Booking not in PENDING state or invalid request"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "403", description = "Cannot cancel another user's booking")
    })
    public ResponseEntity<BookingResponse> cancelBooking(
            @Parameter(description = "Booking ID", required = true, example = "1")
            @PathVariable Long bookingId,
            @Valid @RequestBody BookingCancelRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        BookingResponse bookingResponse = bookingService.cancelBooking(bookingId, currentUser.getId(), request);
        return ResponseEntity.ok(bookingResponse);
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking by ID", description = "Returns booking details including all ticket items.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking found"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "403", description = "Access denied (not owner and not admin)")
    })
    public ResponseEntity<BookingResponse> getBooking(
            @Parameter(description = "Booking ID", required = true, example = "1")
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId, currentUser.getId()));
    }

    @GetMapping("/user")
    @Operation(summary = "Get all bookings of the authenticated user",
            description = "Returns list of bookings (basic details, items not loaded).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<BookingResponse>> getUserBookings(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.getUserBookings(currentUser.getId()));
    }
}