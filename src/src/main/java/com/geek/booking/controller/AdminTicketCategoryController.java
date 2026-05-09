package com.geek.booking.controller;

import com.geek.booking.dto.request.ticketCategory.TicketCategoryCreateRequest;
import com.geek.booking.dto.request.ticketCategory.TicketCategoryUpdateRequest;
import com.geek.booking.dto.response.TicketCategoryResponse;
import com.geek.booking.service.TicketCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin/ticket-categories")
@RequiredArgsConstructor
@Tag(name = "Admin Ticket Category", description = "Admin APIs for ticket category management")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTicketCategoryController {

    private final TicketCategoryService ticketCategoryService;

    @PostMapping
    @Operation(summary = "Create ticket category for a concert")
    public ResponseEntity<TicketCategoryResponse> createTicketCategory(
            @Valid @RequestBody TicketCategoryCreateRequest request,
            @RequestParam Long concertId) {
        return ResponseEntity.ok(ticketCategoryService.createTicketCategory(request, concertId));
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update ticket category")
    public ResponseEntity<TicketCategoryResponse> updateTicketCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody TicketCategoryUpdateRequest request) {
        return ResponseEntity.ok(ticketCategoryService.updateTicketCategory(categoryId, request));
    }

    @GetMapping("/concert/{concertId}")
    @Operation(summary = "Get ticket categories by concert")
    public ResponseEntity<Page<TicketCategoryResponse>> getCategoriesByConcert(
            @PathVariable Long concertId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minAvailable,
            Pageable pageable) {
        return ResponseEntity.ok(ticketCategoryService.getAllTicketCategories(name, concertId, minPrice, maxPrice, minAvailable, pageable));
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get ticket category by ID")
    public ResponseEntity<TicketCategoryResponse> getTicketCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(ticketCategoryService.getTicketCategoryById(categoryId));
    }
}