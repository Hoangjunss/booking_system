package com.geek.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.booking.dto.request.concern.ConcertCreateRequest;
import com.geek.booking.dto.request.concern.ConcertUpdateRequest;
import com.geek.booking.dto.response.ConcertResponse;
import com.geek.booking.enums.ConcertStatus;
import com.geek.booking.security.JwtTokenProvider;
import com.geek.booking.service.ConcertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminConcertController.class)
@WithMockUser(roles = "ADMIN")
class AdminConcertControllerTest {

    @Autowired
    private MockMvc mockMvc;


    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private ConcertService concertService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("POST /api/admin/concerts - should create concert")
    void createConcert_Success() throws Exception {
        ConcertCreateRequest request = new ConcertCreateRequest();
        request.setName("Summer Fest");
        request.setVenue("Central Park");
        request.setEventDate(java.time.LocalDateTime.now().plusDays(7));

        ConcertResponse response = ConcertResponse.builder()
                .id(1L)
                .name("Summer Fest")
                .status(ConcertStatus.DRAFT)
                .build();

        when(concertService.createConcert(any(ConcertCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/concerts")
                        .with(csrf()) // Thêm CSRF token để tránh lỗi 403
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/admin/concerts/{id} - should update concert")
    void updateConcert_Success() throws Exception {
        ConcertUpdateRequest request = new ConcertUpdateRequest();
        request.setName("Updated Fest");

        ConcertResponse response = ConcertResponse.builder().id(1L).name("Updated Fest").build();
        when(concertService.updateConcert(eq(1L), any(ConcertUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/admin/concerts/1")
                        .with(csrf()) // Thêm CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Fest"));
    }

    @Test
    @DisplayName("POST /api/admin/concerts/{id}/publish - should publish concert")
    void publishConcert_Success() throws Exception {
        doNothing().when(concertService).publishConcert(1L);

        mockMvc.perform(post("/api/admin/concerts/1/publish")
                        .with(csrf())) // Thêm CSRF token
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/admin/concerts/{id}/unpublish - should unpublish concert")
    void unpublishConcert_Success() throws Exception {
        doNothing().when(concertService).unpublishConcert(1L);

        mockMvc.perform(post("/api/admin/concerts/1/unpublish")
                        .with(csrf())) // Thêm CSRF token
                .andExpect(status().isNoContent());
    }
}