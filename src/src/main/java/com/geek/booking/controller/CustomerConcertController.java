package com.geek.booking.controller;

import com.geek.booking.dto.response.ConcertResponse;
import com.geek.booking.dto.response.TicketCategoryResponse;
import com.geek.booking.service.ConcertService;
import com.geek.booking.service.TicketCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
@Tag(name = "Customer Concert", description = "Public concert APIs")
public class CustomerConcertController {

    private final ConcertService concertService;
    private final TicketCategoryService ticketCategoryService;

    @GetMapping
    @Operation(summary = "Get all published concerts")
    public ResponseEntity<List<ConcertResponse>> getPublishedConcerts() {
        return ResponseEntity.ok(concertService.getPublishedConcerts());
    }

    @GetMapping("/{concertId}")
    @Operation(summary = "Get concert details")
    public ResponseEntity<ConcertResponse> getConcert(@PathVariable Long concertId) {
        return ResponseEntity.ok(concertService.getConcertById(concertId));
    }

    @GetMapping("/{concertId}/ticket-categories")
    @Operation(summary = "Get ticket categories of a concert")
    public ResponseEntity<List<TicketCategoryResponse>> getTicketCategories(@PathVariable Long concertId) {
        return ResponseEntity.ok(ticketCategoryService.getTicketCategoriesByConcert(concertId));
    }
}