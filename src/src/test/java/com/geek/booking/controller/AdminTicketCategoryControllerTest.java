package com.geek.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.booking.controller.AdminTicketCategoryController;
import com.geek.booking.dto.request.ticketCategory.TicketCategoryCreateRequest;
import com.geek.booking.dto.request.ticketCategory.TicketCategoryUpdateRequest;
import com.geek.booking.dto.response.TicketCategoryResponse;
import com.geek.booking.security.JwtTokenProvider;
import com.geek.booking.service.TicketCategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminTicketCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminTicketCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TicketCategoryService ticketCategoryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("POST /api/admin/ticket-categories - should create category")
    void createTicketCategory_Success() throws Exception {
        TicketCategoryCreateRequest request = new TicketCategoryCreateRequest();
        request.setName("Early Bird");
        request.setPrice(BigDecimal.valueOf(30));
        request.setTotalQuantity(100);

        TicketCategoryResponse response = TicketCategoryResponse.builder().id(1L).name("Early Bird").build();
        when(ticketCategoryService.createTicketCategory(any(TicketCategoryCreateRequest.class), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/admin/ticket-categories")
                        .param("concertId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/admin/ticket-categories/{id} - should update category")
    void updateTicketCategory_Success() throws Exception {
        TicketCategoryUpdateRequest request = new TicketCategoryUpdateRequest();
        request.setPrice(BigDecimal.valueOf(35));

        TicketCategoryResponse response = TicketCategoryResponse.builder().id(1L).price(BigDecimal.valueOf(35)).build();
        when(ticketCategoryService.updateTicketCategory(eq(1L), any(TicketCategoryUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/admin/ticket-categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(35));
    }


}