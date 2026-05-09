package com.geek.booking.service.impl;

import com.geek.booking.dto.request.concern.ConcertCreateRequest;
import com.geek.booking.dto.request.concern.ConcertUpdateRequest;
import com.geek.booking.dto.response.ConcertResponse;
import com.geek.booking.entity.Concert;
import com.geek.booking.entity.TicketCategory;
import com.geek.booking.enums.ConcertStatus;
import com.geek.booking.exception.ResourceNotFoundException;
import com.geek.booking.mapper.ConcertMapper;
import com.geek.booking.mapper.TicketCategoryMapper;
import com.geek.booking.repository.ConcertRepository;
import com.geek.booking.service.ConcertService;
import com.geek.booking.specification.ConcertSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConcertServiceImpl implements ConcertService {

    private final ConcertRepository concertRepository;
    private final ConcertMapper concertMapper;
    private final TicketCategoryMapper ticketCategoryMapper;

    @Override
    @Transactional
    @CacheEvict(value = {"publishedConcerts", "concert"}, allEntries = true)
    public ConcertResponse createConcert(ConcertCreateRequest request) {
        Concert concert = concertMapper.toEntity(request);
        List<TicketCategory> categories = request.getTicketCategories().stream()
                .map(catReq -> {
                    TicketCategory cat = ticketCategoryMapper.toEntity(catReq);
                    cat.setConcert(concert);
                    cat.setAvailableQuantity(catReq.getTotalQuantity());
                    return cat;
                })
                .collect(Collectors.toList());
        concert.setTicketCategories(categories);
        Concert savedConcert = concertRepository.save(concert);
        log.info("Concert created: id={}, name={}", savedConcert.getId(), savedConcert.getName());
        return concertMapper.toResponse(savedConcert);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"publishedConcerts", "concert"}, allEntries = true)
    public ConcertResponse updateConcert(Long concertId, ConcertUpdateRequest request) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));
        if (request.getName() != null) concert.setName(request.getName());
        if (request.getDescription() != null) concert.setDescription(request.getDescription());
        if (request.getVenue() != null) concert.setVenue(request.getVenue());
        if (request.getEventDate() != null) concert.setEventDate(request.getEventDate());
        if (request.getStatus() != null) {
            concert.setStatus(ConcertStatus.valueOf(request.getStatus().toUpperCase()));
        }
        concert = concertRepository.save(concert);
        log.info("Concert updated: id={}", concertId);
        return concertMapper.toResponse(concert);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"publishedConcerts", "concert"}, allEntries = true)
    public void publishConcert(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));
        concert.setStatus(ConcertStatus.PUBLISHED);
        concertRepository.save(concert);
        log.info("Concert {} published", concertId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"publishedConcerts", "concert"}, allEntries = true)
    public void unpublishConcert(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));
        concert.setStatus(ConcertStatus.DRAFT);
        concertRepository.save(concert);
        log.info("Concert {} unpublished", concertId);
    }

    @Override
    @Cacheable(value = "concert", key = "#concertId", unless = "#result == null")
    public ConcertResponse getConcertById(Long concertId) {
        log.info("Fetching concert {} from DATABASE (cache miss)", concertId);
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));
        return concertMapper.toResponse(concert);
    }

    @Override
    public Page<ConcertResponse> getAllConcerts(String name, String venue, String status,
                                                LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        ConcertStatus concertStatus = status != null && !status.isBlank() ? ConcertStatus.valueOf(status.toUpperCase()) : null;
        return concertRepository.findAll(ConcertSpecification.filterBy(name, venue, concertStatus, fromDate, toDate), pageable)
                .map(concertMapper::toResponse);
    }

    @Override
    @Cacheable(value = "publishedConcerts", unless = "#result == null")
    public List<ConcertResponse> getPublishedConcerts() {
        log.info("Fetching all published concerts from DATABASE (cache miss)");
        return concertMapper.toResponseList(concertRepository.findByStatus(ConcertStatus.PUBLISHED));
    }

    @Override
    public Concert getConcertEntityById(Long concertId) {
        return concertRepository.findById(concertId)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));
    }
}