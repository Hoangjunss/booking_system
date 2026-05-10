package com.geek.booking.controller;

import com.geek.booking.dto.request.voucher.VoucherCreateRequest;
import com.geek.booking.dto.response.VoucherResponse;
import com.geek.booking.service.VoucherService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
@Tag(name = "Admin Voucher", description = "APIs for administrators to manage promotional vouchers (create, list, view).")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController {

    private final VoucherService voucherService;

    @PostMapping
    @Operation(summary = "Create a new voucher", description = "Vouchers are immutable after creation (no update/delete).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Voucher created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g., duplicate code, invalid date range)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden – ADMIN role required")
    })
    public ResponseEntity<VoucherResponse> createVoucher(@Valid @RequestBody VoucherCreateRequest request) {
        return ResponseEntity.ok(voucherService.createVoucher(request));
    }

    @GetMapping
    @Operation(summary = "Get all vouchers (paginated and filtered)",
            description = "Filters: code (partial match), discountType, validity period, and whether valid now.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved page of vouchers"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<VoucherResponse>> getAllVouchers(
            @Parameter(description = "Filter by voucher code (partial match, case-insensitive)")
            @RequestParam(required = false) String code,
            @Parameter(description = "Filter by discount type (PERCENT, FIXED)")
            @RequestParam(required = false) String discountType,
            @Parameter(description = "Filter vouchers that are currently valid (not expired, not used up)")
            @RequestParam(required = false) Boolean isValid,
            @Parameter(description = "Filter by valid from date (start of validity)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validFrom,
            @Parameter(description = "Filter by valid to date (end of validity)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validTo,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(voucherService.getAllVouchers(code, discountType, isValid, validFrom, validTo, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get voucher by ID", description = "Returns voucher details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Voucher found"),
            @ApiResponse(responseCode = "404", description = "Voucher not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<VoucherResponse> getVoucherById(
            @Parameter(description = "Voucher ID", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(voucherService.getVoucherById(id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get voucher by code", description = "Returns voucher details using its unique code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Voucher found"),
            @ApiResponse(responseCode = "404", description = "Voucher not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<VoucherResponse> getVoucherByCode(
            @Parameter(description = "Voucher code", required = true, example = "FLASH10")
            @PathVariable String code) {
        return ResponseEntity.ok(voucherService.getVoucherByCode(code));
    }
}