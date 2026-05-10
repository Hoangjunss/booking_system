package com.geek.booking.controller;

import com.geek.booking.dto.request.concern.ConcertCreateRequest;
import com.geek.booking.dto.request.concern.ConcertUpdateRequest;
import com.geek.booking.dto.response.ConcertResponse;
import com.geek.booking.service.ConcertService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/concerts")
@RequiredArgsConstructor
@Tag(name = "Admin Concert", description = "APIs for administrators to manage concerts (create, update, publish, unpublish, delete, list).")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminConcertController {

    private final ConcertService concertService;

    @PostMapping
    @Operation(summary = "Create a new concert",
            description = "Creates a concert in DRAFT status. Optionally add ticket categories in the same request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Concert created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g., missing name, negative quantity)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden – ADMIN role required")
    })
    public ResponseEntity<ConcertResponse> createConcert(@Valid @RequestBody ConcertCreateRequest request) {
        return ResponseEntity.ok(concertService.createConcert(request));
    }

    @PutMapping("/{concertId}")
    @Operation(summary = "Update an existing concert",
            description = "Partial update allowed: only provided fields will be changed. Status can be changed to DRAFT/PUBLISHED/ENDED/CANCELLED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Concert updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid field value (e.g., future date, unknown status)"),
            @ApiResponse(responseCode = "404", description = "Concert not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ConcertResponse> updateConcert(
            @Parameter(description = "Concert ID", required = true, example = "1")
            @PathVariable Long concertId,
            @Valid @RequestBody ConcertUpdateRequest request) {
        return ResponseEntity.ok(concertService.updateConcert(concertId, request));
    }

    @PostMapping("/{concertId}/publish")
    @Operation(summary = "Publish a concert",
            description = "Changes concert status to PUBLISHED, making it visible to customers. Returns 204 No Content.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Published successfully"),
            @ApiResponse(responseCode = "404", description = "Concert not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> publishConcert(
            @Parameter(description = "Concert ID", required = true, example = "1")
            @PathVariable Long concertId) {
        concertService.publishConcert(concertId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{concertId}/unpublish")
    @Operation(summary = "Unpublish a concert",
            description = "Changes concert status to DRAFT, hiding it from customers. Returns 204 No Content.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Unpublished successfully"),
            @ApiResponse(responseCode = "404", description = "Concert not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> unpublishConcert(
            @Parameter(description = "Concert ID", required = true, example = "1")
            @PathVariable Long concertId) {
        concertService.unpublishConcert(concertId);
        return ResponseEntity.noContent().build();
    }



    @GetMapping
    @Operation(summary = "Get all concerts (paginated and filtered)",
            description = "Retrieve a page of concerts. Available for ADMIN only (for operational dashboard).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved page"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<ConcertResponse>> getAllConcerts(
            @Parameter(description = "Filter by concert name (partial match, case-insensitive)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filter by venue (partial match, case-insensitive)")
            @RequestParam(required = false) String venue,
            @Parameter(description = "Filter by status (DRAFT, PUBLISHED, ENDED, CANCELLED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by event date start (ISO 8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @Parameter(description = "Filter by event date end (ISO 8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "eventDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(concertService.getAllConcerts(name, venue, status, fromDate, toDate, pageable));
    }

    @GetMapping("/{concertId}")
    @Operation(summary = "Get concert by ID (admin view)", description = "Returns full concert details including ticket categories.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Concert found"),
            @ApiResponse(responseCode = "404", description = "Concert not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ConcertResponse> getConcert(
            @Parameter(description = "Concert ID", required = true, example = "1")
            @PathVariable Long concertId) {
        return ResponseEntity.ok(concertService.getConcertById(concertId));
    }
}