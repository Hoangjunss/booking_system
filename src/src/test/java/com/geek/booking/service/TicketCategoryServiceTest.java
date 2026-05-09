package com.geek.booking.service;

import com.geek.booking.dto.request.ticketCategory.TicketCategoryCreateRequest;
import com.geek.booking.dto.request.ticketCategory.TicketCategoryUpdateRequest;
import com.geek.booking.dto.response.TicketCategoryResponse;
import com.geek.booking.entity.Concert;
import com.geek.booking.entity.TicketCategory;
import com.geek.booking.exception.BusinessException;
import com.geek.booking.mapper.TicketCategoryMapper;
import com.geek.booking.repository.TicketCategoryRepository;
import com.geek.booking.service.impl.TicketCategoryServiceImpl;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketCategoryServiceTest {

    @Mock
    private TicketCategoryRepository repository;

    @Mock
    private ConcertService concertService;

    @Mock
    private TicketCategoryMapper mapper;

    @InjectMocks
    private TicketCategoryServiceImpl service;

    private Concert concert;
    private TicketCategory category;
    private TicketCategoryResponse response;
    private TicketCategoryCreateRequest createRequest;
    private TicketCategoryUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        concert = Concert.builder().id(1L).name("Test Concert").build();
        category = TicketCategory.builder()
                .id(1L)
                .concert(concert)
                .name("VIP")
                .price(BigDecimal.valueOf(100))
                .totalQuantity(50)
                .availableQuantity(50)
                .build();
        response = TicketCategoryResponse.builder()
                .id(1L)
                .name("VIP")
                .price(BigDecimal.valueOf(100))
                .totalQuantity(50)
                .availableQuantity(50)
                .build();
        createRequest = new TicketCategoryCreateRequest();
        createRequest.setName("Early Bird");
        createRequest.setPrice(BigDecimal.valueOf(80));
        createRequest.setTotalQuantity(30);
        updateRequest = new TicketCategoryUpdateRequest();
        updateRequest.setPrice(BigDecimal.valueOf(90));
        updateRequest.setTotalQuantity(40);
    }

    @Test
    void createTicketCategory_Success() {
        when(concertService.getConcertEntityById(1L)).thenReturn(concert);
        when(mapper.toEntity(createRequest)).thenReturn(category);
        when(repository.save(any())).thenReturn(category);
        when(mapper.toResponse(category)).thenReturn(response);
        TicketCategoryResponse result = service.createTicketCategory(createRequest, 1L);
        assertThat(result.getName()).isEqualTo("VIP");
    }

    @Test
    void updateTicketCategory_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(category));
        when(repository.save(any())).thenReturn(category);
        when(mapper.toResponse(category)).thenReturn(response);
        TicketCategoryResponse result = service.updateTicketCategory(1L, updateRequest);
        assertThat(category.getPrice()).isEqualTo(BigDecimal.valueOf(90));
        assertThat(category.getTotalQuantity()).isEqualTo(40);
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void getTicketCategoryById_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(category));
        when(mapper.toResponse(category)).thenReturn(response);
        TicketCategoryResponse result = service.getTicketCategoryById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }


    @Test
    void reserveTicket_Success() {
        when(repository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(category));
        service.reserveTicket(1L, 10);
        assertThat(category.getAvailableQuantity()).isEqualTo(40);
        verify(repository).save(category);
    }

    @Test
    void reserveTicket_InsufficientQuantity_ThrowsException() {
        when(repository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(category));
        assertThatThrownBy(() -> service.reserveTicket(1L, 100))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getAllTicketCategories_WithFilters_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TicketCategory> page = new PageImpl<>(List.of(category));
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(category)).thenReturn(response);
        Page<TicketCategoryResponse> result = service.getAllTicketCategories("VIP", 1L, BigDecimal.valueOf(50), BigDecimal.valueOf(150), 10, pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}