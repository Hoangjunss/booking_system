package com.geek.booking.controller;

import com.geek.booking.dto.request.voucher.VoucherCreateRequest;
import com.geek.booking.dto.response.VoucherResponse;
import com.geek.booking.service.VoucherService;
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
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
@Tag(name = "Admin Voucher", description = "Admin APIs for voucher management")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController {

    private final VoucherService voucherService;

    @PostMapping
    @Operation(summary = "Create new voucher")
    public ResponseEntity<VoucherResponse> createVoucher(@Valid @RequestBody VoucherCreateRequest request) {
        return ResponseEntity.ok(voucherService.createVoucher(request));
    }

    @GetMapping
    @Operation(summary = "Get all vouchers with filters")
    public ResponseEntity<Page<VoucherResponse>> getAllVouchers(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String discountType,
            @RequestParam(required = false) Boolean isValid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validTo,
            Pageable pageable) {
        return ResponseEntity.ok(voucherService.getAllVouchers(code, discountType, isValid, validFrom, validTo, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get voucher by ID")
    public ResponseEntity<VoucherResponse> getVoucherById(@PathVariable Long id) {
        return ResponseEntity.ok(voucherService.getVoucherById(id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get voucher by code")
    public ResponseEntity<VoucherResponse> getVoucherByCode(@PathVariable String code) {
        return ResponseEntity.ok(voucherService.getVoucherByCode(code));
    }

}