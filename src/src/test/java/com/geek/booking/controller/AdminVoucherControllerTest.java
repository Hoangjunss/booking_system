package com.geek.booking.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.booking.dto.request.voucher.VoucherCreateRequest;
import com.geek.booking.dto.response.VoucherResponse;
import com.geek.booking.security.JwtTokenProvider;
import com.geek.booking.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminVoucherController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminVoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private VoucherService voucherService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("POST /api/admin/vouchers - should create voucher")
    void createVoucher_Success() throws Exception {
        VoucherCreateRequest request = new VoucherCreateRequest();
        request.setCode("SUMMER20");
        request.setDiscountType("PERCENT");
        request.setDiscountValue(BigDecimal.valueOf(20));
        request.setValidFrom(LocalDateTime.now());
        request.setValidTo(LocalDateTime.now().plusDays(30));

        VoucherResponse response = VoucherResponse.builder().id(1L).code("SUMMER20").build();
        when(voucherService.createVoucher(any(VoucherCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUMMER20"));
    }

    @Test
    @DisplayName("GET /api/admin/vouchers - should return page of vouchers")
    void getAllVouchers_ShouldReturnPage() throws Exception {
        VoucherResponse response = VoucherResponse.builder().id(1L).code("TEST").build();
        Page<VoucherResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
        when(voucherService.getAllVouchers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/vouchers")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("TEST"));
    }


}