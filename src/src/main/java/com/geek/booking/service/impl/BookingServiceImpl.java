package com.geek.booking.service.impl;


import com.geek.booking.dto.request.booking.BookingCancelRequest;
import com.geek.booking.dto.request.booking.BookingCreateRequest;
import com.geek.booking.dto.request.booking.BookingItemRequest;
import com.geek.booking.dto.response.BookingCreationResult;
import com.geek.booking.dto.response.BookingResponse;
import com.geek.booking.entity.*;
import com.geek.booking.enums.BookingStatus;
import com.geek.booking.enums.ConcertStatus;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    @CacheEvict(value = {"publishedConcerts", "concert"}, allEntries = true)
    public BookingCreationResult createBooking(BookingCreateRequest request, Long userId) {
        String idempotencyKey = request.getIdempotencyKey();

        // 1. Idempotency: return existing booking if key already used
        var existing = bookingRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate request with key: {}", idempotencyKey);
            BookingResponse response = bookingMapper.toResponse(existing.get());
            return new BookingCreationResult(response, true);
        }

        // 2. Validate and collect items
        List<BookingItemRequest> itemsRequest = request.getItems();
        if (itemsRequest == null || itemsRequest.isEmpty()) {
            throw new BusinessException("At least one ticket item is required");
        }

        List<TicketCategory> categories = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        Concert concert = null;

        // 3. Lock each ticket category and validate
        for (BookingItemRequest item : itemsRequest) {
            TicketCategory category = ticketCategoryService.getCategoryEntityWithLock(item.getTicketCategoryId());

            // Ensure all items belong to the same concert
            if (concert == null) {
                concert = category.getConcert();
            } else if (!concert.getId().equals(category.getConcert().getId())) {
                throw new BusinessException("All ticket categories must belong to the same concert");
            }

            // Only published concerts are bookable
            if (category.getConcert().getStatus() != ConcertStatus.PUBLISHED) {
                throw new BusinessException("Concert is not available for booking. Current status: " + category.getConcert().getStatus());
            }

            // Check inventory
            if (category.getAvailableQuantity() < item.getQuantity()) {
                throw new InsufficientInventoryException("Not enough tickets for category: " + category.getName());
            }

            categories.add(category);
            quantities.add(item.getQuantity());
            subtotal = subtotal.add(category.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 4. Apply voucher (if any) to total order value
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
            if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
                totalPrice = BigDecimal.ZERO;
            }
        }

        // 5. Decrease inventory (each category uses its own row lock already acquired)
        for (int i = 0; i < categories.size(); i++) {
            ticketCategoryService.reserveTicket(categories.get(i).getId(), quantities.get(i));
        }

        // 6. Create booking entity
        var user = userService.getEntityById(userId);
        Booking booking = Booking.builder()
                .user(user)
                .concert(concert)
                .idempotencyKey(idempotencyKey)
                .status(BookingStatus.PENDING)
                .totalPrice(totalPrice)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        booking = bookingRepository.save(booking);

        // 7. Create booking items and add to booking's item list (so they are included in response)
        if (booking.getItems() == null) {
            booking.setItems(new ArrayList<>());
        }
        for (int i = 0; i < categories.size(); i++) {
            TicketCategory cat = categories.get(i);
            int qty = quantities.get(i);
            BookingItem item = bookingItemService.createBookingItem(booking.getId(), cat.getId(), qty, cat.getPrice());
            booking.getItems().add(item);
        }

        // 8. Record voucher usage if applicable
        if (usedVoucher != null) {
            voucherService.useVoucher(usedVoucher.getId(), userId, booking.getId());
        }

        log.info("Booking created: id={}, userId={}, totalItems={}", booking.getId(), userId, booking.getItems().size());
        return new BookingCreationResult(bookingMapper.toResponse(booking), false);
    }
    @Override
    @Transactional
    @CacheEvict(value = {"publishedConcerts", "concert"}, allEntries = true)
    public BookingResponse cancelBooking(Long bookingId, Long userId, BookingCancelRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!booking.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only cancel your own bookings");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Only pending bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        log.info("Booking {} cancelled by user {}", bookingId, userId);
        return bookingMapper.toResponse(bookingRepository.save(booking));

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
    @CacheEvict(value = {"publishedConcerts", "concert"}, allEntries = true)
    public BookingResponse  updateBookingStatus(Long bookingId, String newStatus, Long adminId) {
        if (!userService.isAdmin(adminId)) {
            throw new AccessDeniedException("Admin rights required");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        BookingStatus target = BookingStatus.valueOf(newStatus.toUpperCase());

        if (booking.getStatus() == BookingStatus.PENDING && target == BookingStatus.PAID) {
            booking.setStatus(BookingStatus.PAID);
        } else if (booking.getStatus() == BookingStatus.PENDING && (target == BookingStatus.CANCELLED || target == BookingStatus.FAILED)) {
            booking.setStatus(target);
        } else if (booking.getStatus() == BookingStatus.PAID && target == BookingStatus.CANCELLED) {
            booking.setStatus(BookingStatus.CANCELLED);
        } else {
            throw new BusinessException("Invalid status transition from " + booking.getStatus() + " to " + target);
        }
        log.info("Booking {} status updated to {} by admin {}", bookingId, target, adminId);
        return bookingMapper.toResponse(bookingRepository.save(booking));

    }
}