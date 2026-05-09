package com.geek.booking.service.impl;


import com.geek.booking.dto.request.booking.BookingCancelRequest;
import com.geek.booking.dto.request.booking.BookingCreateRequest;
import com.geek.booking.dto.response.BookingResponse;
import com.geek.booking.entity.Booking;
import com.geek.booking.entity.BookingItem;
import com.geek.booking.entity.Voucher;
import com.geek.booking.enums.BookingStatus;
import com.geek.booking.enums.DiscountType;
import com.geek.booking.exception.BusinessException;
import com.geek.booking.exception.InsufficientInventoryException;
import com.geek.booking.exception.ResourceNotFoundException;
import com.geek.booking.mapper.BookingMapper;
import com.geek.booking.repository.BookingRepository;

import com.geek.booking.service.*;
import com.geek.booking.specification.BookingSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TicketCategoryService ticketCategoryService;
    private final ConcertService concertService;
    private final VoucherService voucherService;
    private final UserService userService;
    private final BookingItemService bookingItemService;
    private final UserVoucherUsageService usageService;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request, Long userId) {
        String idempotencyKey = request.getIdempotencyKey();

        var existing = bookingRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate request with key: {}", idempotencyKey);
            return bookingMapper.toResponse(existing.get());
        }

        var category = ticketCategoryService.getCategoryEntityWithLock(request.getCategoryId());

        if (category.getAvailableQuantity() < request.getQuantity()) {
            throw new InsufficientInventoryException("Not enough tickets for category: " + category.getName());
        }

        BigDecimal subtotal = category.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        BigDecimal totalPrice = subtotal;
        Voucher usedVoucher = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            usedVoucher = voucherService.getVoucherByCodeWithLock(request.getVoucherCode());

            LocalDateTime now = LocalDateTime.now();
            if (usedVoucher.getValidFrom().isAfter(now) || usedVoucher.getValidTo().isBefore(now)) {
                throw new BusinessException("Voucher is not valid");
            }
            if (usedVoucher.getUsedCount() >= usedVoucher.getUsageLimit()) {
                throw new BusinessException("Voucher usage limit reached");
            }
            if (subtotal.compareTo(usedVoucher.getMinOrderValue()) < 0) {
                throw new BusinessException("Minimum order value not met for this voucher");
            }
            if (usageService.hasUserUsedVoucher(userId, usedVoucher.getId())) {
                throw new BusinessException("You have already used this voucher");
            }

            if (usedVoucher.getDiscountType() == DiscountType.PERCENT) {
                BigDecimal discount = subtotal.multiply(usedVoucher.getDiscountValue().divide(BigDecimal.valueOf(100)));
                totalPrice = subtotal.subtract(discount);
            } else {
                totalPrice = subtotal.subtract(usedVoucher.getDiscountValue());
            }
            if (totalPrice.compareTo(BigDecimal.ZERO) < 0) totalPrice = BigDecimal.ZERO;
        }

        ticketCategoryService.reserveTicket(category.getId(), request.getQuantity());

        var user = userService.getEntityById(userId);
        var concert = concertService.getConcertEntityById(category.getConcert().getId());

        Booking booking = Booking.builder()
                .user(user)
                .concert(concert)
                .idempotencyKey(idempotencyKey)
                .status(BookingStatus.PENDING)
                .totalPrice(totalPrice)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        booking = bookingRepository.save(booking);

        bookingItemService.createBookingItem(booking.getId(), category.getId(), request.getQuantity(), category.getPrice());

        if (usedVoucher != null) {
            voucherService.useVoucher(usedVoucher.getId(), userId, booking.getId());
        }

        log.info("Booking created: id={}, userId={}", booking.getId(), userId);
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId, Long userId, BookingCancelRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!booking.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only cancel your own bookings");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Only pending bookings can be cancelled");
        }

        List<BookingItem> items = bookingItemService.getEntitiesByBookingId(bookingId);
        for (BookingItem item : items) {
            ticketCategoryService.releaseTicket(item.getTicketCategory().getId(), item.getQuantity());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking {} cancelled by user {}", bookingId, userId);
    }

    @Override
    public BookingResponse getBookingById(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!booking.getUser().getId().equals(userId) && !userService.isAdmin(userId)) {
            throw new AccessDeniedException("Access denied");
        }
        return bookingMapper.toResponse(booking);
    }

    @Override
    public List<BookingResponse> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<BookingResponse> getAllBookings(String status, Long userId, Long concertId,
                                                LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        Specification<Booking> spec = BookingSpecification.filterBy(status, userId, concertId, fromDate, toDate);
        return bookingRepository.findAll(spec, pageable).map(bookingMapper::toResponse);
    }
    @Override
    @Transactional
    public void updateBookingStatus(Long bookingId, String newStatus, Long adminId) {
        if (!userService.isAdmin(adminId)) {
            throw new AccessDeniedException("Admin rights required");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        BookingStatus target = BookingStatus.valueOf(newStatus.toUpperCase());

        if (booking.getStatus() == BookingStatus.PENDING && target == BookingStatus.PAID) {
            booking.setStatus(BookingStatus.PAID);
        } else if (booking.getStatus() == BookingStatus.PENDING && (target == BookingStatus.CANCELLED || target == BookingStatus.FAILED)) {

            List<BookingItem> items = bookingItemService.getEntitiesByBookingId(bookingId);
            for (BookingItem item : items) {
                ticketCategoryService.releaseTicket(item.getTicketCategory().getId(), item.getQuantity());
            }
            booking.setStatus(target);
        } else if (booking.getStatus() == BookingStatus.PAID && target == BookingStatus.CANCELLED) {

            booking.setStatus(BookingStatus.CANCELLED);
        } else {
            throw new BusinessException("Invalid status transition from " + booking.getStatus() + " to " + target);
        }
        bookingRepository.save(booking);
        log.info("Booking {} status updated to {} by admin {}", bookingId, target, adminId);
    }
}