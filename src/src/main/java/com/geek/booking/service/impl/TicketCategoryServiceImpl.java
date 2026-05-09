package com.geek.booking.service.impl;


import com.geek.booking.dto.request.ticketCategory.TicketCategoryCreateRequest;
import com.geek.booking.dto.request.ticketCategory.TicketCategoryUpdateRequest;
import com.geek.booking.dto.response.TicketCategoryResponse;
import com.geek.booking.entity.Concert;
import com.geek.booking.entity.TicketCategory;
import com.geek.booking.exception.BusinessException;
import com.geek.booking.exception.ResourceNotFoundException;
import com.geek.booking.mapper.TicketCategoryMapper;
import com.geek.booking.repository.TicketCategoryRepository;

import com.geek.booking.service.ConcertService;
import com.geek.booking.service.TicketCategoryService;
import com.geek.booking.specification.TicketCategorySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketCategoryServiceImpl implements TicketCategoryService {

    private final TicketCategoryRepository repository;
    private final ConcertService concertService;
    private final TicketCategoryMapper mapper;

    @Override
    @Transactional
    public TicketCategoryResponse createTicketCategory(TicketCategoryCreateRequest request, Long concertId) {
        Concert concert = concertService.getConcertEntityById(concertId);
        TicketCategory category = mapper.toEntity(request);
        category.setConcert(concert);
        category.setAvailableQuantity(request.getTotalQuantity());
        category = repository.save(category);
        log.info("Ticket category created: id={}, name={}", category.getId(), category.getName());
        return mapper.toResponse(category);
    }

    @Override
    @Transactional
    public TicketCategoryResponse updateTicketCategory(Long categoryId, TicketCategoryUpdateRequest request) {
        TicketCategory category = repository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (request.getName() != null) category.setName(request.getName());
        if (request.getPrice() != null) category.setPrice(request.getPrice());
        if (request.getTotalQuantity() != null) {
            int delta = request.getTotalQuantity() - category.getTotalQuantity();
            category.setTotalQuantity(request.getTotalQuantity());
            category.setAvailableQuantity(category.getAvailableQuantity() + delta);
        }
        return mapper.toResponse(repository.save(category));
    }

    @Override
    public TicketCategoryResponse getTicketCategoryById(Long id) {
        return mapper.toResponse(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found")));
    }


    @Override
    public List<TicketCategoryResponse> getTicketCategoriesByConcert(Long concertId) {
        return repository.findByConcertId(concertId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void reserveTicket(Long categoryId, int quantity) {
        TicketCategory category = getCategoryEntityWithLock(categoryId);
        if (category.getAvailableQuantity() < quantity) {
            throw new BusinessException("Not enough tickets available");
        }
        category.setAvailableQuantity(category.getAvailableQuantity() - quantity);
        repository.save(category);
    }

    @Override
    @Transactional
    public void releaseTicket(Long categoryId, int quantity) {
        TicketCategory category = repository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setAvailableQuantity(category.getAvailableQuantity() + quantity);
        repository.save(category);
    }

    @Override
    public TicketCategory getCategoryEntityWithLock(Long categoryId) {
        return repository.findByIdWithPessimisticLock(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
    @Override
    public Page<TicketCategoryResponse> getAllTicketCategories(String name, Long concertId,
                                                               BigDecimal minPrice, BigDecimal maxPrice, Integer minAvailable, Pageable pageable) {
        return repository.findAll(TicketCategorySpecification.filterBy(name, concertId,
                        minPrice, maxPrice, minAvailable), pageable)
                .map(mapper::toResponse);
    }
}