package com.geek.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.booking.dto.response.ConcertResponse;
import com.geek.booking.dto.response.TicketCategoryResponse;
import com.geek.booking.enums.ConcertStatus;
import com.geek.booking.security.JwtTokenProvider;
import com.geek.booking.service.ConcertService;
import com.geek.booking.service.TicketCategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerConcertController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerConcertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ConcertService concertService;

    @MockitoBean
    private TicketCategoryService ticketCategoryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("GET /api/concerts - should return list of published concerts")
    void getPublishedConcerts_ShouldReturnList() throws Exception {
        ConcertResponse concert = ConcertResponse.builder()
                .id(1L)
                .name("Rock Fest")
                .status(ConcertStatus.PUBLISHED)
                .eventDate(LocalDateTime.now().plusDays(10))
                .build();
        when(concertService.getPublishedConcerts()).thenReturn(List.of(concert));

        mockMvc.perform(get("/api/concerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Rock Fest"));
    }

    @Test
    @DisplayName("GET /api/concerts/{id} - should return concert details")
    void getConcertById_ShouldReturnConcert() throws Exception {
        ConcertResponse concert = ConcertResponse.builder().id(1L).name("Rock Fest").build();
        when(concertService.getConcertById(1L)).thenReturn(concert);

        mockMvc.perform(get("/api/concerts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/concerts/{id}/ticket-categories - should return categories")
    void getTicketCategories_ShouldReturnList() throws Exception {
        TicketCategoryResponse category = TicketCategoryResponse.builder().id(1L).name("VIP").build();
        when(ticketCategoryService.getTicketCategoriesByConcert(1L)).thenReturn(List.of(category));

        mockMvc.perform(get("/api/concerts/1/ticket-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("VIP"));
    }
}