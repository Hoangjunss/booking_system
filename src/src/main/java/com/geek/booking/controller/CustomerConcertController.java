package com.geek.booking.controller;

import com.geek.booking.dto.response.ConcertResponse;
import com.geek.booking.dto.response.TicketCategoryResponse;
import com.geek.booking.service.ConcertService;
import com.geek.booking.service.TicketCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
@Tag(name = "Customer Concert", description = "Public endpoints for viewing concerts and ticket categories (no authentication required).")
public class CustomerConcertController {

    private final ConcertService concertService;
    private final TicketCategoryService ticketCategoryService;

    @GetMapping
    @Operation(summary = "Get all published concerts", description = "Returns concerts with status PUBLISHED only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ConcertResponse>> getPublishedConcerts() {
        return ResponseEntity.ok(concertService.getPublishedConcerts());
    }

    @GetMapping("/{concertId}")
    @Operation(summary = "Get concert details", description = "Returns concert information including ticket categories (if published).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Concert found"),
            @ApiResponse(responseCode = "404", description = "Concert not found or not published")
    })
    public ResponseEntity<ConcertResponse> getConcert(
            @Parameter(description = "Concert ID", required = true, example = "1")
            @PathVariable Long concertId) {
        return ResponseEntity.ok(concertService.getConcertById(concertId));
    }

    @GetMapping("/{concertId}/ticket-categories")
    @Operation(summary = "Get ticket categories of a concert", description = "Returns list of ticket categories with price and availability.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list"),
            @ApiResponse(responseCode = "404", description = "Concert not found")
    })
    public ResponseEntity<List<TicketCategoryResponse>> getTicketCategories(
            @Parameter(description = "Concert ID", required = true, example = "1")
            @PathVariable Long concertId) {
        return ResponseEntity.ok(ticketCategoryService.getTicketCategoriesByConcert(concertId));
    }
}