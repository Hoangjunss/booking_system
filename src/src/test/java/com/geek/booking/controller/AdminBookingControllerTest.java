package com.geek.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.booking.dto.response.BookingResponse;
import com.geek.booking.enums.BookingStatus;
import com.geek.booking.security.JwtTokenProvider;
import com.geek.booking.security.UserPrincipal;
import com.geek.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminBookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;


    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private UserPrincipal mockAdmin;

    @BeforeEach
    void setUp() {
        // Khởi tạo đối tượng admin giả lập
        mockAdmin = UserPrincipal.builder()
                .id(99L)
                .email("admin@geek.com")
                .role("ADMIN")
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/bookings - should return paged bookings")
    void getAllBookings_ShouldReturnPage() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .bookingId(1L)
                .status(BookingStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(100))
                .build();

        Page<BookingResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        when(bookingService.getAllBookings(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/bookings")
                        .with(user(mockAdmin)) // Thêm user để tránh Null ở các API khác nếu cần
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].bookingId").value(1L));
    }

}