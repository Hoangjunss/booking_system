package com.geek.booking.dto.request.concern;

import com.geek.booking.dto.request.ticketCategory.TicketCategoryCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConcertCreateRequest {
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String venue;
    @NotNull
    private LocalDateTime eventDate;

    @Valid
    private List<TicketCategoryCreateRequest> ticketCategories;
}