package com.geek.booking.service;


import com.geek.booking.dto.request.ticketCategory.TicketCategoryCreateRequest;
import com.geek.booking.dto.request.ticketCategory.TicketCategoryUpdateRequest;
import com.geek.booking.dto.response.TicketCategoryResponse;
import com.geek.booking.entity.TicketCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface TicketCategoryService {
    TicketCategoryResponse createTicketCategory(TicketCategoryCreateRequest request, Long concertId);
    TicketCategoryResponse updateTicketCategory(Long id, TicketCategoryUpdateRequest request);
    TicketCategoryResponse getTicketCategoryById(Long id);
    List<TicketCategoryResponse> getTicketCategoriesByConcert(Long concertId);
    void reserveTicket(Long id, int quantity);
    void releaseTicket(Long id, int quantity);
    TicketCategory getCategoryEntityWithLock(Long id);
    Page<TicketCategoryResponse> getAllTicketCategories(String name, Long concertId, BigDecimal minPrice,
                                                        BigDecimal maxPrice, Integer minAvailable, Pageable pageable);
}