package com.geek.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.booking.dto.request.booking.BookingCreateRequest;
import com.geek.booking.dto.response.BookingResponse;
import com.geek.booking.enums.BookingStatus;
import com.geek.booking.exception.InsufficientInventoryException;
import com.geek.booking.security.JwtTokenProvider;
import com.geek.booking.security.UserPrincipal;
import com.geek.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerBookingController.class)
class CustomerBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private UserPrincipal mockUser;

    @BeforeEach
    void setUp() {
        mockUser = UserPrincipal.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .role("CUSTOMER")
                .build();

    }

    @Test
    @DisplayName("POST /api/bookings - should create booking successfully")
    void createBooking_Success() throws Exception {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setCategoryId(1L);
        request.setQuantity(2);
        request.setIdempotencyKey("test-uuid");
        request.setVoucherCode("FLASH10");

        BookingResponse response = BookingResponse.builder()
                .bookingId(1L)
                .status(BookingStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(180))
                .build();

        // Giả lập service trả về response
        when(bookingService.createBooking(any(BookingCreateRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/bookings")
                        .with(user(mockUser)) // Giải quyết lỗi NullPointerException cho currentUser
                        .with(csrf())         // Tránh lỗi 403 khi dùng method POST
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(1L));
    }

    @Test
    @DisplayName("POST /api/bookings - should return 409 when inventory insufficient")
    void createBooking_InsufficientInventory_ShouldReturn409() throws Exception {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setCategoryId(1L);
        request.setQuantity(1000);
        request.setIdempotencyKey("test-uuid");

        when(bookingService.createBooking(any(BookingCreateRequest.class), anyLong()))
                .thenThrow(new InsufficientInventoryException("Not enough tickets"));

        mockMvc.perform(post("/api/bookings")
                        .with(user(mockUser)) // Inject user vào đây
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/bookings/{id}/cancel - should cancel booking")
    void cancelBooking_Success() throws Exception {
        mockMvc.perform(post("/api/bookings/1/cancel")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Changed mind\"}"))
                .andExpect(status().isNoContent());
    }
}