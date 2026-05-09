package com.geek.booking.controller;

import com.geek.booking.dto.request.concern.ConcertCreateRequest;
import com.geek.booking.dto.request.concern.ConcertUpdateRequest;
import com.geek.booking.dto.response.ConcertResponse;
import com.geek.booking.service.ConcertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/concerts")
@RequiredArgsConstructor
@Tag(name = "Admin Concert", description = "Admin APIs for concert management")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminConcertController {

    private final ConcertService concertService;

    @PostMapping
    @Operation(summary = "Create new concert")
    public ResponseEntity<ConcertResponse> createConcert(@Valid @RequestBody ConcertCreateRequest request) {
        return ResponseEntity.ok(concertService.createConcert(request));
    }

    @PutMapping("/{concertId}")
    @Operation(summary = "Update concert")
    public ResponseEntity<ConcertResponse> updateConcert(
            @PathVariable Long concertId,
            @Valid @RequestBody ConcertUpdateRequest request) {
        return ResponseEntity.ok(concertService.updateConcert(concertId, request));
    }

    @PostMapping("/{concertId}/publish")
    @Operation(summary = "Publish concert")
    public ResponseEntity<Void> publishConcert(@PathVariable Long concertId) {
        concertService.publishConcert(concertId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{concertId}/unpublish")
    @Operation(summary = "Unpublish concert")
    public ResponseEntity<Void> unpublishConcert(@PathVariable Long concertId) {
        concertService.unpublishConcert(concertId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get all concerts with filters")
    public ResponseEntity<Page<ConcertResponse>> getAllConcerts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            Pageable pageable) {
        return ResponseEntity.ok(concertService.getAllConcerts(name, venue, status, fromDate, toDate, pageable));
    }

    @GetMapping("/{concertId}")
    @Operation(summary = "Get concert by ID")
    public ResponseEntity<ConcertResponse> getConcert(@PathVariable Long concertId) {
        return ResponseEntity.ok(concertService.getConcertById(concertId));
    }
}