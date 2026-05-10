package com.geek.booking.controller;

import com.geek.booking.dto.request.ticketCategory.TicketCategoryCreateRequest;
import com.geek.booking.dto.request.ticketCategory.TicketCategoryUpdateRequest;
import com.geek.booking.dto.response.TicketCategoryResponse;
import com.geek.booking.service.TicketCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin/ticket-categories")
@RequiredArgsConstructor
@Tag(name = "Admin Ticket Category", description = "APIs for administrators to manage ticket categories (prices, quantities, etc.)")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTicketCategoryController {

    private final TicketCategoryService ticketCategoryService;

    @PostMapping
    @Operation(summary = "Create a new ticket category for a concert",
            description = "Requires concertId as query parameter. Initially availableQuantity = totalQuantity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g., price negative, total quantity < 1)"),
            @ApiResponse(responseCode = "404", description = "Concert not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TicketCategoryResponse> createTicketCategory(
            @Valid @RequestBody TicketCategoryCreateRequest request,
            @Parameter(description = "ID of the concert to attach this category to", required = true, example = "1")
            @RequestParam Long concertId) {
        return ResponseEntity.ok(ticketCategoryService.createTicketCategory(request, concertId));
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update a ticket category",
            description = "Partial update allowed. When totalQuantity changes, availableQuantity adjusts accordingly.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data (e.g., new price negative)"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TicketCategoryResponse> updateTicketCategory(
            @Parameter(description = "Ticket category ID", required = true, example = "1")
            @PathVariable Long categoryId,
            @Valid @RequestBody TicketCategoryUpdateRequest request) {
        return ResponseEntity.ok(ticketCategoryService.updateTicketCategory(categoryId, request));
    }

    @GetMapping("/concert/{concertId}")
    @Operation(summary = "Get paginated ticket categories of a concert with filters",
            description = "Supports filtering by name, price range, and minimum available quantity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of ticket categories returned"),
            @ApiResponse(responseCode = "404", description = "Concert not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<TicketCategoryResponse>> getCategoriesByConcert(
            @Parameter(description = "Concert ID", required = true, example = "1")
            @PathVariable Long concertId,
            @Parameter(description = "Filter by category name (partial match)", example = "VIP")
            @RequestParam(required = false) String name,
            @Parameter(description = "Minimum price", example = "50.00")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price", example = "200.00")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Minimum available quantity", example = "10")
            @RequestParam(required = false) Integer minAvailable,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "price", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ticketCategoryService.getAllTicketCategories(name, concertId, minPrice, maxPrice, minAvailable, pageable));
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get ticket category by ID", description = "Returns details of a single ticket category.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TicketCategoryResponse> getTicketCategory(
            @Parameter(description = "Ticket category ID", required = true, example = "1")
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(ticketCategoryService.getTicketCategoryById(categoryId));
    }
}