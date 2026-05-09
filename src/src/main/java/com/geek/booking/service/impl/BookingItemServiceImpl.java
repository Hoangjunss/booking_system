package com.geek.booking.service.impl;

import com.geek.booking.dto.response.BookingItemResponse;
import com.geek.booking.entity.Booking;
import com.geek.booking.entity.BookingItem;
import com.geek.booking.entity.TicketCategory;
import com.geek.booking.exception.ResourceNotFoundException;
import com.geek.booking.repository.BookingItemRepository;
import com.geek.booking.repository.BookingRepository;
import com.geek.booking.repository.TicketCategoryRepository;
import com.geek.booking.service.BookingItemService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingItemServiceImpl implements BookingItemService {

    private final BookingItemRepository repository;
    private final BookingRepository bookingRepository;
    private final TicketCategoryRepository categoryRepository;

    @Override
    @Transactional
    public BookingItem createBookingItem(Long bookingId, Long categoryId, int quantity, BigDecimal unitPrice) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        TicketCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        BookingItem item = BookingItem.builder()
                .booking(booking)
                .ticketCategory(category)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .build();
        return repository.save(item);
    }

    @Override
    public List<BookingItemResponse> getItemsByBookingId(Long bookingId) {
        return repository.findByBookingId(bookingId).stream()
                .map(item -> BookingItemResponse.builder()
                        .id(item.getId())
                        .categoryName(item.getTicketCategory().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingItem> getEntitiesByBookingId(Long bookingId) {
        return repository.findByBookingId(bookingId);
    }

    @Override
    public Page<BookingItemResponse> getAllBookingItems(Long bookingId, Long categoryId,
                                                        BigDecimal minUnitPrice, BigDecimal maxUnitPrice,
                                                        Pageable pageable) {
        Specification<BookingItem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (bookingId != null) {
                predicates.add(cb.equal(root.get("booking").get("id"), bookingId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("ticketCategory").get("id"), categoryId));
            }
            if (minUnitPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("unitPrice"), minUnitPrice));
            }
            if (maxUnitPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("unitPrice"), maxUnitPrice));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable).map(item -> BookingItemResponse.builder()
                .id(item.getId())
                .categoryName(item.getTicketCategory().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build());
    }
}