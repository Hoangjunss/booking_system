package com.geek.booking.service;

import com.geek.booking.dto.request.concern.ConcertCreateRequest;
import com.geek.booking.dto.request.concern.ConcertUpdateRequest;
import com.geek.booking.dto.response.ConcertResponse;
import com.geek.booking.entity.Concert;
import com.geek.booking.enums.ConcertStatus;
import com.geek.booking.exception.ResourceNotFoundException;
import com.geek.booking.mapper.ConcertMapper;
import com.geek.booking.repository.ConcertRepository;
import com.geek.booking.service.impl.ConcertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConcertServiceTest {

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private ConcertMapper concertMapper;

    @InjectMocks
    private ConcertServiceImpl concertService;

    private Concert concert;
    private ConcertResponse concertResponse;
    private ConcertCreateRequest createRequest;
    private ConcertUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        concert = Concert.builder()
                .id(1L)
                .name("Rock Fest")
                .description("Big concert")
                .venue("Stadium")
                .eventDate(LocalDateTime.now().plusDays(10))
                .status(ConcertStatus.DRAFT)
                .build();

        concertResponse = ConcertResponse.builder()
                .id(1L)
                .name("Rock Fest")
                .venue("Stadium")
                .status(ConcertStatus.DRAFT)
                .build();

        createRequest = new ConcertCreateRequest();
        createRequest.setName("New Concert");
        createRequest.setVenue("Arena");
        createRequest.setEventDate(LocalDateTime.now().plusDays(5));
        createRequest.setTicketCategories(List.of());

        updateRequest = new ConcertUpdateRequest();
        updateRequest.setName("Updated Rock Fest");
        updateRequest.setStatus("PUBLISHED");
    }

    @Test
    void createConcert_Success() {
        when(concertMapper.toEntity(createRequest)).thenReturn(concert);
        when(concertRepository.save(any(Concert.class))).thenReturn(concert);
        when(concertMapper.toResponse(concert)).thenReturn(concertResponse);

        ConcertResponse result = concertService.createConcert(createRequest);
        assertThat(result.getName()).isEqualTo("Rock Fest");
    }

    @Test
    void updateConcert_Success() {
        when(concertRepository.findById(1L)).thenReturn(Optional.of(concert));
        when(concertRepository.save(any(Concert.class))).thenReturn(concert);
        when(concertMapper.toResponse(concert)).thenReturn(concertResponse);

        ConcertResponse result = concertService.updateConcert(1L, updateRequest);
        assertThat(result.getName()).isEqualTo("Rock Fest");
        assertThat(concert.getName()).isEqualTo("Updated Rock Fest");
        assertThat(concert.getStatus()).isEqualTo(ConcertStatus.PUBLISHED);
    }

    @Test
    void updateConcert_NotFound_ThrowsException() {
        when(concertRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> concertService.updateConcert(99L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getConcertById_Success() {
        when(concertRepository.findById(1L)).thenReturn(Optional.of(concert));
        when(concertMapper.toResponse(concert)).thenReturn(concertResponse);
        ConcertResponse result = concertService.getConcertById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getConcertById_NotFound_ThrowsException() {
        when(concertRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> concertService.getConcertById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void publishConcert_Success() {
        when(concertRepository.findById(1L)).thenReturn(Optional.of(concert));
        concertService.publishConcert(1L);
        assertThat(concert.getStatus()).isEqualTo(ConcertStatus.PUBLISHED);
    }

    @Test
    void unpublishConcert_Success() {
        concert.setStatus(ConcertStatus.PUBLISHED);
        when(concertRepository.findById(1L)).thenReturn(Optional.of(concert));
        concertService.unpublishConcert(1L);
        assertThat(concert.getStatus()).isEqualTo(ConcertStatus.DRAFT);
    }

    @Test
    void getAllConcerts_WithFilters_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Concert> concertPage = new PageImpl<>(List.of(concert));
        when(concertRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(concertPage);
        when(concertMapper.toResponse(concert)).thenReturn(concertResponse);

        Page<ConcertResponse> result = concertService.getAllConcerts("rock", "Stadium", "DRAFT", null, null, pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getPublishedConcerts_ReturnsList() {
        when(concertRepository.findByStatus(ConcertStatus.PUBLISHED)).thenReturn(List.of(concert));
        when(concertMapper.toResponseList(anyList())).thenReturn(List.of(concertResponse));
        List<ConcertResponse> result = concertService.getPublishedConcerts();
        assertThat(result).hasSize(1);
    }

    @Test
    void getConcertEntityById_Success() {
        when(concertRepository.findById(1L)).thenReturn(Optional.of(concert));
        Concert result = concertService.getConcertEntityById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }
}